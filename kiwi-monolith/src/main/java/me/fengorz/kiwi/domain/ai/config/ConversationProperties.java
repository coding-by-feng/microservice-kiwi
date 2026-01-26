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
package me.fengorz.kiwi.domain.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Conversation Generation Properties
 *
 * @author codingByFeng
 */
@Data
@Component
@ConfigurationProperties(prefix = "kiwi.ai.conversation")
public class ConversationProperties {

    /**
     * Whether conversation generation is enabled
     */
    private boolean enabled = true;

    /**
     * Maximum number of conversations a user can generate per day
     */
    private int maxDailyGenerations = 10;

    /**
     * FTP storage path for conversation audio files
     */
    private String audioStoragePath = "/conversation-audio";

    /**
     * Delay between TTS API calls in milliseconds (to avoid rate limiting)
     */
    private int ttsDelayMs = 200;

    /**
     * SSE timeout in milliseconds (default: 10 minutes)
     */
    private long sseTimeoutMs = 600000;

    /**
     * Maximum speakers allowed in a conversation
     */
    private int maxSpeakers = 4;

    /**
     * Minimum speakers required in a conversation
     */
    private int minSpeakers = 2;

    /**
     * Cache TTL for conversation data in seconds
     */
    private int cacheTtlSeconds = 3600;

    /**
     * Topic generation prompt template.
     * Placeholders: %s (topic input), %s (difficulty description)
     */
    private String topicGenerationPrompt = """
            Generate a detailed, engaging conversation topic/scenario based on the following:

            %s

            Difficulty level: %s

            Requirements:
            - Create a realistic scenario with context and setting
            - The topic should be suitable for a conversation practice session
            - Include 4 relevant vocabulary keywords for this topic
            - Suggest an appropriate number of speakers (2-4) based on the scenario
            - Suggest an appropriate duration (TWO_MINUTES, FIVE_MINUTES, or TEN_MINUTES)

            IMPORTANT: Output ONLY valid JSON in this exact format, no other text:
            {
              "topic": "A detailed, engaging scenario description in 2-3 sentences",
              "suggestedSpeakerCount": 2,
              "suggestedDuration": "FIVE_MINUTES",
              "keywords": ["keyword1", "keyword2", "keyword3", "keyword4"]
            }
            """;
}
