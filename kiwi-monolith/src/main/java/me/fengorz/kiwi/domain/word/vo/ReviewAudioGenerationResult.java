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
package me.fengorz.kiwi.domain.word.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * Review Audio Generation Result VO
 *
 * @author codingByFeng
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewAudioGenerationResult {

    /**
     * Whether audio generation is enabled
     */
    private boolean enabled;

    /**
     * Total number of paraphrases requested
     */
    private int totalRequested;

    /**
     * Number of successfully generated audio files
     */
    private int successCount;

    /**
     * Number of failed generations
     */
    private int failedCount;

    /**
     * Number of skipped (already exists)
     */
    private int skippedCount;

    /**
     * Number remaining (exceeds max limit)
     */
    private int remainingCount;

    /**
     * Maximum generation count per request
     */
    private int maxGenerationCount;

    /**
     * List of successfully generated paraphrase IDs
     */
    private List<Integer> successIds;

    /**
     * List of failed paraphrase IDs
     */
    private List<Integer> failedIds;

    /**
     * List of skipped paraphrase IDs
     */
    private List<Integer> skippedIds;

    /**
     * Create a disabled result
     */
    public static ReviewAudioGenerationResult disabled() {
        return ReviewAudioGenerationResult.builder()
                .enabled(false)
                .totalRequested(0)
                .successCount(0)
                .failedCount(0)
                .skippedCount(0)
                .remainingCount(0)
                .maxGenerationCount(0)
                .successIds(Collections.emptyList())
                .failedIds(Collections.emptyList())
                .skippedIds(Collections.emptyList())
                .build();
    }

    /**
     * Check if all requested items were processed
     */
    public boolean isComplete() {
        return remainingCount == 0;
    }

    /**
     * Check if there were any failures
     */
    public boolean hasFailures() {
        return failedCount > 0;
    }

    /**
     * Get processing percentage
     */
    public int getProcessedPercentage() {
        if (totalRequested == 0) return 100;
        int processed = successCount + failedCount + skippedCount;
        return (int) ((processed * 100.0) / totalRequested);
    }
}
