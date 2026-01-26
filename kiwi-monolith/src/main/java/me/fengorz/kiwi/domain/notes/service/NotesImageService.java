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
import me.fengorz.kiwi.domain.notes.dto.NotesImageRequest;
import me.fengorz.kiwi.domain.notes.entity.MediaStatus;
import me.fengorz.kiwi.domain.notes.entity.NotesItem;
import me.fengorz.kiwi.domain.notes.mapper.NotesItemMapper;
import me.fengorz.kiwi.domain.notes.vo.NotesItemVO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;

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

    @Autowired(required = false)
    private GeminiImageClient geminiImageClient;

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
     * Build image prompt from note content, custom additions, and style
     */
    private String buildImagePrompt(String noteContent, String customPrompt, String style) {
        StringBuilder prompt = new StringBuilder();

        // Extract key concepts from note content (first 200 chars)
        String contentSummary = noteContent.length() > 200
                ? noteContent.substring(0, 200) + "..."
                : noteContent;
        prompt.append("Image description: ").append(contentSummary);

        if (StringUtils.isNotBlank(customPrompt)) {
            prompt.append(". Additional context: ").append(customPrompt);
        }

        if (StringUtils.isNotBlank(style)) {
            prompt.append(". Image style: ").append(style);
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
