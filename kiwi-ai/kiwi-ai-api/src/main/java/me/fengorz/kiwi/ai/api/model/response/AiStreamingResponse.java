package me.fengorz.kiwi.ai.api.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import me.fengorz.kiwi.ai.api.model.request.AiStreamingRequest;

import java.io.Serializable;

/**
 * WebSocket streaming response model for AI operations
 * 
 * @Author Kason Zhan
 * @Date 06/03/2025
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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

    // Convenience factory methods
    public static AiStreamingResponse connected(String message) {
        return AiStreamingResponse.builder()
                .type("connected")
                .message(message)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static AiStreamingResponse started(String message, AiStreamingRequest request) {
        return AiStreamingResponse.builder()
                .type("started")
                .message(message)
                .request(request)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static AiStreamingResponse chunk(String chunk, AiStreamingRequest request) {
        return AiStreamingResponse.builder()
                .type("chunk")
                .chunk(chunk)
                .request(request)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static AiStreamingResponse chunk(String chunk, Integer chunkIndex, Integer totalChunks, AiStreamingRequest request) {
        return AiStreamingResponse.builder()
                .type("chunk")
                .chunk(chunk)
                .chunkIndex(chunkIndex)
                .totalChunks(totalChunks)
                .request(request)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static AiStreamingResponse completed(String message, AiStreamingRequest request, String fullResponse) {
        return AiStreamingResponse.builder()
                .type("completed")
                .message(message)
                .request(request)
                .fullResponse(fullResponse)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static AiStreamingResponse completed(String message, AiStreamingRequest request, String fullResponse, Long processingDuration) {
        return AiStreamingResponse.builder()
                .type("completed")
                .message(message)
                .request(request)
                .fullResponse(fullResponse)
                .processingDuration(processingDuration)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static AiStreamingResponse error(String message, AiStreamingRequest request) {
        return AiStreamingResponse.builder()
                .type("error")
                .message(message)
                .request(request)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static AiStreamingResponse error(String message, String errorCode, AiStreamingRequest request) {
        return AiStreamingResponse.builder()
                .type("error")
                .message(message)
                .errorCode(errorCode)
                .request(request)
                .timestamp(System.currentTimeMillis())
                .build();
    }
}