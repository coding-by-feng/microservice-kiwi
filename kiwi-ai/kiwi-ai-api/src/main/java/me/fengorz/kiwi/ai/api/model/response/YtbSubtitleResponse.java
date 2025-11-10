package me.fengorz.kiwi.ai.api.model.response;

import me.fengorz.kiwi.ai.api.model.request.YtbSubtitleRequest;
import me.fengorz.kiwi.common.api.ws.WsConstants;

public class YtbSubtitleResponse {

    private String type;
    private String message;
    private String chunk;
    private String fullContent;
    private String errorCode;
    private Long timestamp;
    private Long processingDuration;
    private YtbSubtitleRequest originalRequest;

    // Progress information
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

    // Convenience type checking methods
    public boolean ifConnected() { return WsConstants.TYPE_CONNECTED.equals(type); }
    public boolean ifStarted() { return WsConstants.TYPE_STARTED.equals(type); }
    public boolean ifChunk() { return WsConstants.TYPE_CHUNK.equals(type); }
    public boolean ifCompleted() { return WsConstants.TYPE_COMPLETED.equals(type); }
    public boolean ifError() { return WsConstants.TYPE_ERROR.equals(type); }

    // Factory methods for different response types
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

    public static YtbSubtitleResponse progress(String message, YtbSubtitleRequest request) {
        return new YtbSubtitleResponse()
                .setType(WsConstants.TYPE_STARTED)
                .setMessage(message)
                .setOriginalRequest(request);
    }

    public static YtbSubtitleResponse chunk(String chunk, YtbSubtitleRequest request) {
        return new YtbSubtitleResponse()
                .setType(WsConstants.TYPE_CHUNK)
                .setChunk(chunk)
                .setOriginalRequest(request);
    }

    public static YtbSubtitleResponse completed(String message, YtbSubtitleRequest request,
                                                String fullContent, Long processingDuration) {
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