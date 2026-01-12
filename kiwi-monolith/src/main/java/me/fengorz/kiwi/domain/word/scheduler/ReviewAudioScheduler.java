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
package me.fengorz.kiwi.domain.word.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.domain.word.config.ReviewAudioProperties;
import me.fengorz.kiwi.domain.word.entity.ParaphraseStarRel;
import me.fengorz.kiwi.domain.word.service.ParaphraseStarRelService;
import me.fengorz.kiwi.domain.word.service.ReviewAudioService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Review Audio Scheduler
 * Automatically generates audio for recently collected paraphrases
 *
 * @author codingByFeng
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "kiwi.review.audio.scheduler",
        name = "enabled",
        havingValue = "true"
)
public class ReviewAudioScheduler {

    private final ReviewAudioProperties properties;
    private final ReviewAudioService reviewAudioService;
    private final ParaphraseStarRelService starRelService;

    /**
     * Scheduled job to generate audio for recently collected paraphrases
     * Runs based on cron expression in configuration
     */
    @Scheduled(cron = "${kiwi.review.audio.scheduler.cron:0 0 3 * * *}")
    public void generateAudioForRecentParaphrases() {
        if (!properties.isEnabled()) {
            log.info("Review audio generation is disabled, skipping scheduled job");
            return;
        }

        if (!properties.getScheduler().isEnabled()) {
            log.debug("Review audio scheduler is disabled");
            return;
        }

        log.info("Starting scheduled review audio generation job");

        try {
            int maxPerRun = properties.getScheduler().getMaxPerRun();
            int lookbackDays = properties.getScheduler().getLookbackDays();
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(lookbackDays);

            // Find recently added paraphrases to star lists
            List<ParaphraseStarRel> recentItems = findRecentStarRelItems(cutoffDate, maxPerRun);

            if (recentItems.isEmpty()) {
                log.info("No recent paraphrases found for audio generation");
                return;
            }

            log.info("Found {} recent paraphrases for audio generation (lookback: {} days)",
                    recentItems.size(), lookbackDays);

            int successCount = 0;
            int failedCount = 0;
            int skippedCount = 0;

            for (ParaphraseStarRel rel : recentItems) {
                Integer paraphraseId = rel.getParaphraseId();

                // Skip if audio already exists
                if (reviewAudioService.hasAudio(paraphraseId)) {
                    skippedCount++;
                    continue;
                }

                boolean success = reviewAudioService.generateAudioForParaphrase(paraphraseId);
                if (success) {
                    successCount++;
                } else {
                    failedCount++;
                }

                // Rate limiting delay
                if (properties.getApiCallDelayMs() > 0) {
                    try {
                        Thread.sleep(properties.getApiCallDelayMs());
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.warn("Scheduled job interrupted");
                        break;
                    }
                }
            }

            log.info("Scheduled review audio generation completed. Success: {}, Failed: {}, Skipped: {}",
                    successCount, failedCount, skippedCount);

        } catch (Exception e) {
            log.error("Error during scheduled review audio generation", e);
        }
    }

    /**
     * Find recent paraphrase star relations for audio generation
     */
    private List<ParaphraseStarRel> findRecentStarRelItems(LocalDateTime cutoffDate, int limit) {
        return starRelService.list(new LambdaQueryWrapper<ParaphraseStarRel>()
                .ge(ParaphraseStarRel::getCreateTime, cutoffDate)
                .orderByDesc(ParaphraseStarRel::getCreateTime)
                .last("LIMIT " + limit));
    }
}
