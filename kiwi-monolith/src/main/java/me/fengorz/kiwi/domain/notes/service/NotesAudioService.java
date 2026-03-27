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
import me.fengorz.kiwi.common.tts.TtsService;
import me.fengorz.kiwi.domain.notes.dto.NotesAudioRequest;
import me.fengorz.kiwi.domain.notes.entity.MediaStatus;
import me.fengorz.kiwi.domain.notes.entity.NotesItem;
import me.fengorz.kiwi.domain.notes.mapper.NotesItemMapper;
import me.fengorz.kiwi.domain.notes.vo.NotesItemVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;

/**
 * Notes Audio Service - Integrates with OpenAI TTS for audio generation
 *
 * @author codingByFeng
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotesAudioService {

    private final NotesItemMapper notesItemMapper;
    private final TtsService ttsService;
    private final DfsService dfsService;

    /**
     * Generate audio for a note item using OpenAI TTS
     */
    @Transactional
    public NotesItemVO generateAudio(NotesAudioRequest request, Integer userId) {
        NotesItem item = notesItemMapper.selectById(request.getNoteItemId());

        if (item == null || GlobalConstants.FLAG_Y.equals(item.getIsDel())) {
            throw new ServiceException("Note item not found");
        }
        if (!item.getUserId().equals(userId)) {
            throw new ServiceException("Access denied");
        }

        if (MediaStatus.GENERATING.getCode().equals(item.getAudioStatus())) {
            throw new ServiceException("Audio is already being generated");
        }

        // Mark as generating
        item.setAudioStatusEnum(MediaStatus.GENERATING);
        item.setAudioAccent(request.getAccent().name());
        item.setAudioVoice(request.getVoice());
        notesItemMapper.updateById(item);

        try {
            // Generate audio via OpenAI TTS
            byte[] audioBytes = ttsService.speechWithAccent(
                    item.getContent(),
                    request.getVoice(),
                    request.getAccent()
            );

            if (audioBytes == null || audioBytes.length == 0) {
                throw new ServiceException("Failed to generate audio");
            }

            // Upload to DFS/MinIO
            ByteArrayInputStream inputStream = new ByteArrayInputStream(audioBytes);
            String audioUrl = dfsService.uploadFile(inputStream, audioBytes.length, ttsService.getAudioFormat());

            // Estimate duration (~12.5 characters per second for English)
            int durationMs = (int) (item.getContent().length() / 12.5 * 1000);

            // Update item
            item.setAudioUrl(audioUrl);
            item.setAudioDurationMs(durationMs);
            item.setAudioStatusEnum(MediaStatus.READY);
            item.setUpdateTime(LocalDateTime.now());
            notesItemMapper.updateById(item);

            log.info("Generated audio for note item {} ({} bytes, {}ms)",
                    item.getId(), audioBytes.length, durationMs);

            return NotesItemVO.fromEntity(item);

        } catch (Exception e) {
            log.error("Failed to generate audio for note item {}", item.getId(), e);
            item.setAudioStatusEnum(MediaStatus.FAILED);
            notesItemMapper.updateById(item);
            throw new ServiceException("Failed to generate audio: " + e.getMessage());
        }
    }

    /**
     * Delete audio for a note item
     */
    @Transactional
    public void deleteAudio(Long noteItemId, Integer userId) {
        NotesItem item = notesItemMapper.selectById(noteItemId);

        if (item == null || !item.getUserId().equals(userId)) {
            throw new ServiceException("Access denied");
        }

        if (item.getAudioUrl() != null) {
            try {
                dfsService.deleteFile("", item.getAudioUrl());
            } catch (Exception e) {
                log.warn("Failed to delete audio file from DFS: {}", e.getMessage());
            }
        }

        item.setAudioUrl(null);
        item.setAudioDurationMs(0);
        item.setAudioStatusEnum(MediaStatus.NONE);
        item.setUpdateTime(LocalDateTime.now());
        notesItemMapper.updateById(item);

        log.info("Deleted audio for note item {}", noteItemId);
    }

    /**
     * Get audio bytes for streaming
     */
    public byte[] getAudioBytes(Long noteItemId, Integer userId) {
        NotesItem item = notesItemMapper.selectById(noteItemId);

        if (item == null || !item.getUserId().equals(userId)) {
            throw new ServiceException("Access denied");
        }

        if (item.getAudioUrl() == null) {
            throw new ServiceException("Audio not available");
        }

        return dfsService.downloadFile("", item.getAudioUrl());
    }
}
