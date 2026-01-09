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
package me.fengorz.kiwi.messaging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Message for AI task operations
 *
 * @author codingByFeng
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiTaskMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Task ID for tracking
     */
    private String taskId;

    /**
     * Task type (translation, tts, subtitle, etc.)
     */
    private String taskType;

    /**
     * User ID who requested the task
     */
    private Integer userId;

    /**
     * Input data for the task
     */
    private String inputData;

    /**
     * Additional parameters
     */
    private Map<String, Object> parameters;

    /**
     * Priority level
     */
    private Integer priority;

    /**
     * Callback URL for result notification
     */
    private String callbackUrl;

    /**
     * Timestamp when message was created
     */
    private LocalDateTime createdAt;

    /**
     * Number of retry attempts
     */
    private Integer retryCount;

    /**
     * AI task types
     */
    public static final String TYPE_TRANSLATION = "translation";
    public static final String TYPE_TTS = "tts";
    public static final String TYPE_SUBTITLE = "subtitle";
    public static final String TYPE_SUMMARY = "summary";
    public static final String TYPE_VOCABULARY_EXPAND = "vocabulary_expand";
}
