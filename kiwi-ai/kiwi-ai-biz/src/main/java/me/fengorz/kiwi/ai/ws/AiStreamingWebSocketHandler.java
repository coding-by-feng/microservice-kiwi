package me.fengorz.kiwi.ai.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.ai.api.model.request.AiStreamingRequest;
import me.fengorz.kiwi.ai.api.model.response.AiStreamingResponse;
import me.fengorz.kiwi.ai.model.ValidationResult;
import me.fengorz.kiwi.ai.service.AiStreamingService;
import me.fengorz.kiwi.ai.service.history.AiCallHistoryService;
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

    private static final String LOG_PREFIX = "[AI-WS]";
    private static final String VALIDATION_PREFIX = "[VALIDATION]";
    private static final String REQUEST_PREFIX = "[REQUEST]";
    private static final String RESPONSE_PREFIX = "[RESPONSE]";

    private final AiStreamingService aiStreamingService;
    private final AiCallHistoryService aiCallHistoryService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, Boolean> activeStreams = new ConcurrentHashMap<>();

    public AiStreamingWebSocketHandler(@Qualifier("grokStreamingService") AiStreamingService aiStreamingService,
                                       AiCallHistoryService aiCallHistoryService) {
        this.aiStreamingService = aiStreamingService;
        this.aiCallHistoryService = aiCallHistoryService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        sessions.put(sessionId, session);
        activeStreams.put(sessionId, true);

        log.info("{} Connection established - SessionId: {}, RemoteAddress: {}",
                LOG_PREFIX, sessionId, session.getRemoteAddress());

        // Send welcome message
        AiStreamingResponse welcomeResponse = AiStreamingResponse.connected("AI Streaming connection established");
        sendMessageWithLogging(session, welcomeResponse, "CONNECTION_ESTABLISHED");
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String sessionId = session.getId();
        String payload = message.getPayload();

        log.info("{} {} Received message - SessionId: {}, PayloadLength: {}",
                LOG_PREFIX, REQUEST_PREFIX, sessionId, payload.length());
        log.debug("{} {} Raw payload: {}", LOG_PREFIX, REQUEST_PREFIX, payload);

        try {
            // Parse the incoming request
            AiStreamingRequest request = objectMapper.readValue(payload, AiStreamingRequest.class);

            log.info("{} {} Parsed request - SessionId: {}, PromptMode: {}, TargetLanguage: {}, NativeLanguage: {}, AiUrl: {}, PromptLength: {}",
                    LOG_PREFIX, REQUEST_PREFIX, sessionId,
                    request.getPromptMode(),
                    request.getTargetLanguage(),
                    request.getNativeLanguage(),
                    request.getAiUrl(),
                    request.getPrompt() != null ? request.getPrompt().length() : 0);

            // Validate request
            ValidationResult validation = validateRequest(request);
            if (!validation.isValid()) {
                log.warn("{} {} Validation failed - SessionId: {}, Error: {}",
                        LOG_PREFIX, VALIDATION_PREFIX, sessionId, validation.getErrorMessage());

                AiStreamingResponse errorResponse = AiStreamingResponse.error(
                        validation.getErrorMessage(),
                        validation.getErrorCode(),
                        request);
                sendMessageWithLogging(session, errorResponse, "VALIDATION_ERROR");
                return;
            }

            log.info("{} {} Validation passed - SessionId: {}",
                    LOG_PREFIX, VALIDATION_PREFIX, sessionId);

            // Set timestamp if not provided
            if (request.getTimestamp() == null) {
                request.setTimestamp(System.currentTimeMillis());
            }

            // Save call history to database
            try {
                Integer currentUserId = getUserIdFromSession(session);
                if (currentUserId != null) {
                    Long historyId = aiCallHistoryService.saveCallHistory(request, Long.valueOf(currentUserId));
                    log.info("{} Saved call history with ID: {} for user: {}", LOG_PREFIX, historyId, currentUserId);
                } else {
                    log.warn("{} No authenticated user found, skipping call history save", LOG_PREFIX);
                }
            } catch (Exception e) {
                log.error("{} Failed to save call history: {}", LOG_PREFIX, e.getMessage(), e);
                // Don't fail the request, just log the error
            }

            // Process the AI streaming request
            processAiStreamingRequest(session, request);

        } catch (Exception e) {
            log.error("{} Error processing message - SessionId: {}, Error: {}",
                    LOG_PREFIX, sessionId, e.getMessage(), e);

            AiStreamingResponse errorResponse = AiStreamingResponse.error(
                    "Failed to process request: " + e.getMessage(),
                    "PROCESSING_ERROR",
                    null);
            sendMessageWithLogging(session, errorResponse, "PROCESSING_ERROR");
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = session.getId();
        sessions.remove(sessionId);
        activeStreams.put(sessionId, false); // Mark as inactive but keep for cleanup

        log.info("{} Connection closed - SessionId: {}, Code: {}, Reason: '{}'",
                LOG_PREFIX, sessionId, status.getCode(), status.getReason());

        // Log specific details for abnormal closures
        if (status.getCode() == 1005) {
            log.warn("{} Connection closed with code 1005 (No Status) - likely network issue or client disconnect", LOG_PREFIX);
        } else if (status.getCode() != 1000) {
            log.warn("{} Connection closed abnormally - Code: {}", LOG_PREFIX, status.getCode());
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String sessionId = session.getId();
        log.error("{} Transport error - SessionId: {}, Error: {}",
                LOG_PREFIX, sessionId, exception.getMessage(), exception);

        sessions.remove(sessionId);
        activeStreams.put(sessionId, false); // Mark as inactive

        if (session.isOpen()) {
            AiStreamingResponse errorResponse = AiStreamingResponse.error(
                    "Transport error occurred",
                    "TRANSPORT_ERROR",
                    null);
            sendMessageWithLogging(session, errorResponse, "TRANSPORT_ERROR");
        }
    }

    private ValidationResult validateRequest(AiStreamingRequest request) {
        log.debug("{} {} Starting validation for request", LOG_PREFIX, VALIDATION_PREFIX);

        // Check prompt
        if (!StringUtils.hasText(request.getPrompt())) {
            return ValidationResult.invalid("Prompt cannot be empty", "INVALID_PROMPT");
        }
        log.debug("{} {} Prompt validation passed", LOG_PREFIX, VALIDATION_PREFIX);

        // Check prompt mode
        if (!StringUtils.hasText(request.getPromptMode())) {
            return ValidationResult.invalid("Prompt mode cannot be empty", "INVALID_PROMPT_MODE");
        }

        // Validate prompt mode enum
        try {
            AiPromptModeEnum promptMode = AiPromptModeEnum.fromMode(request.getPromptMode());
            log.debug("{} {} Prompt mode validation passed: {}", LOG_PREFIX, VALIDATION_PREFIX, promptMode);
        } catch (IllegalArgumentException e) {
            return ValidationResult.invalid("Invalid prompt mode: " + request.getPromptMode(), "INVALID_PROMPT_MODE");
        }

        // Check target language
        if (!StringUtils.hasText(request.getTargetLanguage())) {
            return ValidationResult.invalid("Target language cannot be empty", "INVALID_TARGET_LANGUAGE");
        }

        // Validate target language
        try {
            LanguageEnum targetLang = LanguageConvertor.convertLanguageToEnum(request.getTargetLanguage());
            log.debug("{} {} Target language validation passed: {}", LOG_PREFIX, VALIDATION_PREFIX, targetLang);
        } catch (Exception e) {
            return ValidationResult.invalid("Invalid target language: " + request.getTargetLanguage(), "INVALID_TARGET_LANGUAGE");
        }

        // Validate native language if provided
        if (StringUtils.hasText(request.getNativeLanguage())) {
            try {
                LanguageEnum nativeLang = LanguageConvertor.convertLanguageToEnum(request.getNativeLanguage());
                log.debug("{} {} Native language validation passed: {}", LOG_PREFIX, VALIDATION_PREFIX, nativeLang);
            } catch (Exception e) {
                return ValidationResult.invalid("Invalid native language: " + request.getNativeLanguage(), "INVALID_NATIVE_LANGUAGE");
            }
        }

        log.debug("{} {} All validations passed", LOG_PREFIX, VALIDATION_PREFIX);
        return ValidationResult.valid();
    }

    private void processAiStreamingRequest(WebSocketSession session, AiStreamingRequest request) {
        String sessionId = session.getId();

        try {
            log.info("{} Starting AI streaming - SessionId: {}", LOG_PREFIX, sessionId);

            long startTime = System.currentTimeMillis();

            // Send processing started message
            AiStreamingResponse startedResponse = AiStreamingResponse.started("AI streaming started", request);
            sendMessageWithLogging(session, startedResponse, "PROCESSING_STARTED");

            // Decode the original text
            String decodedText = WebTools.decode(request.getPrompt());
            log.debug("{} Decoded text length: {}", LOG_PREFIX, decodedText.length());

            // Set up streaming callbacks
            StringBuilder fullResponse = new StringBuilder();

            // Two-language mode
            LanguageEnum targetLang = LanguageConvertor.convertLanguageToEnum(request.getTargetLanguage());
            LanguageEnum nativeLang = LanguageConvertor.convertLanguageToEnum(request.getNativeLanguage());

            log.info("{} Two-language mode - SessionId: {}, Target: {}, Native: {}",
                    LOG_PREFIX, sessionId, targetLang, nativeLang);

            aiStreamingService.streamCall(
                    decodedText,
                    AiPromptModeEnum.fromMode(request.getPromptMode()),
                    targetLang,
                    nativeLang,
                    // onChunk callback with session check
                    chunk -> {
                        if (isSessionActive(sessionId)) {
                            log.debug("🔍 Received chunk from AI: '{}'", chunk);
                            fullResponse.append(chunk);
                            AiStreamingResponse chunkResponse = AiStreamingResponse.chunk(chunk, request);
                            log.debug("🔍 Created chunk response - type: {}, chunk: '{}', chunkIsNull: {}",
                                    chunkResponse.getType(), chunkResponse.getChunk(), chunkResponse.getChunk() == null);
                            sendMessageWithLogging(session, chunkResponse, "CHUNK");
                        } else {
                            log.debug("{} Skipping chunk for inactive session: {}", LOG_PREFIX, sessionId);
                        }
                    },
                    // onError callback
                    error -> {
                        if (isSessionActive(sessionId)) {
                            log.error("{} AI streaming error - SessionId: {}, Error: {}",
                                    LOG_PREFIX, sessionId, error.getMessage(), error);
                            AiStreamingResponse errorResponse = AiStreamingResponse.error(
                                    "AI streaming failed: " + error.getMessage(),
                                    "STREAMING_ERROR",
                                    request);
                            sendMessageWithLogging(session, errorResponse, "STREAMING_ERROR");
                        }
                        cleanupSession(sessionId);
                    },
                    // onComplete callback
                    () -> {
                        long processingDuration = System.currentTimeMillis() - startTime;
                        log.info("{} AI streaming completed - SessionId: {}, Duration: {}ms, ResponseLength: {}",
                                LOG_PREFIX, sessionId, processingDuration, fullResponse.length());

                        if (isSessionActive(sessionId)) {
                            AiStreamingResponse completedResponse = AiStreamingResponse.completed(
                                    "AI streaming completed",
                                    request,
                                    fullResponse.toString(),
                                    processingDuration);
                            sendMessageWithLogging(session, completedResponse, "COMPLETED");
                        }
                        cleanupSession(sessionId);
                    }
            );

        } catch (Exception e) {
            log.error("{} Error processing AI streaming request - SessionId: {}, Error: {}",
                    LOG_PREFIX, sessionId, e.getMessage(), e);

            AiStreamingResponse errorResponse = AiStreamingResponse.error(
                    "AI streaming request failed: " + e.getMessage(),
                    "REQUEST_ERROR",
                    request);
            sendMessageWithLogging(session, errorResponse, "REQUEST_ERROR");
        }
    }

    private void sendMessageWithLogging(WebSocketSession session, AiStreamingResponse response, String messageType) {
        String sessionId = session.getId();

        // Check if session is still active before sending
        if (!isSessionActive(sessionId)) {
            log.debug("{} {} Skipping message for inactive session - SessionId: {}, MessageType: {}",
                    LOG_PREFIX, RESPONSE_PREFIX, sessionId, messageType);
            return;
        }

        try {
            if (session.isOpen()) {
                String jsonResponse = objectMapper.writeValueAsString(response);

                // Log response details based on type
                if ("CHUNK".equals(messageType)) {
                    log.debug("{} {} Sending chunk - SessionId: {}, ChunkLength: {}",
                            LOG_PREFIX, RESPONSE_PREFIX, sessionId,
                            response.getChunk() != null ? response.getChunk().length() : 0);
                } else {
                    log.info("{} {} Sending response - SessionId: {}, Type: {}, ResponseType: {}",
                            LOG_PREFIX, RESPONSE_PREFIX, sessionId, messageType, response.getType());
                    log.debug("{} {} Response content: {}", LOG_PREFIX, RESPONSE_PREFIX, jsonResponse);
                }

                session.sendMessage(new TextMessage(jsonResponse));
            } else {
                log.warn("{} {} Cannot send message, session closed - SessionId: {}, MessageType: {}",
                        LOG_PREFIX, RESPONSE_PREFIX, sessionId, messageType);
                // Mark session as inactive if we discover it's closed
                activeStreams.put(sessionId, false);
            }
        } catch (IOException e) {
            log.error("{} {} Failed to send message - SessionId: {}, MessageType: {}, Error: {}",
                    LOG_PREFIX, RESPONSE_PREFIX, sessionId, messageType, e.getMessage(), e);
            // Mark session as inactive on send failure
            activeStreams.put(sessionId, false);
        }
    }

    /**
     * Check if a session is still active (connected and able to receive messages)
     */
    private boolean isSessionActive(String sessionId) {
        return activeStreams.getOrDefault(sessionId, false) &&
                sessions.containsKey(sessionId) &&
                sessions.get(sessionId).isOpen();
    }

    /**
     * Clean up session resources
     */
    private void cleanupSession(String sessionId) {
        activeStreams.remove(sessionId);
        log.debug("{} Cleaned up session resources - SessionId: {}", LOG_PREFIX, sessionId);
    }

    /**
     * Get user ID from WebSocket session attributes
     */
    private Integer getUserIdFromSession(WebSocketSession session) {
        try {
            Object userId = session.getAttributes().get("userId");
            if (userId instanceof Integer) {
                log.debug("Retrieved user ID from session: {}", userId);
                return (Integer) userId;
            } else if (userId != null) {
                log.debug("Converting user ID from session: {} ({})", userId, userId.getClass().getSimpleName());
                return Integer.valueOf(userId.toString());
            }
        } catch (Exception e) {
            log.debug("Failed to get user ID from session: {}", e.getMessage());
        }
        return null;
    }

}