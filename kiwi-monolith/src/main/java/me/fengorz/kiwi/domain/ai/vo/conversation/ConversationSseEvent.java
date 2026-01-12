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

import java.util.List;

/**
 * SSE Event VO for streaming conversation generation
 *
 * @author codingByFeng
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationSseEvent {

    /**
     * Event type: metadata, message, progress, error, complete
     */
    private String eventType;

    /**
     * Event data payload
     */
    private Object data;

    /**
     * Timestamp of the event
     */
    private Long timestamp;

    public static ConversationSseEvent metadata(MetadataPayload payload) {
        return ConversationSseEvent.builder()
                .eventType("metadata")
                .data(payload)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static ConversationSseEvent message(MessageVO message) {
        return ConversationSseEvent.builder()
                .eventType("message")
                .data(message)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static ConversationSseEvent progress(ProgressPayload payload) {
        return ConversationSseEvent.builder()
                .eventType("progress")
                .data(payload)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static ConversationSseEvent error(String errorMessage, String errorCode) {
        return ConversationSseEvent.builder()
                .eventType("error")
                .data(ErrorPayload.builder().message(errorMessage).code(errorCode).build())
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static ConversationSseEvent complete(CompletePayload payload) {
        return ConversationSseEvent.builder()
                .eventType("complete")
                .data(payload)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * Metadata payload sent when script generation is complete
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MetadataPayload {
        private Long conversationId;
        private String topic;
        private List<SpeakerVO> speakers;
        private Integer totalMessageCount;
    }

    /**
     * Progress payload for tracking generation progress
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProgressPayload {
        private Integer completed;
        private Integer total;
        private Integer percentage;
    }

    /**
     * Error payload
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorPayload {
        private String message;
        private String code;
    }

    /**
     * Complete payload when all generation is finished
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompletePayload {
        private Long conversationId;
        private Integer totalMessages;
        private Long totalAudioDurationMs;
        private Long generationTimeMs;
    }
}
