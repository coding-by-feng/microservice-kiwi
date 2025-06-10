package me.fengorz.kiwi.ai.api.model.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import me.fengorz.kiwi.ai.api.model.request.AiStreamingRequest;

import java.io.Serializable;

/**
 * WebSocket streaming response model for AI operations
 *
 * @Author Kason Zhan
 * @Date 06/03/2025
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AiStreamingResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    // Response type constants
    public static final String TYPE_CONNECTED = "connected";
    public static final String TYPE_STARTED = "started";
    public static final String TYPE_CHUNK = "chunk";
    public static final String TYPE_COMPLETED = "completed";
    public static final String TYPE_ERROR = "error";

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

    // Convenience methods for type checking

    public boolean ifError() {
        return TYPE_ERROR.equals(type);
    }

    // Convenience factory methods
    public static AiStreamingResponse connected(String message) {
        return AiStreamingResponse.builder()
                .type(TYPE_CONNECTED)
                .message(message)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static AiStreamingResponse started(String message, AiStreamingRequest request) {
        return AiStreamingResponse.builder()
                .type(TYPE_STARTED)
                .message(message)
                .request(request)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static AiStreamingResponse chunk(String chunk, AiStreamingRequest request) {
        return AiStreamingResponse.builder()
                .type(TYPE_CHUNK)
                .chunk(chunk)
                .request(request)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static AiStreamingResponse chunk(String chunk, Integer chunkIndex, Integer totalChunks, AiStreamingRequest request) {
        return AiStreamingResponse.builder()
                .type(TYPE_CHUNK)
                .chunk(chunk)
                .chunkIndex(chunkIndex)
                .totalChunks(totalChunks)
                .request(request)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static AiStreamingResponse completed(String message, AiStreamingRequest request, String fullResponse) {
        return AiStreamingResponse.builder()
                .type(TYPE_COMPLETED)
                .message(message)
                .request(request)
                .fullResponse(fullResponse)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static AiStreamingResponse completed(String message, AiStreamingRequest request, String fullResponse, Long processingDuration) {
        return AiStreamingResponse.builder()
                .type(TYPE_COMPLETED)
                .message(message)
                .request(request)
                .fullResponse(fullResponse)
                .processingDuration(processingDuration)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static AiStreamingResponse error(String message, AiStreamingRequest request) {
        return AiStreamingResponse.builder()
                .type(TYPE_ERROR)
                .message(message)
                .request(request)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static AiStreamingResponse error(String message, String errorCode, AiStreamingRequest request) {
        return AiStreamingResponse.builder()
                .type(TYPE_ERROR)
                .message(message)
                .errorCode(errorCode)
                .request(request)
                .timestamp(System.currentTimeMillis())
                .build();
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
            case TYPE_CONNECTED:
                return message != null;
            case TYPE_STARTED:
                return message != null && request != null;
            case TYPE_CHUNK:
                return chunk != null && request != null;
            case TYPE_COMPLETED:
                return message != null && request != null && fullResponse != null;
            case TYPE_ERROR:
                return message != null;
            default:
                return false;
        }
    }
}