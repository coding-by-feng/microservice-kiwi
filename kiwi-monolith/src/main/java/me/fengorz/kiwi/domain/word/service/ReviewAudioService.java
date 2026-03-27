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
package me.fengorz.kiwi.domain.word.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.tts.TtsService;
import me.fengorz.kiwi.domain.word.config.ReviewAudioProperties;
import me.fengorz.kiwi.domain.word.entity.Paraphrase;
import me.fengorz.kiwi.domain.word.entity.ParaphraseStarRel;
import me.fengorz.kiwi.domain.word.vo.ReviewAudioGenerationResult;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Review Audio Service - generates TTS audio for paraphrase review
 *
 * @author codingByFeng
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewAudioService {

    private final ReviewAudioProperties properties;
    private final ParaphraseService paraphraseService;
    private final ParaphraseStarRelService starRelService;

    private final TtsService ttsService;

    /**
     * Generate audio for all paraphrases in a star list
     *
     * @param listId the star list ID
     * @return generation result with success/failure counts
     */
    public ReviewAudioGenerationResult generateAudioForStarList(Integer listId) {
        if (!properties.isEnabled()) {
            log.warn("Review audio generation is disabled");
            return ReviewAudioGenerationResult.disabled();
        }

        List<ParaphraseStarRel> starRels = starRelService.findByListId(listId);
        return generateAudioForParaphrases(starRels);
    }

    /**
     * Generate audio for review items only (not remembered yet)
     *
     * @param listId the star list ID
     * @return generation result
     */
    public ReviewAudioGenerationResult generateAudioForReviewItems(Integer listId) {
        if (!properties.isEnabled()) {
            log.warn("Review audio generation is disabled");
            return ReviewAudioGenerationResult.disabled();
        }

        List<ParaphraseStarRel> reviewItems = starRelService.findReviewItems(listId);
        return generateAudioForParaphrases(reviewItems);
    }

    /**
     * Generate audio for a single paraphrase
     *
     * @param paraphraseId the paraphrase ID
     * @return true if generation succeeded
     */
    public boolean generateAudioForParaphrase(Integer paraphraseId) {
        if (!properties.isEnabled()) {
            log.warn("Review audio generation is disabled");
            return false;
        }

        Paraphrase paraphrase = paraphraseService.getById(paraphraseId);
        if (paraphrase == null) {
            log.warn("Paraphrase not found: {}", paraphraseId);
            return false;
        }

        return generateAndSaveAudio(paraphrase);
    }

    /**
     * Generate audio asynchronously for a star list
     *
     * @param listId the star list ID
     * @return future with generation result
     */
    @Async
    public CompletableFuture<ReviewAudioGenerationResult> generateAudioForStarListAsync(Integer listId) {
        return CompletableFuture.completedFuture(generateAudioForStarList(listId));
    }

    /**
     * Get the audio file path for a paraphrase
     *
     * @param paraphraseId the paraphrase ID
     * @return the file path or null if not exists
     */
    public String getAudioFilePath(Integer paraphraseId) {
        String filePath = buildAudioFilePath(paraphraseId);
        File file = new File(filePath);
        return file.exists() ? filePath : null;
    }

    /**
     * Get audio bytes for a paraphrase
     *
     * @param paraphraseId the paraphrase ID
     * @return audio bytes or null if not exists
     */
    public byte[] getAudioBytes(Integer paraphraseId) {
        String filePath = getAudioFilePath(paraphraseId);
        if (filePath == null) {
            return null;
        }

        try {
            return Files.readAllBytes(Paths.get(filePath));
        } catch (IOException e) {
            log.error("Failed to read audio file for paraphrase {}", paraphraseId, e);
            return null;
        }
    }

    /**
     * Check if audio exists for a paraphrase
     *
     * @param paraphraseId the paraphrase ID
     * @return true if audio file exists
     */
    public boolean hasAudio(Integer paraphraseId) {
        return getAudioFilePath(paraphraseId) != null;
    }

    /**
     * Delete audio file for a paraphrase
     *
     * @param paraphraseId the paraphrase ID
     * @return true if deleted successfully
     */
    public boolean deleteAudio(Integer paraphraseId) {
        String filePath = getAudioFilePath(paraphraseId);
        if (filePath == null) {
            return true;  // Already doesn't exist
        }

        try {
            Files.deleteIfExists(Paths.get(filePath));
            log.info("Deleted audio file for paraphrase {}", paraphraseId);
            return true;
        } catch (IOException e) {
            log.error("Failed to delete audio file for paraphrase {}", paraphraseId, e);
            return false;
        }
    }

    /**
     * Regenerate audio for a paraphrase (delete and recreate)
     *
     * @param paraphraseId the paraphrase ID
     * @return true if regenerated successfully
     */
    public boolean regenerateAudio(Integer paraphraseId) {
        deleteAudio(paraphraseId);
        return generateAudioForParaphrase(paraphraseId);
    }

    private ReviewAudioGenerationResult generateAudioForParaphrases(List<ParaphraseStarRel> starRels) {
        int maxCount = properties.getMaxGenerationCount();
        int toProcess = Math.min(starRels.size(), maxCount);

        List<Integer> successIds = new ArrayList<>();
        List<Integer> failedIds = new ArrayList<>();
        List<Integer> skippedIds = new ArrayList<>();

        log.info("Starting audio generation for {} paraphrases (max: {})", toProcess, maxCount);

        for (int i = 0; i < toProcess; i++) {
            ParaphraseStarRel rel = starRels.get(i);
            Integer paraphraseId = rel.getParaphraseId();

            // Skip if audio already exists
            if (hasAudio(paraphraseId)) {
                skippedIds.add(paraphraseId);
                continue;
            }

            Paraphrase paraphrase = paraphraseService.getById(paraphraseId);
            if (paraphrase == null) {
                failedIds.add(paraphraseId);
                continue;
            }

            boolean success = generateAndSaveAudio(paraphrase);
            if (success) {
                successIds.add(paraphraseId);
            } else {
                failedIds.add(paraphraseId);
            }

            // Rate limiting delay
            if (properties.getApiCallDelayMs() > 0 && i < toProcess - 1) {
                try {
                    Thread.sleep(properties.getApiCallDelayMs());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        int remaining = starRels.size() - toProcess;
        log.info("Audio generation complete. Success: {}, Failed: {}, Skipped: {}, Remaining: {}",
                successIds.size(), failedIds.size(), skippedIds.size(), remaining);

        return ReviewAudioGenerationResult.builder()
                .enabled(true)
                .totalRequested(starRels.size())
                .successCount(successIds.size())
                .failedCount(failedIds.size())
                .skippedCount(skippedIds.size())
                .remainingCount(remaining)
                .successIds(successIds)
                .failedIds(failedIds)
                .skippedIds(skippedIds)
                .maxGenerationCount(maxCount)
                .build();
    }

    private boolean generateAndSaveAudio(Paraphrase paraphrase) {
        try {
            String text = buildAudioText(paraphrase);
            if (StringUtils.isBlank(text)) {
                log.warn("No text content for paraphrase {}", paraphrase.getParaphraseId());
                return false;
            }

            byte[] audioBytes = generateAudioBytes(text, paraphrase);
            if (audioBytes == null || audioBytes.length == 0) {
                log.error("Failed to generate audio for paraphrase {}", paraphrase.getParaphraseId());
                return false;
            }

            return saveAudioFile(paraphrase.getParaphraseId(), audioBytes);
        } catch (Exception e) {
            log.error("Error generating audio for paraphrase {}", paraphrase.getParaphraseId(), e);
            return false;
        }
    }

    private String buildAudioText(Paraphrase paraphrase) {
        StringBuilder sb = new StringBuilder();

        if (properties.isEnglishOnly() || properties.isBothLanguages()) {
            String english = paraphrase.getParaphraseEnglish();
            if (StringUtils.isNotBlank(english)) {
                sb.append(english);
            }
        }

        if (properties.isChineseOnly() || properties.isBothLanguages()) {
            String chinese = paraphrase.getMeaningChinese();
            if (StringUtils.isNotBlank(chinese)) {
                if (sb.length() > 0) {
                    sb.append(". ");  // Separator between languages
                }
                sb.append(chinese);
            }
        }

        return sb.toString().trim();
    }

    private byte[] generateAudioBytes(String text, Paraphrase paraphrase) {
        try {
            // Determine language based on content
            if (properties.isChineseOnly()) {
                return ttsService.speechChinese(text);
            } else if (properties.isEnglishOnly()) {
                return ttsService.speechEnglish(text);
            } else {
                // For both languages, use English voice (will handle Chinese reasonably well)
                return ttsService.speechEnglish(text);
            }
        } catch (Exception e) {
            log.error("TTS API call failed for paraphrase {}", paraphrase.getParaphraseId(), e);
            return null;
        }
    }

    private boolean saveAudioFile(Integer paraphraseId, byte[] audioBytes) {
        String filePath = buildAudioFilePath(paraphraseId);

        try {
            // Ensure directory exists
            Path parentDir = Paths.get(filePath).getParent();
            if (parentDir != null) {
                Files.createDirectories(parentDir);
            }

            // Write audio file
            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                fos.write(audioBytes);
            }

            log.debug("Saved audio file: {} ({} bytes)", filePath, audioBytes.length);
            return true;
        } catch (IOException e) {
            log.error("Failed to save audio file for paraphrase {}", paraphraseId, e);
            return false;
        }
    }

    private String buildAudioFilePath(Integer paraphraseId) {
        return properties.getStoragePath() + File.separator + "paraphrase_" + paraphraseId + "." + ttsService.getAudioFormat();
    }
}
