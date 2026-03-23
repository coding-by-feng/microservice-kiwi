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
package me.fengorz.kiwi.domain.ai.service.conversation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.dfs.DfsService;
import me.fengorz.kiwi.common.tts.AccentType;
import me.fengorz.kiwi.common.tts.TtsService;
import me.fengorz.kiwi.domain.ai.config.ConversationProperties;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Service for generating TTS audio for conversation messages
 *
 * @author codingByFeng
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationTtsService {

    private final TtsService ttsService;
    private final DfsService dfsService;
    private final ConversationProperties properties;

    /**
     * Voice pool for different speakers
     * These voices have distinct characteristics suitable for conversations
     */
    private static final List<String> VOICE_POOL = Arrays.asList(
            "alloy",    // Neutral, balanced
            "echo",     // Deeper, authoritative
            "fable",    // Warm, storytelling
            "onyx",     // Deep, mature
            "nova",     // Friendly, conversational
            "shimmer",  // Soft, gentle
            "coral",    // Clear, articulate
            "sage"      // Calm, measured
    );

    /**
     * Assign distinct voices to speakers
     *
     * @param speakerCount number of speakers
     * @return list of voice names
     */
    public List<String> assignVoices(int speakerCount) {
        List<String> assignedVoices = new ArrayList<>();
        for (int i = 0; i < speakerCount && i < VOICE_POOL.size(); i++) {
            assignedVoices.add(VOICE_POOL.get(i));
        }
        return assignedVoices;
    }

    /**
     * Generate audio for a message with specific voice and accent
     *
     * @param text   the text to synthesize
     * @param voice  the voice to use
     * @param accent the accent type
     * @return audio bytes
     */
    public byte[] generateAudio(String text, String voice, AccentType accent) {
        log.debug("Generating audio for text length {} with voice {} and accent {}",
                text.length(), voice, accent);

        return ttsService.speechWithAccent(text, voice, accent);
    }

    /**
     * Generate audio and upload to FTP
     *
     * @param text           the text to synthesize
     * @param voice          the voice to use
     * @param accent         the accent type
     * @param conversationId the conversation ID
     * @param messageId      the message ID
     * @return FTP file path
     */
    public String generateAndUpload(String text, String voice, AccentType accent,
                                    Long conversationId, Long messageId) {
        byte[] audioBytes = generateAudio(text, voice, accent);

        if (audioBytes == null || audioBytes.length == 0) {
            log.warn("Generated empty audio for message {}", messageId);
            return null;
        }

        // Build FTP path: /conversation-audio/{conversationId}/message_{messageId}.mp3
        String fileName = String.format("%s/%d/message_%d.mp3",
                properties.getAudioStoragePath(),
                conversationId,
                messageId);

        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(audioBytes);
            String ftpPath = dfsService.uploadFile(inputStream, audioBytes.length, "mp3");
            log.info("Uploaded audio to FTP: {} ({} bytes)", ftpPath, audioBytes.length);
            return ftpPath;
        } catch (Exception e) {
            log.error("Failed to upload audio to FTP for message {}", messageId, e);
            throw e;
        }
    }

    /**
     * Estimate audio duration based on text length
     * Rough estimate: ~150 words per minute, ~5 characters per word
     *
     * @param text the text
     * @return estimated duration in milliseconds
     */
    public int estimateAudioDuration(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        // ~750 characters per minute = 12.5 characters per second
        int charCount = text.length();
        return (int) (charCount / 12.5 * 1000);
    }
}
