package me.fengorz.kiwi.ai.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.ai.api.model.request.YtbSubtitleRequest;
import me.fengorz.kiwi.ai.api.model.response.YtbSubtitleResponse;
import me.fengorz.kiwi.ai.model.ValidationResult;
import me.fengorz.kiwi.ai.service.ytb.YtbSubtitleStreamingService;
import me.fengorz.kiwi.ai.util.LanguageConvertor;
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

@Slf4j
@Component
public class YtbSubtitleWebSocketHandler extends TextWebSocketHandler {

    private static final String LOG_PREFIX = "[YTB-SUBTITLE-WS]";
    private static final String VALIDATION_PREFIX = "[VALIDATION]";
    private static final String REQUEST_PREFIX = "[REQUEST]";
    private static final String RESPONSE_PREFIX = "[RESPONSE]";

    private final YtbSubtitleStreamingService subtitleStreamingService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, Boolean> activeStreams = new ConcurrentHashMap<>();
    // Aggregate chunks per session to provide fullContent on completion
    private final Map<String, StringBuilder> sessionBuffers = new ConcurrentHashMap<>();

    public YtbSubtitleWebSocketHandler(YtbSubtitleStreamingService subtitleStreamingService) {
        this.subtitleStreamingService = subtitleStreamingService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String sessionId = session.getId();
        sessions.put(sessionId, session);
        activeStreams.put(sessionId, true);
        // buffer will be created lazily when processing starts

        log.info("{} Connection established - SessionId: {}, RemoteAddress: {}",
                LOG_PREFIX, sessionId, session.getRemoteAddress());

        // Send welcome message
        YtbSubtitleResponse welcomeResponse = YtbSubtitleResponse
                .connected("YouTube Subtitle WebSocket connection established");
        sendMessageWithLogging(session, welcomeResponse, "CONNECTION_ESTABLISHED");
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String sessionId = session.getId();
        String payload = message.getPayload();

        log.info("{} {} Received message - SessionId: {}, PayloadLength: {}",
                LOG_PREFIX, REQUEST_PREFIX, sessionId, payload.length());
        log.debug("{} {} Raw payload: {}", LOG_PREFIX, REQUEST_PREFIX, payload);

        try {
            // Parse the incoming request
            YtbSubtitleRequest request = objectMapper.readValue(payload, YtbSubtitleRequest.class);

            log.info("{} {} Parsed request - SessionId: {}, VideoUrl: {}, Language: {}, RequestType: {}",
                    LOG_PREFIX, REQUEST_PREFIX, sessionId,
                    request.getVideoUrl(),
                    request.getLanguage(),
                    request.getRequestType());

            // Validate request
            ValidationResult validation = validateRequest(request);
            if (!validation.isValid()) {
                log.warn("{} {} Validation failed - SessionId: {}, Error: {}",
                        LOG_PREFIX, VALIDATION_PREFIX, sessionId, validation.getErrorMessage());

                YtbSubtitleResponse errorResponse = YtbSubtitleResponse.error(
                        validation.getErrorMessage(),
                        validation.getErrorCode(),
                        request);
                sendMessageWithLogging(session, errorResponse, "VALIDATION_ERROR");
                return;
            }

            log.info("{} {} Validation passed - SessionId: {}",
                    LOG_PREFIX, VALIDATION_PREFIX, sessionId);

            // Process the subtitle request asynchronously
            processSubtitleRequest(session, request);

        } catch (Exception e) {
            log.error("{} Error processing message - SessionId: {}, Error: {}",
                    LOG_PREFIX, sessionId, e.getMessage(), e);

            YtbSubtitleResponse errorResponse = YtbSubtitleResponse.error(
                    "Failed to process request: " + e.getMessage(),
                    "PROCESSING_ERROR",
                    null);
            sendMessageWithLogging(session, errorResponse, "PROCESSING_ERROR");
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String sessionId = session.getId();
        sessions.remove(sessionId);
        activeStreams.put(sessionId, false);
        sessionBuffers.remove(sessionId);

        log.info("{} Connection closed - SessionId: {}, Code: {}, Reason: '{}'",
                LOG_PREFIX, sessionId, status.getCode(), status.getReason());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        String sessionId = session.getId();
        log.error("{} Transport error - SessionId: {}, Error: {}",
                LOG_PREFIX, sessionId, exception.getMessage(), exception);

        sessions.remove(sessionId);
        activeStreams.put(sessionId, false);
        sessionBuffers.remove(sessionId);

        if (session.isOpen()) {
            YtbSubtitleResponse errorResponse = YtbSubtitleResponse.error(
                    "Transport error occurred",
                    "TRANSPORT_ERROR",
                    null);
            sendMessageWithLogging(session, errorResponse, "TRANSPORT_ERROR");
        }
    }

    private ValidationResult validateRequest(YtbSubtitleRequest request) {
        log.debug("{} {} Starting validation for request", LOG_PREFIX, VALIDATION_PREFIX);

        // Check video URL
        if (!StringUtils.hasText(request.getVideoUrl())) {
            return ValidationResult.invalid("Video URL cannot be empty", "INVALID_VIDEO_URL");
        }

        // Check request type
        if (!StringUtils.hasText(request.getRequestType())) {
            return ValidationResult.invalid("Request type cannot be empty", "INVALID_REQUEST_TYPE");
        }

        // Validate language if provided (for translation requests)
        if (StringUtils.hasText(request.getLanguage()) && !"null".equals(request.getLanguage())) {
            try {
                LanguageConvertor.convertLanguageToEnum(request.getLanguage());
            } catch (Exception e) {
                return ValidationResult.invalid("Invalid language: " + request.getLanguage(), "INVALID_LANGUAGE");
            }
        }

        log.debug("{} {} All validations passed", LOG_PREFIX, VALIDATION_PREFIX);
        return ValidationResult.valid();
    }

    private void processSubtitleRequest(WebSocketSession session, YtbSubtitleRequest request) {
        String sessionId = session.getId();

        CompletableFuture.runAsync(() -> {
            try {
                log.info("{} Starting subtitle processing - SessionId: {}", LOG_PREFIX, sessionId);

                long startTime = System.currentTimeMillis();

                // Initialize aggregation buffer for this session/request
                sessionBuffers.put(sessionId, new StringBuilder());

                // Send processing started message
                YtbSubtitleResponse startedResponse = YtbSubtitleResponse.started("Subtitle processing started",
                        request);
                sendMessageWithLogging(session, startedResponse, "PROCESSING_STARTED");

                // Determine effective language: if requestType is 'scrolling', bypass
                // translation
                String requestType = request.getRequestType() != null ? request.getRequestType().trim().toLowerCase()
                        : "";
                String effectiveLanguage = "scrolling".equals(requestType) ? null : request.getLanguage();

                // Use the streaming service to handle the request
                subtitleStreamingService.streamSubtitleTranslation(
                        request.getVideoUrl(),
                        effectiveLanguage,
                        // onChunk callback
                        chunk -> {
                            if (isSessionActive(sessionId)) {
                                // Append chunk to buffer for fullContent on completion
                                StringBuilder buffer = sessionBuffers.computeIfAbsent(sessionId,
                                        k -> new StringBuilder());
                                buffer.append(chunk);

                                log.debug("{} Received chunk: '{}'", LOG_PREFIX, chunk);
                                YtbSubtitleResponse chunkResponse = YtbSubtitleResponse.chunk(chunk, request);
                                sendMessageWithLogging(session, chunkResponse, "AI_CHUNK");
                            }
                        },
                        // onError callback
                        error -> {
                            if (isSessionActive(sessionId)) {
                                log.error("{} Subtitle processing error - SessionId: {}, Error: {}",
                                        LOG_PREFIX, sessionId, error.getMessage(), error);
                                YtbSubtitleResponse errorResponse = YtbSubtitleResponse.error(
                                        "Subtitle processing failed: " + error.getMessage(),
                                        "PROCESSING_ERROR",
                                        request);
                                sendMessageWithLogging(session, errorResponse, "PROCESSING_ERROR");
                            }
                            // Clean up buffer on error
                            sessionBuffers.remove(sessionId);
                        },
                        // onComplete callback
                        () -> {
                            long processingDuration = System.currentTimeMillis() - startTime;
                            log.info("{} Subtitle processing completed - SessionId: {}, Duration: {}ms",
                                    LOG_PREFIX, sessionId, processingDuration);

                            if (isSessionActive(sessionId)) {
                                String fullContent = null;
                                StringBuilder buffer = sessionBuffers.remove(sessionId);
                                if (buffer != null && buffer.length() > 0) {
                                    fullContent = buffer.toString();
                                }

                                YtbSubtitleResponse completedResponse = YtbSubtitleResponse.completed(
                                        "Subtitle processing completed successfully",
                                        request,
                                        fullContent, // aggregated full content from chunks
                                        processingDuration);
                                sendMessageWithLogging(session, completedResponse, "COMPLETED");
                            } else {
                                // Ensure buffer cleanup if session inactive
                                sessionBuffers.remove(sessionId);
                            }
                        });

            } catch (Exception e) {
                log.error("{} Error processing subtitle request - SessionId: {}, Error: {}",
                        LOG_PREFIX, sessionId, e.getMessage(), e);

                YtbSubtitleResponse errorResponse = YtbSubtitleResponse.error(
                        "Subtitle processing failed: " + e.getMessage(),
                        "PROCESSING_ERROR",
                        request);
                sendMessageWithLogging(session, errorResponse, "PROCESSING_ERROR");
                // Clean up buffer on exception
                sessionBuffers.remove(sessionId);
            }
        });
    }

    private boolean isSessionActive(String sessionId) {
        return activeStreams.getOrDefault(sessionId, false) &&
                sessions.containsKey(sessionId) &&
                sessions.get(sessionId).isOpen();
    }

    private void sendMessageWithLogging(WebSocketSession session, YtbSubtitleResponse response, String messageType) {
        String sessionId = session.getId();

        if (!isSessionActive(sessionId)) {
            log.debug("{} {} Skipping message for inactive session - SessionId: {}, MessageType: {}",
                    LOG_PREFIX, RESPONSE_PREFIX, sessionId, messageType);
            return;
        }

        try {
            if (session.isOpen()) {
                String jsonResponse = objectMapper.writeValueAsString(response);

                if ("AI_CHUNK".equals(messageType)) {
                    log.debug("{} {} Sending AI chunk - SessionId: {}, ChunkLength: {}",
                            LOG_PREFIX, RESPONSE_PREFIX, sessionId,
                            response.getChunk() != null ? response.getChunk().length() : 0);
                } else {
                    log.info("{} {} Sending response - SessionId: {}, Type: {}, ResponseType: {}",
                            LOG_PREFIX, RESPONSE_PREFIX, sessionId, messageType, response.getType());
                }

                session.sendMessage(new TextMessage(jsonResponse));
            } else {
                log.warn("{} {} Cannot send message, session closed - SessionId: {}, MessageType: {}",
                        LOG_PREFIX, RESPONSE_PREFIX, sessionId, messageType);
                activeStreams.put(sessionId, false);
            }
        } catch (IOException e) {
            log.error("{} {} Failed to send message - SessionId: {}, MessageType: {}, Error: {}",
                    LOG_PREFIX, RESPONSE_PREFIX, sessionId, messageType, e.getMessage(), e);
            activeStreams.put(sessionId, false);
        }
    }

}