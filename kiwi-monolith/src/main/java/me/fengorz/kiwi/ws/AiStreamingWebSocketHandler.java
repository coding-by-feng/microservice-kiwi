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
package me.fengorz.kiwi.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.enumeration.AiPromptModeEnum;
import me.fengorz.kiwi.common.enumeration.LanguageEnum;
import me.fengorz.kiwi.common.util.WebTools;
import me.fengorz.kiwi.domain.ai.service.AiCallHistoryService;
import me.fengorz.kiwi.domain.ai.service.AiStreamingService;
import me.fengorz.kiwi.ws.model.AiStreamingRequest;
import me.fengorz.kiwi.ws.model.AiStreamingResponse;
import me.fengorz.kiwi.ws.model.ValidationResult;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI Streaming WebSocket Handler
 *
 * @author codingByFeng
 */
@Slf4j
@Component
public class AiStreamingWebSocketHandler extends TextWebSocketHandler {

    private static final String LOG_PREFIX = "[AI-WS]";

    private final AiStreamingService aiStreamingService;
    private final AiCallHistoryService aiCallHistoryService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, Boolean> activeStreams = new ConcurrentHashMap<>();

    public AiStreamingWebSocketHandler(AiStreamingService aiStreamingService,
                                        AiCallHistoryService aiCallHistoryService) {
        this.aiStreamingService = aiStreamingService;
        this.aiCallHistoryService = aiCallHistoryService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        sessions.put(sessionId, session);
        activeStreams.put(sessionId, true);

        log.info("{} Connection established - SessionId: {}", LOG_PREFIX, sessionId);

        AiStreamingResponse welcomeResponse = AiStreamingResponse.connected("AI Streaming connection established");
        sendMessage(session, welcomeResponse);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String sessionId = session.getId();
        String payload = message.getPayload();

        log.info("{} Received message - SessionId: {}, Length: {}", LOG_PREFIX, sessionId, payload.length());

        try {
            AiStreamingRequest request = objectMapper.readValue(payload, AiStreamingRequest.class);

            ValidationResult validation = validateRequest(request);
            if (!validation.isValid()) {
                log.warn("{} Validation failed: {}", LOG_PREFIX, validation.getErrorMessage());
                sendMessage(session, AiStreamingResponse.error(validation.getErrorMessage(), validation.getErrorCode(), request));
                return;
            }

            if (request.getTimestamp() == null) {
                request.setTimestamp(System.currentTimeMillis());
            }

            // Save call history
            try {
                Integer userId = getUserIdFromSession(session);
                if (userId != null) {
                    aiCallHistoryService.logCall(
                            Long.valueOf(userId),
                            request.getAiUrl(),
                            request.getPrompt(),
                            request.getPromptMode(),
                            request.getTargetLanguage(),
                            request.getNativeLanguage()
                    );
                }
            } catch (Exception e) {
                log.error("{} Failed to save call history: {}", LOG_PREFIX, e.getMessage());
            }

            processAiStreamingRequest(session, request);

        } catch (Exception e) {
            log.error("{} Error processing message: {}", LOG_PREFIX, e.getMessage(), e);
            sendMessage(session, AiStreamingResponse.error("Failed to process request: " + e.getMessage(), "PROCESSING_ERROR", null));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = session.getId();
        sessions.remove(sessionId);
        activeStreams.put(sessionId, false);
        log.info("{} Connection closed - SessionId: {}, Status: {}", LOG_PREFIX, sessionId, status.getCode());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String sessionId = session.getId();
        log.error("{} Transport error - SessionId: {}: {}", LOG_PREFIX, sessionId, exception.getMessage());
        sessions.remove(sessionId);
        activeStreams.put(sessionId, false);
    }

    private ValidationResult validateRequest(AiStreamingRequest request) {
        if (!StringUtils.hasText(request.getPrompt())) {
            return ValidationResult.invalid("Prompt cannot be empty", "INVALID_PROMPT");
        }

        if (!StringUtils.hasText(request.getPromptMode())) {
            return ValidationResult.invalid("Prompt mode cannot be empty", "INVALID_PROMPT_MODE");
        }

        try {
            AiPromptModeEnum.fromMode(request.getPromptMode());
        } catch (IllegalArgumentException e) {
            return ValidationResult.invalid("Invalid prompt mode: " + request.getPromptMode(), "INVALID_PROMPT_MODE");
        }

        if (!StringUtils.hasText(request.getTargetLanguage())) {
            return ValidationResult.invalid("Target language cannot be empty", "INVALID_TARGET_LANGUAGE");
        }

        try {
            LanguageEnum.fromCode(request.getTargetLanguage());
        } catch (Exception e) {
            return ValidationResult.invalid("Invalid target language: " + request.getTargetLanguage(), "INVALID_TARGET_LANGUAGE");
        }

        if (StringUtils.hasText(request.getNativeLanguage())) {
            try {
                LanguageEnum.fromCode(request.getNativeLanguage());
            } catch (Exception e) {
                return ValidationResult.invalid("Invalid native language: " + request.getNativeLanguage(), "INVALID_NATIVE_LANGUAGE");
            }
        }

        return ValidationResult.valid();
    }

    private void processAiStreamingRequest(WebSocketSession session, AiStreamingRequest request) {
        String sessionId = session.getId();

        try {
            log.info("{} Starting AI streaming - SessionId: {}", LOG_PREFIX, sessionId);

            long startTime = System.currentTimeMillis();

            sendMessage(session, AiStreamingResponse.started("AI streaming started", request));

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
                        if (isSessionActive(sessionId)) {
                            fullResponse.append(chunk);
                            sendMessage(session, AiStreamingResponse.chunk(chunk, request));
                        }
                    },
                    // onError
                    error -> {
                        if (isSessionActive(sessionId)) {
                            log.error("{} Streaming error: {}", LOG_PREFIX, error.getMessage());
                            sendMessage(session, AiStreamingResponse.error("AI streaming failed: " + error.getMessage(), "STREAMING_ERROR", request));
                        }
                        cleanupSession(sessionId);
                    },
                    // onComplete
                    () -> {
                        long duration = System.currentTimeMillis() - startTime;
                        log.info("{} Streaming completed - Duration: {}ms", LOG_PREFIX, duration);
                        if (isSessionActive(sessionId)) {
                            sendMessage(session, AiStreamingResponse.completed("AI streaming completed", request, fullResponse.toString(), duration));
                        }
                        cleanupSession(sessionId);
                    }
            );

        } catch (Exception e) {
            log.error("{} Error processing request: {}", LOG_PREFIX, e.getMessage(), e);
            sendMessage(session, AiStreamingResponse.error("AI streaming request failed: " + e.getMessage(), "REQUEST_ERROR", request));
        }
    }

    private void sendMessage(WebSocketSession session, AiStreamingResponse response) {
        String sessionId = session.getId();
        if (!isSessionActive(sessionId)) {
            return;
        }

        try {
            if (session.isOpen()) {
                String json = objectMapper.writeValueAsString(response);
                session.sendMessage(new TextMessage(json));
            }
        } catch (IOException e) {
            log.error("{} Failed to send message: {}", LOG_PREFIX, e.getMessage());
            activeStreams.put(sessionId, false);
        }
    }

    private boolean isSessionActive(String sessionId) {
        return activeStreams.getOrDefault(sessionId, false)
                && sessions.containsKey(sessionId)
                && sessions.get(sessionId).isOpen();
    }

    private void cleanupSession(String sessionId) {
        activeStreams.remove(sessionId);
    }

    private Integer getUserIdFromSession(WebSocketSession session) {
        try {
            Object userId = session.getAttributes().get("userId");
            if (userId instanceof Integer) {
                return (Integer) userId;
            } else if (userId != null) {
                return Integer.valueOf(userId.toString());
            }
        } catch (Exception e) {
            log.debug("Failed to get user ID from session: {}", e.getMessage());
        }
        return null;
    }
}
