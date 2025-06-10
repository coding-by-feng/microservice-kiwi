package me.fengorz.kiwi.ai.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.ai.api.model.request.AiStreamingRequest;
import me.fengorz.kiwi.ai.api.model.response.AiStreamingResponse;
import me.fengorz.kiwi.ai.service.AiStreamingService;
import me.fengorz.kiwi.ai.util.LanguageConvertor;
import me.fengorz.kiwi.common.sdk.enumeration.AiPromptModeEnum;
import me.fengorz.kiwi.common.sdk.enumeration.LanguageEnum;
import me.fengorz.kiwi.common.sdk.web.WebTools;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class AiStreamingWebSocketHandler extends TextWebSocketHandler {

    private final AiStreamingService aiStreamingService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public AiStreamingWebSocketHandler(@Qualifier("grokStreamingService") AiStreamingService aiStreamingService) {
        this.aiStreamingService = aiStreamingService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.put(session.getId(), session);
        log.info("AI Streaming WebSocket connection established: {}", session.getId());

        // Send welcome message
        sendMessage(session, AiStreamingResponse.connected("AI Streaming connection established"));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            String payload = message.getPayload();
            log.info("Received AI streaming request from session {}: {}", session.getId(), payload);

            // Parse the incoming request
            AiStreamingRequest request = objectMapper.readValue(payload, AiStreamingRequest.class);

            // Validate request
            if (!validateRequest(session, request)) {
                return;
            }

            // Set timestamp if not provided
            if (request.getTimestamp() == null) {
                request.setTimestamp(System.currentTimeMillis());
            }

            // Process the AI streaming request
            processAiStreamingRequest(session, request);

        } catch (Exception e) {
            log.error("Error processing AI streaming message from session {}: {}", session.getId(), e.getMessage(), e);
            sendMessage(session, AiStreamingResponse.error("Failed to process request: " + e.getMessage(), null));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session.getId());
        log.info("AI Streaming WebSocket connection closed: {}, status: {}", session.getId(), status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("AI Streaming transport error for session {}: {}", session.getId(), exception.getMessage(), exception);
        sessions.remove(session.getId());
    }

    private boolean validateRequest(WebSocketSession session, AiStreamingRequest request) {
        if (!StringUtils.hasText(request.getPrompt())) {
            sendMessage(session, AiStreamingResponse.error("Prompt cannot be empty", "INVALID_PROMPT", request));
            return false;
        }

        if (!StringUtils.hasText(request.getPromptMode())) {
            sendMessage(session, AiStreamingResponse.error("Prompt mode cannot be empty", "INVALID_PROMPT_MODE", request));
            return false;
        }

        if (!StringUtils.hasText(request.getTargetLanguage())) {
            sendMessage(session, AiStreamingResponse.error("Target language cannot be empty", "INVALID_TARGET_LANGUAGE", request));
            return false;
        }

        // Validate prompt mode enum
        try {
            AiPromptModeEnum.valueOf(request.getPromptMode());
        } catch (IllegalArgumentException e) {
            sendMessage(session, AiStreamingResponse.error("Invalid prompt mode: " + request.getPromptMode(), "INVALID_PROMPT_MODE", request));
            return false;
        }

        // Validate target language
        try {
            LanguageConvertor.convertLanguageToEnum(request.getTargetLanguage());
        } catch (Exception e) {
            sendMessage(session, AiStreamingResponse.error("Invalid target language: " + request.getTargetLanguage(), "INVALID_TARGET_LANGUAGE", request));
            return false;
        }

        // Validate native language if provided
        if (StringUtils.hasText(request.getNativeLanguage())) {
            try {
                LanguageConvertor.convertLanguageToEnum(request.getNativeLanguage());
            } catch (Exception e) {
                sendMessage(session, AiStreamingResponse.error("Invalid native language: " + request.getNativeLanguage(), "INVALID_NATIVE_LANGUAGE", request));
                return false;
            }
        }

        return true;
    }

    private void processAiStreamingRequest(WebSocketSession session, AiStreamingRequest request) {
        try {
            log.info("Processing AI streaming request for session: {}", session.getId());

            long startTime = System.currentTimeMillis();

            // Send processing started message
            sendMessage(session, AiStreamingResponse.started("AI streaming started", request));

            // Decode the original text
            String decodedText = WebTools.decode(request.getPrompt());

            // Set up streaming callbacks
            StringBuilder fullResponse = new StringBuilder();

            if (StringUtils.hasText(request.getNativeLanguage())) {
                // Two-language mode
                LanguageEnum targetLang = LanguageConvertor.convertLanguageToEnum(request.getTargetLanguage());
                LanguageEnum nativeLang = LanguageConvertor.convertLanguageToEnum(request.getNativeLanguage());

                aiStreamingService.streamCall(
                        decodedText,
                        AiPromptModeEnum.valueOf(request.getPromptMode()),
                        targetLang,
                        nativeLang,
                        // onChunk callback
                        chunk -> {
                            fullResponse.append(chunk);
                            sendMessage(session, AiStreamingResponse.chunk(chunk, request));
                        },
                        // onError callback
                        error -> {
                            log.error("AI streaming error for session {}: {}", session.getId(), error.getMessage(), error);
                            sendMessage(session, AiStreamingResponse.error("AI streaming failed: " + error.getMessage(), "STREAMING_ERROR", request));
                        },
                        // onComplete callback
                        () -> {
                            long processingDuration = System.currentTimeMillis() - startTime;
                            log.info("AI streaming completed for session: {} in {}ms", session.getId(), processingDuration);
                            sendMessage(session, AiStreamingResponse.completed("AI streaming completed", request, fullResponse.toString(), processingDuration));
                        }
                );
            } else {
                // Single language mode
                LanguageEnum language = LanguageConvertor.convertLanguageToEnum(request.getTargetLanguage());

                aiStreamingService.streamCall(
                        decodedText,
                        AiPromptModeEnum.valueOf(request.getPromptMode()),
                        language,
                        // onChunk callback
                        chunk -> {
                            fullResponse.append(chunk);
                            sendMessage(session, AiStreamingResponse.chunk(chunk, request));
                        },
                        // onError callback
                        error -> {
                            log.error("AI streaming error for session {}: {}", session.getId(), error.getMessage(), error);
                            sendMessage(session, AiStreamingResponse.error("AI streaming failed: " + error.getMessage(), "STREAMING_ERROR", request));
                        },
                        // onComplete callback
                        () -> {
                            long processingDuration = System.currentTimeMillis() - startTime;
                            log.info("AI streaming completed for session: {} in {}ms", session.getId(), processingDuration);
                            sendMessage(session, AiStreamingResponse.completed("AI streaming completed", request, fullResponse.toString(), processingDuration));
                        }
                );
            }

        } catch (Exception e) {
            log.error("Error processing AI streaming request for session {}: {}", session.getId(), e.getMessage(), e);
            sendMessage(session, AiStreamingResponse.error("AI streaming request failed: " + e.getMessage(), "REQUEST_ERROR", request));
        }
    }

    private void sendMessage(WebSocketSession session, AiStreamingResponse response) {
        try {
            if (session.isOpen()) {
                String jsonResponse = objectMapper.writeValueAsString(response);
                session.sendMessage(new TextMessage(jsonResponse));
            }
        } catch (IOException e) {
            log.error("Failed to send message to session {}: {}", session.getId(), e.getMessage(), e);
        }
    }
}