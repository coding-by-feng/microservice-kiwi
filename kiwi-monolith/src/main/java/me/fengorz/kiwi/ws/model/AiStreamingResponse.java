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

import java.io.Serializable;

/**
 * WebSocket streaming response model for AI operations
 *
 * @author codingByFeng
 */
public class AiStreamingResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private String type;
    private String message;
    private String chunk;
    private Integer chunkIndex;
    private Integer totalChunks;
    private String fullResponse;
    private Long timestamp;
    private AiStreamingRequest request;
    private Long processingDuration;
    private String errorCode;
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

    // Factory methods
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

    public static AiStreamingResponse completed(String message, AiStreamingRequest request, String fullResponse, Long processingDuration) {
        return new AiStreamingResponse()
                .setType(WsConstants.TYPE_COMPLETED)
                .setMessage(message)
                .setRequest(request)
                .setFullResponse(fullResponse)
                .setProcessingDuration(processingDuration)
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
}
