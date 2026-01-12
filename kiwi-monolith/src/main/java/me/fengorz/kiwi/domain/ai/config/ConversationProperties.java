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
     * Maximum concurrent TTS API calls (rate limit protection)
     */
    private int ttsMaxConcurrency = 3;
}
