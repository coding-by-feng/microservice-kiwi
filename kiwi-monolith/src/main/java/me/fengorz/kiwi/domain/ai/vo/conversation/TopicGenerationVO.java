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
package me.fengorz.kiwi.domain.ai.vo.conversation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import me.fengorz.kiwi.domain.ai.dto.conversation.DurationOption;
import me.fengorz.kiwi.domain.ai.dto.conversation.TopicGenerationRequest;

import java.util.List;

/**
 * Topic generation response VO
 *
 * @author codingByFeng
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopicGenerationVO {

    /**
     * The refined conversation topic/scenario
     */
    private String topic;

    /**
     * Topic category
     */
    private String category;

    /**
     * Difficulty level
     */
    private String difficulty;

    /**
     * Recommended number of speakers (2-4)
     */
    private Integer suggestedSpeakerCount;

    /**
     * Suggested conversation length
     */
    private String suggestedDuration;

    /**
     * Related vocabulary keywords for the topic
     */
    private List<String> keywords;
}
