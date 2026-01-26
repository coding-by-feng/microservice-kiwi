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
package me.fengorz.kiwi.api.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.enumeration.AiPromptModeEnum;
import me.fengorz.kiwi.common.enumeration.LanguageEnum;
import me.fengorz.kiwi.common.util.WebTools;
import me.fengorz.kiwi.domain.ai.service.AiCallHistoryService;
import me.fengorz.kiwi.domain.ai.service.AiStreamingService;
import me.fengorz.kiwi.ws.model.AiStreamingRequest;
import me.fengorz.kiwi.ws.model.AiStreamingResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import me.fengorz.kiwi.domain.ai.entity.AiCallHistory;
import me.fengorz.kiwi.security.KiwiUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

/**
 * AI Streaming SSE Controller
 * Replaces WebSocket with Server-Sent Events for AI streaming responses
 *
 * @author codingByFeng
 */
@Slf4j
@RestController
@RequestMapping("/api/ai/sse")
@Tag(name = "AI SSE Streaming", description = "AI streaming operations using Server-Sent Events")
public class AiStreamingSseController {

    private static final String LOG_PREFIX = "[AI-SSE]";
    private static final long SSE_TIMEOUT_MS = 300000L; // 5 minutes

    private final AiStreamingService aiStreamingService;
    private final AiCallHistoryService aiCallHistoryService;
    private final Executor taskExecutor;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AiStreamingSseController(AiStreamingService aiStreamingService,
                                     AiCallHistoryService aiCallHistoryService,
                                     @Qualifier("webSocketExecutor") Executor taskExecutor) {
        this.aiStreamingService = aiStreamingService;
        this.aiCallHistoryService = aiCallHistoryService;
        this.taskExecutor = taskExecutor;
    }

    /**
     * AI streaming endpoint using SSE
     *
     * @param prompt         the prompt text (URL encoded)
     * @param promptMode     the prompt mode (e.g., DIRECTLY_TRANSLATION, GRAMMAR_EXPLANATION)
     * @param targetLanguage the target language code
     * @param nativeLanguage the native language code (optional)
     * @param userId         the user ID (optional, from header)
     * @return SSE emitter for streaming AI responses
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "AI streaming with SSE", description = "Stream AI responses using Server-Sent Events")
    public SseEmitter streamAi(
            @RequestParam("prompt") String prompt,
            @RequestParam("promptMode") String promptMode,
            @RequestParam("targetLanguage") String targetLanguage,
            @RequestParam(value = "nativeLanguage", required = false) String nativeLanguage,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {

        log.info("{} Stream request - promptMode: {}, targetLanguage: {}", LOG_PREFIX, promptMode, targetLanguage);

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);

        emitter.onCompletion(() -> log.info("{} SSE connection completed", LOG_PREFIX));
        emitter.onTimeout(() -> log.warn("{} SSE connection timed out", LOG_PREFIX));
        emitter.onError(e -> log.error("{} SSE error: {}", LOG_PREFIX, e.getMessage()));

        // Build request object
        AiStreamingRequest request = AiStreamingRequest.builder()
                .prompt(prompt)
                .promptMode(promptMode)
                .targetLanguage(targetLanguage)
                .nativeLanguage(nativeLanguage)
                .timestamp(System.currentTimeMillis())
                .build();

        // Validate request
        String validationError = validateRequest(request);
        if (validationError != null) {
            log.warn("{} Validation failed: {}", LOG_PREFIX, validationError);
            sendErrorAndComplete(emitter, validationError, "VALIDATION_ERROR", request);
            return emitter;
        }

        // Log call history and get history ID for updating response later
        AtomicReference<Long> historyIdRef = new AtomicReference<>();
        if (userId != null) {
            try {
                AiCallHistory history = aiCallHistoryService.logCall(
                        userId,
                        null,
                        request.getPrompt(),
                        request.getPromptMode(),
                        request.getTargetLanguage(),
                        request.getNativeLanguage()
                );
                historyIdRef.set(history.getId());
            } catch (Exception e) {
                log.error("{} Failed to save call history: {}", LOG_PREFIX, e.getMessage());
            }
        }

        // Process AI streaming in background
        taskExecutor.execute(() -> processAiStreaming(emitter, request, historyIdRef.get()));

        return emitter;
    }

    /**
     * AI streaming endpoint using POST (for longer prompts)
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "AI streaming with SSE (POST)", description = "Stream AI responses using POST for longer prompts")
    public SseEmitter streamAiPost(
            @RequestBody AiStreamingRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {

        log.info("{} Stream POST request - promptMode: {}, targetLanguage: {}",
                LOG_PREFIX, request.getPromptMode(), request.getTargetLanguage());

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);

        emitter.onCompletion(() -> log.info("{} SSE connection completed", LOG_PREFIX));
        emitter.onTimeout(() -> log.warn("{} SSE connection timed out", LOG_PREFIX));
        emitter.onError(e -> log.error("{} SSE error: {}", LOG_PREFIX, e.getMessage()));

        if (request.getTimestamp() == null) {
            request.setTimestamp(System.currentTimeMillis());
        }

        // Validate request
        String validationError = validateRequest(request);
        if (validationError != null) {
            log.warn("{} Validation failed: {}", LOG_PREFIX, validationError);
            sendErrorAndComplete(emitter, validationError, "VALIDATION_ERROR", request);
            return emitter;
        }

        // Log call history and get history ID for updating response later
        AtomicReference<Long> historyIdRef = new AtomicReference<>();
        if (userId != null) {
            try {
                AiCallHistory history = aiCallHistoryService.logCall(
                        userId,
                        request.getAiUrl(),
                        request.getPrompt(),
                        request.getPromptMode(),
                        request.getTargetLanguage(),
                        request.getNativeLanguage()
                );
                historyIdRef.set(history.getId());
            } catch (Exception e) {
                log.error("{} Failed to save call history: {}", LOG_PREFIX, e.getMessage());
            }
        }

        // Process AI streaming in background
        taskExecutor.execute(() -> processAiStreaming(emitter, request, historyIdRef.get()));

        return emitter;
    }

    /**
     * Regenerate AI response for a history record
     */
    @PostMapping(value = "/regenerate/{historyId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Regenerate AI response", description = "Re-call AI with the same prompt from history and update the stored response")
    public SseEmitter regenerateResponse(
            @AuthenticationPrincipal KiwiUser user,
            @PathVariable Long historyId) {

        log.info("{} Regenerate request for history {} by user {}", LOG_PREFIX, historyId, user.getUserId());

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);

        emitter.onCompletion(() -> log.info("{} SSE connection completed", LOG_PREFIX));
        emitter.onTimeout(() -> log.warn("{} SSE connection timed out", LOG_PREFIX));
        emitter.onError(e -> log.error("{} SSE error: {}", LOG_PREFIX, e.getMessage()));

        // Find history record and verify ownership
        Optional<AiCallHistory> historyOpt = aiCallHistoryService.findByIdAndUserId(historyId, user.getUserId().longValue());

        if (historyOpt.isEmpty()) {
            // Check if it exists but belongs to another user
            Optional<AiCallHistory> anyHistory = aiCallHistoryService.findById(historyId);
            if (anyHistory.isPresent()) {
                sendErrorAndComplete(emitter, "You don't have permission to regenerate this history", "UNAUTHORIZED", null);
            } else {
                sendErrorAndComplete(emitter, "History record not found", "NOT_FOUND", null);
            }
            return emitter;
        }

        AiCallHistory history = historyOpt.get();

        // Build request from history
        AiStreamingRequest request = AiStreamingRequest.builder()
                .prompt(history.getPrompt())
                .promptMode(history.getPromptMode())
                .targetLanguage(history.getTargetLanguage())
                .nativeLanguage(history.getNativeLanguage())
                .aiUrl(history.getAiUrl())
                .timestamp(System.currentTimeMillis())
                .build();

        // Validate the stored request data
        String validationError = validateRequest(request);
        if (validationError != null) {
            log.warn("{} Validation failed for history {}: {}", LOG_PREFIX, historyId, validationError);
            sendErrorAndComplete(emitter, validationError, "VALIDATION_ERROR", request);
            return emitter;
        }

        // Process AI streaming in background
        taskExecutor.execute(() -> processAiStreaming(emitter, request, historyId));

        return emitter;
    }

    private String validateRequest(AiStreamingRequest request) {
        if (!StringUtils.hasText(request.getPrompt())) {
            return "Prompt cannot be empty";
        }

        if (!StringUtils.hasText(request.getPromptMode())) {
            return "Prompt mode cannot be empty";
        }

        try {
            AiPromptModeEnum.fromMode(request.getPromptMode());
        } catch (IllegalArgumentException e) {
            return "Invalid prompt mode: " + request.getPromptMode();
        }

        if (!StringUtils.hasText(request.getTargetLanguage())) {
            return "Target language cannot be empty";
        }

        try {
            LanguageEnum.fromCode(request.getTargetLanguage());
        } catch (Exception e) {
            return "Invalid target language: " + request.getTargetLanguage();
        }

        if (StringUtils.hasText(request.getNativeLanguage())) {
            try {
                LanguageEnum.fromCode(request.getNativeLanguage());
            } catch (Exception e) {
                return "Invalid native language: " + request.getNativeLanguage();
            }
        }

        return null;
    }

    private void processAiStreaming(SseEmitter emitter, AiStreamingRequest request, Long historyId) {
        try {
            log.info("{} Starting AI streaming", LOG_PREFIX);

            long startTime = System.currentTimeMillis();

            // Send started event
            sendEvent(emitter, "started", AiStreamingResponse.started("AI streaming started", request));

            String decodedText = WebTools.decode(request.getPrompt());
            StringBuilder fullResponse = new StringBuilder();

            LanguageEnum targetLang = LanguageEnum.fromCode(request.getTargetLanguage());
            LanguageEnum nativeLang = StringUtils.hasText(request.getNativeLanguage())
                    ? LanguageEnum.fromCode(request.getNativeLanguage())
                    : targetLang;

            aiStreamingService.streamCall(
                    decodedText,
                    AiPromptModeEnum.fromMode(request.getPromptMode()),
                    targetLang,
                    nativeLang,
                    // onChunk
                    chunk -> {
                        fullResponse.append(chunk);
                        sendEvent(emitter, "chunk", AiStreamingResponse.chunk(chunk, request));
                    },
                    // onError
                    error -> {
                        log.error("{} Streaming error: {}", LOG_PREFIX, error.getMessage());
                        sendErrorAndComplete(emitter, "AI streaming failed: " + error.getMessage(),
                                "STREAMING_ERROR", request);
                    },
                    // onComplete
                    () -> {
                        long duration = System.currentTimeMillis() - startTime;
                        log.info("{} Streaming completed - Duration: {}ms", LOG_PREFIX, duration);

                        // Save AI response to history
                        if (historyId != null) {
                            try {
                                aiCallHistoryService.updateAiResponse(historyId, fullResponse.toString());
                                log.info("{} Saved AI response to history {}", LOG_PREFIX, historyId);
                            } catch (Exception e) {
                                log.error("{} Failed to save AI response to history {}: {}", LOG_PREFIX, historyId, e.getMessage());
                            }
                        }

                        sendEvent(emitter, "completed",
                                AiStreamingResponse.completed("AI streaming completed", request, fullResponse.toString(), duration));
                        emitter.complete();
                    }
            );

        } catch (Exception e) {
            log.error("{} Error processing request: {}", LOG_PREFIX, e.getMessage(), e);
            sendErrorAndComplete(emitter, "AI streaming request failed: " + e.getMessage(),
                    "REQUEST_ERROR", request);
        }
    }

    private void sendEvent(SseEmitter emitter, String eventName, Object data) {
        try {
            String json = objectMapper.writeValueAsString(data);
            emitter.send(SseEmitter.event().name(eventName).data(json, MediaType.APPLICATION_JSON));
        } catch (IOException e) {
            log.error("{} Failed to send event: {}", LOG_PREFIX, e.getMessage());
        }
    }

    private void sendErrorAndComplete(SseEmitter emitter, String message, String errorCode, AiStreamingRequest request) {
        try {
            sendEvent(emitter, "error", AiStreamingResponse.error(message, errorCode, request));
            emitter.complete();
        } catch (Exception e) {
            log.error("{} Failed to send error: {}", LOG_PREFIX, e.getMessage());
            try {
                emitter.completeWithError(e);
            } catch (Exception ignored) {
            }
        }
    }
}
