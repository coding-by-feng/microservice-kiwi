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
 * Request DTO for generating random conversation topics
 *
 * @author codingByFeng
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationTopicRequest {

    /**
     * Topic category. Default: "lifestyle"
     * Options: "lifestyle", "travel", "food", "work", "hobbies", "health"
     */
    @Builder.Default
    private TopicCategory category = TopicCategory.LIFESTYLE;

    /**
     * Difficulty level. Default: "intermediate"
     * Options: "beginner", "intermediate", "advanced"
     */
    @Builder.Default
    private TopicDifficulty difficulty = TopicDifficulty.INTERMEDIATE;

    /**
     * Target language code. Default: "en"
     */
    @Builder.Default
    private String language = "en";
}
