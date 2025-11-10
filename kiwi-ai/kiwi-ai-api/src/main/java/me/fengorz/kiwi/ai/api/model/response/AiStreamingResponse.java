package me.fengorz.kiwi.ai.api.model.response;

import me.fengorz.kiwi.ai.api.model.request.AiStreamingRequest;
import me.fengorz.kiwi.common.api.ws.WsConstants;

import java.io.Serializable;

/**
 * WebSocket streaming response model for AI operations
 *
 * @Author Kason Zhan
 * @Date 06/03/2025
 */
public class AiStreamingResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Response type: connected, started, chunk, completed, error
     */
    private String type;

    /**
     * Status or error message
     */
    private String message;

    /**
     * Text chunk for streaming responses
     */
    private String chunk;

    /**
     * Index of current chunk (for ordering)
     */
    private Integer chunkIndex;

    /**
     * Total number of chunks expected
     */
    private Integer totalChunks;

    /**
     * Complete response text (sent with completed type)
     */
    private String fullResponse;

    /**
     * Response timestamp
     */
    private Long timestamp;

    /**
     * Original request that triggered this response
     */
    private AiStreamingRequest request;

    /**
     * Processing duration in milliseconds (optional)
     */
    private Long processingDuration;

    /**
     * Error code (if type is error)
     */
    private String errorCode;

    /**
     * Additional response metadata
     */
    private String metadata;

    public AiStreamingResponse() {}

    // Getters
    public String getType() { return type; }
    public String getMessage() { return message; }
    public String getChunk() { return chunk; }
    public Integer getChunkIndex() { return chunkIndex; }
    public Integer getTotalChunks() { return totalChunks; }
    public String getFullResponse() { return fullResponse; }
    public Long getTimestamp() { return timestamp; }
    public AiStreamingRequest getRequest() { return request; }
    public Long getProcessingDuration() { return processingDuration; }
    public String getErrorCode() { return errorCode; }
    public String getMetadata() { return metadata; }

    // Chainable setters
    public AiStreamingResponse setType(String type) { this.type = type; return this; }
    public AiStreamingResponse setMessage(String message) { this.message = message; return this; }
    public AiStreamingResponse setChunk(String chunk) { this.chunk = chunk; return this; }
    public AiStreamingResponse setChunkIndex(Integer chunkIndex) { this.chunkIndex = chunkIndex; return this; }
    public AiStreamingResponse setTotalChunks(Integer totalChunks) { this.totalChunks = totalChunks; return this; }
    public AiStreamingResponse setFullResponse(String fullResponse) { this.fullResponse = fullResponse; return this; }
    public AiStreamingResponse setTimestamp(Long timestamp) { this.timestamp = timestamp; return this; }
    public AiStreamingResponse setRequest(AiStreamingRequest request) { this.request = request; return this; }
    public AiStreamingResponse setProcessingDuration(Long processingDuration) { this.processingDuration = processingDuration; return this; }
    public AiStreamingResponse setErrorCode(String errorCode) { this.errorCode = errorCode; return this; }
    public AiStreamingResponse setMetadata(String metadata) { this.metadata = metadata; return this; }

    // Convenience methods for type checking

    public boolean ifError() {
        return WsConstants.TYPE_ERROR.equals(type);
    }

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

    // Convenience factory methods
    public static AiStreamingResponse connected(String message) {
        return new AiStreamingResponse()
                .setType(WsConstants.TYPE_CONNECTED)
                .setMessage(message)
                .setTimestamp(System.currentTimeMillis());
    }

    public static AiStreamingResponse started(String message, AiStreamingRequest request) {
        return new AiStreamingResponse()
                .setType(WsConstants.TYPE_STARTED)
                .setMessage(message)
                .setRequest(request)
                .setTimestamp(System.currentTimeMillis());
    }

    public static AiStreamingResponse chunk(String chunk, AiStreamingRequest request) {
        return new AiStreamingResponse()
                .setType(WsConstants.TYPE_CHUNK)
                .setChunk(chunk)
                .setRequest(request)
                .setTimestamp(System.currentTimeMillis());
    }

    public static AiStreamingResponse chunk(String chunk, Integer chunkIndex, Integer totalChunks, AiStreamingRequest request) {
        return new AiStreamingResponse()
                .setType(WsConstants.TYPE_CHUNK)
                .setChunk(chunk)
                .setChunkIndex(chunkIndex)
                .setTotalChunks(totalChunks)
                .setRequest(request)
                .setTimestamp(System.currentTimeMillis());
    }

    public static AiStreamingResponse completed(String message, AiStreamingRequest request, String fullResponse) {
        return new AiStreamingResponse()
                .setType(WsConstants.TYPE_COMPLETED)
                .setMessage(message)
                .setRequest(request)
                .setFullResponse(fullResponse)
                .setTimestamp(System.currentTimeMillis());
    }

    public static AiStreamingResponse completed(String message, AiStreamingRequest request, String fullResponse, Long processingDuration) {
        return new AiStreamingResponse()
                .setType(WsConstants.TYPE_COMPLETED)
                .setMessage(message)
                .setRequest(request)
                .setFullResponse(fullResponse)
                .setProcessingDuration(processingDuration)
                .setTimestamp(System.currentTimeMillis());
    }

    public static AiStreamingResponse error(String message, AiStreamingRequest request) {
        return new AiStreamingResponse()
                .setType(WsConstants.TYPE_ERROR)
                .setMessage(message)
                .setRequest(request)
                .setTimestamp(System.currentTimeMillis());
    }

    public static AiStreamingResponse error(String message, String errorCode, AiStreamingRequest request) {
        return new AiStreamingResponse()
                .setType(WsConstants.TYPE_ERROR)
                .setMessage(message)
                .setErrorCode(errorCode)
                .setRequest(request)
                .setTimestamp(System.currentTimeMillis());
    }

    /**
     * Validates that the response has all required fields for its type
     * @return true if valid, false otherwise
     */
    public boolean ifValid() {
        if (type == null || timestamp == null) {
            return false;
        }

        switch (type) {
            case WsConstants.TYPE_CONNECTED:
                return message != null;
            case WsConstants.TYPE_STARTED:
                return message != null && request != null;
            case WsConstants.TYPE_CHUNK:
                return chunk != null && request != null;
            case WsConstants.TYPE_COMPLETED:
                return message != null && request != null && fullResponse != null;
            case WsConstants.TYPE_ERROR:
                return message != null;
            default:
                return false;
        }
    }
}