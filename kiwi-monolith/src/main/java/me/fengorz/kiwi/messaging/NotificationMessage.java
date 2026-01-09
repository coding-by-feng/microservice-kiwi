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
 * Message for notification operations
 *
 * @author codingByFeng
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Notification ID for tracking
     */
    private String notificationId;

    /**
     * Target user ID
     */
    private Integer userId;

    /**
     * Notification type
     */
    private String type;

    /**
     * Notification title
     */
    private String title;

    /**
     * Notification content
     */
    private String content;

    /**
     * Additional data
     */
    private Map<String, Object> data;

    /**
     * Notification channel (push, email, sms, websocket)
     */
    private String channel;

    /**
     * Priority level
     */
    private Integer priority;

    /**
     * Timestamp when message was created
     */
    private LocalDateTime createdAt;

    /**
     * Notification types
     */
    public static final String TYPE_WORD_FETCHED = "word_fetched";
    public static final String TYPE_AI_COMPLETED = "ai_completed";
    public static final String TYPE_SYSTEM = "system";
    public static final String TYPE_REMINDER = "reminder";

    /**
     * Notification channels
     */
    public static final String CHANNEL_PUSH = "push";
    public static final String CHANNEL_EMAIL = "email";
    public static final String CHANNEL_WEBSOCKET = "websocket";
}
