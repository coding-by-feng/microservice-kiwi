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
package me.fengorz.kiwi.domain.ai.dto.conversation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Topic generation request DTO
 *
 * @author codingByFeng
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopicGenerationRequest {

    /**
     * Custom topic idea to refine. If provided, generates topic based on this input.
     */
    private String prompt;

    /**
     * Topic category (ignored when prompt is provided)
     */
    @Builder.Default
    private TopicCategory category = TopicCategory.LIFESTYLE;

    /**
     * Difficulty level for the conversation
     */
    @Builder.Default
    private TopicDifficulty difficulty = TopicDifficulty.INTERMEDIATE;

    /**
     * Target language code
     */
    @Builder.Default
    private String language = "en";

    /**
     * Topic category enum
     */
    public enum TopicCategory {
        LIFESTYLE("Daily life topics"),
        TRAVEL("Travel and tourism"),
        FOOD("Food and dining"),
        WORK("Professional and business"),
        HOBBIES("Hobbies and interests"),
        HEALTH("Health and wellness");

        private final String description;

        TopicCategory(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Topic difficulty enum
     */
    public enum TopicDifficulty {
        BEGINNER("Simple vocabulary and short sentences"),
        INTERMEDIATE("Moderate complexity"),
        ADVANCED("Complex topics and vocabulary");

        private final String description;

        TopicDifficulty(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}
