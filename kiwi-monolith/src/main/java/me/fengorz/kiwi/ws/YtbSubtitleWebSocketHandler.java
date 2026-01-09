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
import me.fengorz.kiwi.common.enumeration.LanguageEnum;
import me.fengorz.kiwi.domain.ai.service.YtbSubtitleService;
import me.fengorz.kiwi.ws.model.ValidationResult;
import me.fengorz.kiwi.ws.model.YtbSubtitleRequest;
import me.fengorz.kiwi.ws.model.YtbSubtitleResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

/**
 * YouTube Subtitle WebSocket Handler
 *
 * @author codingByFeng
 */
@Slf4j
@Component
public class YtbSubtitleWebSocketHandler extends TextWebSocketHandler {

    private static final String LOG_PREFIX = "[YTB-SUBTITLE-WS]";

    private final YtbSubtitleService ytbSubtitleService;
    private final Executor webSocketExecutor;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, Boolean> activeStreams = new ConcurrentHashMap<>();

    public YtbSubtitleWebSocketHandler(YtbSubtitleService ytbSubtitleService,
                                       @Qualifier("webSocketExecutor") Executor webSocketExecutor) {
        this.ytbSubtitleService = ytbSubtitleService;
        this.webSocketExecutor = webSocketExecutor;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String sessionId = session.getId();
        sessions.put(sessionId, session);
        activeStreams.put(sessionId, true);

        log.info("{} Connection established - SessionId: {}", LOG_PREFIX, sessionId);

        sendMessage(session, YtbSubtitleResponse.connected("YouTube Subtitle WebSocket connection established"));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String sessionId = session.getId();
        String payload = message.getPayload();

        log.info("{} Received message - SessionId: {}, Length: {}", LOG_PREFIX, sessionId, payload.length());

        try {
            // Handle ping/pong messages for keep-alive
            if (isPingMessage(payload)) {
                log.debug("{} Received ping message - SessionId: {}", LOG_PREFIX, sessionId);
                sendPongMessage(session);
                return;
            }

            YtbSubtitleRequest request = objectMapper.readValue(payload, YtbSubtitleRequest.class);

            ValidationResult validation = validateRequest(request);
            if (!validation.isValid()) {
                log.warn("{} Validation failed: {}", LOG_PREFIX, validation.getErrorMessage());
                sendMessage(session, YtbSubtitleResponse.error(validation.getErrorMessage(), validation.getErrorCode(), request));
                return;
            }

            processSubtitleRequest(session, request);

        } catch (Exception e) {
            log.error("{} Error processing message: {}", LOG_PREFIX, e.getMessage(), e);
            sendMessage(session, YtbSubtitleResponse.error("Failed to process request: " + e.getMessage(), "PROCESSING_ERROR", null));
        }
    }

    private boolean isPingMessage(String payload) {
        try {
            var node = objectMapper.readTree(payload);
            return node.has("type") && "ping".equals(node.get("type").asText());
        } catch (Exception e) {
            return false;
        }
    }

    private void sendPongMessage(WebSocketSession session) {
        try {
            if (session.isOpen()) {
                String pong = objectMapper.writeValueAsString(Map.of("type", "pong", "ts", System.currentTimeMillis()));
                session.sendMessage(new TextMessage(pong));
            }
        } catch (IOException e) {
            log.error("{} Failed to send pong message: {}", LOG_PREFIX, e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String sessionId = session.getId();
        sessions.remove(sessionId);
        activeStreams.put(sessionId, false);
        log.info("{} Connection closed - SessionId: {}, Status: {}", LOG_PREFIX, sessionId, status.getCode());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        String sessionId = session.getId();
        log.error("{} Transport error - SessionId: {}: {}", LOG_PREFIX, sessionId, exception.getMessage());
        sessions.remove(sessionId);
        activeStreams.put(sessionId, false);
    }

    private ValidationResult validateRequest(YtbSubtitleRequest request) {
        if (!StringUtils.hasText(request.getVideoUrl())) {
            return ValidationResult.invalid("Video URL cannot be empty", "INVALID_VIDEO_URL");
        }

        if (!StringUtils.hasText(request.getRequestType())) {
            return ValidationResult.invalid("Request type cannot be empty", "INVALID_REQUEST_TYPE");
        }

        if (StringUtils.hasText(request.getLanguage()) && !"null".equals(request.getLanguage())) {
            try {
                LanguageEnum.fromCode(request.getLanguage());
            } catch (Exception e) {
                return ValidationResult.invalid("Invalid language: " + request.getLanguage(), "INVALID_LANGUAGE");
            }
        }

        return ValidationResult.valid();
    }

    private void processSubtitleRequest(WebSocketSession session, YtbSubtitleRequest request) {
        String sessionId = session.getId();

        CompletableFuture.runAsync(() -> {
            try {
                log.info("{} Starting subtitle processing - SessionId: {}", LOG_PREFIX, sessionId);

                long startTime = System.currentTimeMillis();

                sendMessage(session, YtbSubtitleResponse.started("Subtitle processing started", request));

                String requestType = request.getRequestType() != null ? request.getRequestType().trim().toLowerCase() : "";
                String effectiveLanguage = "scrolling".equals(requestType) ? null : request.getLanguage();

                String result;
                if ("scrolling".equals(requestType)) {
                    // Get scrolling subtitles (no translation)
                    var subtitlesResult = ytbSubtitleService.getScrollingSubtitles(request.getVideoUrl());
                    result = subtitlesResult != null ? subtitlesResult.toString() : null;
                } else {
                    // Get translated subtitles
                    result = ytbSubtitleService.getTranslatedSubtitles(request.getVideoUrl(), effectiveLanguage);
                }

                long duration = System.currentTimeMillis() - startTime;
                log.info("{} Subtitle processing completed - Duration: {}ms", LOG_PREFIX, duration);

                if (isSessionActive(sessionId)) {
                    // Send the full result as a single chunk
                    if (result != null) {
                        sendMessage(session, YtbSubtitleResponse.chunk(result, request));
                    }
                    sendMessage(session, YtbSubtitleResponse.completed("Subtitle processing completed", request, result, duration));
                }

            } catch (Exception e) {
                log.error("{} Error processing subtitle request: {}", LOG_PREFIX, e.getMessage(), e);
                if (isSessionActive(sessionId)) {
                    sendMessage(session, YtbSubtitleResponse.error("Subtitle processing failed: " + e.getMessage(), "PROCESSING_ERROR", request));
                }
            }
        }, webSocketExecutor);
    }

    private void sendMessage(WebSocketSession session, YtbSubtitleResponse response) {
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
}
