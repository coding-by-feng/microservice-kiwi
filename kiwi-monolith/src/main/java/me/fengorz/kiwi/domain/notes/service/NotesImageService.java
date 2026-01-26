/*
 * Copyright [2019~2025] [codingByFeng]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.fengorz.kiwi.domain.notes.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.constant.GlobalConstants;
import me.fengorz.kiwi.common.dfs.DfsService;
import me.fengorz.kiwi.common.exception.ServiceException;
import me.fengorz.kiwi.common.gemini.GeminiImageClient;
import me.fengorz.kiwi.domain.notes.config.NotesImageStyleProperties;
import me.fengorz.kiwi.domain.notes.dto.NotesImageRequest;
import me.fengorz.kiwi.domain.notes.entity.MediaStatus;
import me.fengorz.kiwi.domain.notes.entity.NotesItem;
import me.fengorz.kiwi.domain.notes.mapper.NotesItemMapper;
import me.fengorz.kiwi.domain.notes.vo.ImageStyleVO;
import me.fengorz.kiwi.domain.notes.vo.NotesItemVO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Notes Image Service - Integrates with Gemini Imagen API for image generation
 *
 * @author codingByFeng
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotesImageService {

    private final NotesItemMapper notesItemMapper;
    private final DfsService dfsService;
    private final NotesImageStyleProperties styleProperties;

    @Autowired(required = false)
    private GeminiImageClient geminiImageClient;

    /**
     * List all available image generation styles
     */
    public List<ImageStyleVO> listAvailableStyles() {
        return styleProperties.getImageStyles().stream()
                .filter(NotesImageStyleProperties.ImageStyle::isEnabled)
                .sorted(Comparator.comparingInt(NotesImageStyleProperties.ImageStyle::getSortOrder))
                .map(style -> ImageStyleVO.builder()
                        .id(style.getId())
                        .name(style.getName())
                        .description(style.getDescription())
                        .previewUrl(style.getPreviewUrl())
                        .sortOrder(style.getSortOrder())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Get style prompt by style ID
     */
    private String getStylePromptById(String styleId) {
        if (styleId == null) {
            return null;
        }
        return styleProperties.getImageStyles().stream()
                .filter(s -> s.isEnabled() && styleId.equals(s.getId()))
                .findFirst()
                .map(NotesImageStyleProperties.ImageStyle::getPrompt)
                .orElse(styleId); // Fall back to raw style string if not found
    }

    /**
     * Generate image for a note item using Gemini Imagen API
     */
    @Transactional
    public NotesItemVO generateImage(NotesImageRequest request, Integer userId) {
        NotesItem item = notesItemMapper.selectById(request.getNoteItemId());

        if (item == null || GlobalConstants.FLAG_Y.equals(item.getIsDel())) {
            throw new ServiceException("Note item not found");
        }
        if (!item.getUserId().equals(userId)) {
            throw new ServiceException("Access denied");
        }

        if (MediaStatus.GENERATING.getCode().equals(item.getImageStatus())) {
            throw new ServiceException("Image is already being generated");
        }

        // Build image prompt
        String finalPrompt = buildImagePrompt(item.getContent(), request.getCustomPrompt(), request.getStyle());

        // Store the prompt
        item.setImagePrompt(finalPrompt);
        item.setImageStyle(request.getStyle());
        item.setImageStatusEnum(MediaStatus.GENERATING);
        item.setUpdateTime(LocalDateTime.now());
        notesItemMapper.updateById(item);

        // Check if Gemini client is available
        if (geminiImageClient == null) {
            item.setImageStatusEnum(MediaStatus.PENDING);
            notesItemMapper.updateById(item);
            log.warn("Gemini Image Client not configured. Image prompt stored for future generation.");
            throw new ServiceException("Gemini image generation not configured. Please set GEMINI_API_KEY and enable kiwi.gemini.enabled=true");
        }

        try {
            // Generate image via Gemini Imagen API
            byte[] imageBytes = geminiImageClient.generateImage(finalPrompt);

            if (imageBytes == null || imageBytes.length == 0) {
                throw new ServiceException("Failed to generate image");
            }

            // Upload to DFS/MinIO
            ByteArrayInputStream inputStream = new ByteArrayInputStream(imageBytes);
            String imageUrl = dfsService.uploadFile(inputStream, imageBytes.length, "png");

            // Update item
            item.setImageUrl(imageUrl);
            item.setImageStatusEnum(MediaStatus.READY);
            item.setUpdateTime(LocalDateTime.now());
            notesItemMapper.updateById(item);

            log.info("Generated image for note item {} ({} bytes)", item.getId(), imageBytes.length);

            return NotesItemVO.fromEntity(item);

        } catch (Exception e) {
            log.error("Failed to generate image for note item {}", item.getId(), e);
            item.setImageStatusEnum(MediaStatus.FAILED);
            notesItemMapper.updateById(item);
            throw new ServiceException("Failed to generate image: " + e.getMessage());
        }
    }

    /**
     * Build image prompt from note content and style.
     * Uses a structured format that prioritizes the content while applying the artistic style.
     *
     * @param noteContent the note content to describe
     * @param customPrompt optional additional context (currently unused, reserved for future)
     * @param styleId the style ID to look up, or raw style string as fallback
     */
    private String buildImagePrompt(String noteContent, String customPrompt, String styleId) {
        StringBuilder prompt = new StringBuilder();

        // Extract key concepts from note content (first 300 chars for better context)
        String contentSummary = noteContent.length() > 300
                ? noteContent.substring(0, 300) + "..."
                : noteContent;

        // Structured prompt format that prioritizes content
        prompt.append("Create an image that visually represents and captures the essence of this concept:\n\n");
        prompt.append("\"").append(contentSummary).append("\"\n\n");
        prompt.append("The image should clearly convey the meaning, emotion, and message of this text through visual storytelling.");

        // Get the style prompt (resolve ID to full prompt, or use raw string)
        String stylePrompt = getStylePromptById(styleId);
        if (StringUtils.isNotBlank(stylePrompt)) {
            prompt.append("\n\nArtistic style to apply: ").append(stylePrompt);
            prompt.append("\n\nImportant: The subject matter and core message must be the primary focus. ");
            prompt.append("Use the artistic style to enhance the visual presentation without overshadowing the content's meaning.");
        }

        return prompt.toString();
    }

    /**
     * Delete image for a note item
     */
    @Transactional
    public void deleteImage(Long noteItemId, Integer userId) {
        NotesItem item = notesItemMapper.selectById(noteItemId);

        if (item == null || !item.getUserId().equals(userId)) {
            throw new ServiceException("Access denied");
        }

        if (item.getImageUrl() != null) {
            try {
                dfsService.deleteFile("", item.getImageUrl());
            } catch (Exception e) {
                log.warn("Failed to delete image file from DFS: {}", e.getMessage());
            }
        }

        item.setImageUrl(null);
        item.setImagePrompt(null);
        item.setImageStyle(null);
        item.setImageStatusEnum(MediaStatus.NONE);
        item.setUpdateTime(LocalDateTime.now());
        notesItemMapper.updateById(item);

        log.info("Deleted image for note item {}", noteItemId);
    }

    /**
     * Get image bytes for streaming
     */
    public byte[] getImageBytes(Long noteItemId, Integer userId) {
        NotesItem item = notesItemMapper.selectById(noteItemId);

        if (item == null || !item.getUserId().equals(userId)) {
            throw new ServiceException("Access denied");
        }

        if (item.getImageUrl() == null) {
            throw new ServiceException("Image not available");
        }

        return dfsService.downloadFile("", item.getImageUrl());
    }
}
