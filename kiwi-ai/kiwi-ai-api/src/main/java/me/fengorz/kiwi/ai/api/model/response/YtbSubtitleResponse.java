package me.fengorz.kiwi.ai.api.model.response;

import lombok.Data;
import lombok.experimental.Accessors;
import me.fengorz.kiwi.ai.api.model.request.YtbSubtitleRequest;
import me.fengorz.kiwi.common.api.ws.WsConstants;

@Data
@Accessors(chain = true)
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

    // Convenience type checking methods
    public boolean ifConnected() {
        return WsConstants.TYPE_CONNECTED.equals(type);
    }

    public boolean ifStarted() {
        return WsConstants.TYPE_STARTED.equals(type);
    }

    public boolean ifChunk() {
        return WsConstants.TYPE_CHUNK.equals(type);
    }

    public boolean ifCompleted() {
        return WsConstants.TYPE_COMPLETED.equals(type);
    }

    public boolean ifError() {
        return WsConstants.TYPE_ERROR.equals(type);
    }

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