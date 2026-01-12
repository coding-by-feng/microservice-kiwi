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
package me.fengorz.kiwi.domain.word.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Review Audio Configuration Properties
 *
 * @author codingByFeng
 */
@Data
@Component
@ConfigurationProperties(prefix = "kiwi.review.audio")
public class ReviewAudioProperties {

    /**
     * Whether review audio generation is enabled
     */
    private boolean enabled = true;

    /**
     * Maximum number of audio files to generate per batch/request
     * This limits API costs and prevents abuse
     */
    private int maxGenerationCount = 100;

    /**
     * Path to store generated audio files
     */
    private String storagePath = "/wordTmp/review-audio";

    /**
     * Whether to generate audio asynchronously
     */
    private boolean asyncGeneration = true;

    /**
     * Content mode for audio generation:
     * - english: Only English definition/paraphrase
     * - chinese: Only Chinese meaning
     * - both: English followed by Chinese
     */
    private String contentMode = "both";

    /**
     * Delay between API calls in milliseconds to avoid rate limiting
     */
    private int apiCallDelayMs = 200;

    /**
     * Scheduled job configuration
     */
    private Scheduler scheduler = new Scheduler();

    public boolean isEnglishOnly() {
        return "english".equalsIgnoreCase(contentMode);
    }

    public boolean isChineseOnly() {
        return "chinese".equalsIgnoreCase(contentMode);
    }

    public boolean isBothLanguages() {
        return "both".equalsIgnoreCase(contentMode);
    }

    @Data
    public static class Scheduler {
        /**
         * Whether the scheduled job is enabled
         */
        private boolean enabled = false;

        /**
         * Cron expression for the scheduled job
         * Default: Run at 3:00 AM daily
         */
        private String cron = "0 0 3 * * *";

        /**
         * Maximum paraphrases to process per scheduled run
         */
        private int maxPerRun = 500;

        /**
         * Only generate for paraphrases created in the last N days
         */
        private int lookbackDays = 30;
    }
}
