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
package me.fengorz.kiwi.ws.model;

import me.fengorz.kiwi.ws.WsConstants;

/**
 * YouTube Subtitle WebSocket Response
 *
 * @author codingByFeng
 */
public class YtbSubtitleResponse {

    private String type;
    private String message;
    private String chunk;
    private String fullContent;
    private String errorCode;
    private Long timestamp;
    private Long processingDuration;
    private YtbSubtitleRequest originalRequest;
    private Integer currentStep;
    private Integer totalSteps;
    private String currentStepDescription;

    public YtbSubtitleResponse() {
        this.timestamp = System.currentTimeMillis();
    }

    // Getters
    public String getType() { return type; }
    public String getMessage() { return message; }
    public String getChunk() { return chunk; }
    public String getFullContent() { return fullContent; }
    public String getErrorCode() { return errorCode; }
    public Long getTimestamp() { return timestamp; }
    public Long getProcessingDuration() { return processingDuration; }
    public YtbSubtitleRequest getOriginalRequest() { return originalRequest; }
    public Integer getCurrentStep() { return currentStep; }
    public Integer getTotalSteps() { return totalSteps; }
    public String getCurrentStepDescription() { return currentStepDescription; }

    // Chainable setters
    public YtbSubtitleResponse setType(String type) { this.type = type; return this; }
    public YtbSubtitleResponse setMessage(String message) { this.message = message; return this; }
    public YtbSubtitleResponse setChunk(String chunk) { this.chunk = chunk; return this; }
    public YtbSubtitleResponse setFullContent(String fullContent) { this.fullContent = fullContent; return this; }
    public YtbSubtitleResponse setErrorCode(String errorCode) { this.errorCode = errorCode; return this; }
    public YtbSubtitleResponse setTimestamp(Long timestamp) { this.timestamp = timestamp; return this; }
    public YtbSubtitleResponse setProcessingDuration(Long processingDuration) { this.processingDuration = processingDuration; return this; }
    public YtbSubtitleResponse setOriginalRequest(YtbSubtitleRequest originalRequest) { this.originalRequest = originalRequest; return this; }
    public YtbSubtitleResponse setCurrentStep(Integer currentStep) { this.currentStep = currentStep; return this; }
    public YtbSubtitleResponse setTotalSteps(Integer totalSteps) { this.totalSteps = totalSteps; return this; }
    public YtbSubtitleResponse setCurrentStepDescription(String currentStepDescription) { this.currentStepDescription = currentStepDescription; return this; }

    // Factory methods
    public static YtbSubtitleResponse connected(String message) {
        return new YtbSubtitleResponse()
                .setType(WsConstants.TYPE_CONNECTED)
                .setMessage(message);
    }

    public static YtbSubtitleResponse started(String message, YtbSubtitleRequest request) {
        return new YtbSubtitleResponse()
                .setType(WsConstants.TYPE_STARTED)
                .setMessage(message)
                .setOriginalRequest(request)
                .setCurrentStep(1)
                .setTotalSteps(3)
                .setCurrentStepDescription("Initializing subtitle processing");
    }

    public static YtbSubtitleResponse chunk(String chunk, YtbSubtitleRequest request) {
        return new YtbSubtitleResponse()
                .setType(WsConstants.TYPE_CHUNK)
                .setChunk(chunk)
                .setOriginalRequest(request);
    }

    public static YtbSubtitleResponse completed(String message, YtbSubtitleRequest request, String fullContent, Long processingDuration) {
        return new YtbSubtitleResponse()
                .setType(WsConstants.TYPE_COMPLETED)
                .setMessage(message)
                .setFullContent(fullContent)
                .setProcessingDuration(processingDuration)
                .setOriginalRequest(request)
                .setCurrentStep(3)
                .setTotalSteps(3)
                .setCurrentStepDescription("Subtitle processing completed");
    }

    public static YtbSubtitleResponse error(String message, String errorCode, YtbSubtitleRequest request) {
        return new YtbSubtitleResponse()
                .setType(WsConstants.TYPE_ERROR)
                .setMessage(message)
                .setErrorCode(errorCode)
                .setOriginalRequest(request);
    }
}
