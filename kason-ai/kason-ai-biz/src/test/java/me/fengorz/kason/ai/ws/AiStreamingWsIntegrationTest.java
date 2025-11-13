package me.fengorz.kason.ai.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kason.ai.AiApplication;
import me.fengorz.kason.ai.api.model.request.AiStreamingRequest;
import me.fengorz.kason.ai.api.model.response.AiStreamingResponse;
import me.fengorz.kason.common.sdk.constant.EnvConstants;
import me.fengorz.kason.common.sdk.enumeration.LanguageEnum;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static me.fengorz.kason.common.sdk.enumeration.AiPromptModeEnum.*;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@ActiveProfiles(EnvConstants.TEST)
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = AiApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AiStreamingWsIntegrationTest {

    @LocalServerPort
    private int port;

    private CountDownLatch connectionLatch;
    private CountDownLatch completionLatch;
    private CountDownLatch errorLatch;
    private List<AiStreamingResponse> receivedMessages;
    private WebSocketSession session;
    private ObjectMapper objectMapper;
    private AtomicInteger chunkCount;

    @BeforeEach
    void setUp() throws Exception {
        connectionLatch = new CountDownLatch(1);
        completionLatch = new CountDownLatch(1);
        errorLatch = new CountDownLatch(1);
        receivedMessages = new ArrayList<>();
        objectMapper = new ObjectMapper();
        chunkCount = new AtomicInteger(0);

        // Connect to AI Streaming WebSocket endpoint
        String url = "ws://localhost:" + port + "/ai/ws/stream";
        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();

        session = new StandardWebSocketClient().doHandshake(new TextWebSocketHandler() {
            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) {
                try {
                    String payload = message.getPayload();
                    log.debug("Received message: {}", payload);

                    AiStreamingResponse response = objectMapper.readValue(payload, AiStreamingResponse.class);
                    receivedMessages.add(response);

                    switch (response.getType()) {
                        case "connected":
                            log.info("WebSocket connected successfully");
                            connectionLatch.countDown();
                            break;
                        case "chunk":
                            chunkCount.incrementAndGet();
                            log.debug("Received chunk #{}: {}", chunkCount.get(), response.getChunk());
                            break;
                        case "completed":
                            log.info("AI streaming completed");
                            completionLatch.countDown();
                            break;
                        case "error":
                            log.error("Received error: {}", response.getMessage());
                            errorLatch.countDown();
                            break;
                    }
                } catch (Exception e) {
                    log.error("Error parsing received message: {}", e.getMessage(), e);
                }
            }

            @Override
            public void handleTransportError(WebSocketSession session, Throwable exception) {
                log.error("WebSocket transport error: {}", exception.getMessage(), exception);
                errorLatch.countDown();
            }
        }, headers, new URI(url)).get(10, TimeUnit.SECONDS);
    }

    @AfterEach
    void tearDown() throws IOException {
        if (session != null && session.isOpen()) {
            session.close(CloseStatus.NORMAL);
        }
    }

    @Test
    void testAiStreamingTranslation_Successful() throws Exception {
        // Wait for connection
        assertTrue(connectionLatch.await(5, TimeUnit.SECONDS), "Should connect within 5 seconds");

        // Create AI streaming request
        AiStreamingRequest request = AiStreamingRequest.builder()
                .requestId("test-translation-" + System.currentTimeMillis())
                .prompt("Hello%2C%20how%20are%20you%20today%3F") // URL encoded "Hello, how are you today?"
                .promptMode(DIRECTLY_TRANSLATION.getMode())
                .targetLanguage(LanguageEnum.ZH_CN.getCode())
                .nativeLanguage(LanguageEnum.EN.getCode())
                .timestamp(System.currentTimeMillis())
                .build();

        // Send request
        String requestJson = objectMapper.writeValueAsString(request);
        session.sendMessage(new TextMessage(requestJson));
        log.info("Sent AI streaming request: {}", requestJson);

        // Wait for completion
        boolean completed = completionLatch.await(60, TimeUnit.SECONDS);
        assertTrue(completed, "AI streaming should complete within 30 seconds");

        // Verify responses
        assertFalse(receivedMessages.isEmpty(), "Should receive at least one message");

        // Verify message sequence
        List<String> messageTypes = receivedMessages.stream()
                .map(AiStreamingResponse::getType)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

        assertTrue(messageTypes.contains("connected"), "Should receive connected message");
        assertTrue(messageTypes.contains("started"), "Should receive started message");
        assertTrue(messageTypes.contains("completed"), "Should receive completed message");

        // Verify chunks were received
        long chunkMessages = receivedMessages.stream()
                .filter(msg -> "chunk".equals(msg.getType()))
                .count();
        assertTrue(chunkMessages > 0, "Should receive at least one chunk message");

        // Verify final response
        AiStreamingResponse completedResponse = receivedMessages.stream()
                .filter(msg -> "completed".equals(msg.getType()))
                .findFirst()
                .orElse(null);

        assertNotNull(completedResponse, "Should have completed response");
        assertNotNull(completedResponse.getFullResponse(), "Should have full response text");
        assertFalse(completedResponse.getFullResponse().isEmpty(), "Full response should not be empty");
        assertNotNull(completedResponse.getProcessingDuration(), "Should have processing duration");
        assertTrue(completedResponse.getProcessingDuration() > 0, "Processing duration should be positive");

        log.info("Received {} total messages", receivedMessages.size());
        log.info("Received {} chunk messages", chunkMessages);
        log.info("Final translation: {}", completedResponse.getFullResponse());
        log.info("Processing duration: {}ms", completedResponse.getProcessingDuration());
    }

    @Test
    void testAiStreamingGrammarExplanation_Successful() throws Exception {
        // Wait for connection
        assertTrue(connectionLatch.await(5, TimeUnit.SECONDS), "Should connect within 5 seconds");

        // Create grammar explanation request
        AiStreamingRequest request = AiStreamingRequest.builder()
                .requestId("test-grammar-" + System.currentTimeMillis())
                .prompt("I%20goes%20to%20school%20everyday") // "I goes to school everyday" - intentional grammar error
                .promptMode(GRAMMAR_EXPLANATION.getMode())
                .targetLanguage(LanguageEnum.EN.getCode())
                .nativeLanguage(LanguageEnum.ZH_CN.getCode())
                .timestamp(System.currentTimeMillis())
                .build();

        // Send request
        String requestJson = objectMapper.writeValueAsString(request);
        session.sendMessage(new TextMessage(requestJson));
        log.info("Sent grammar explanation request: {}", requestJson);

        // Wait for completion
        boolean completed = completionLatch.await(100, TimeUnit.SECONDS);
        assertTrue(completed, "Grammar explanation should complete within 100 seconds");

        // Verify response contains grammar explanation
        AiStreamingResponse completedResponse = receivedMessages.stream()
                .filter(msg -> "completed".equals(msg.getType()))
                .findFirst()
                .orElse(null);

        assertNotNull(completedResponse, "Should have completed response");
        assertNotNull(completedResponse.getFullResponse(), "Should have full response text");

        String explanation = completedResponse.getFullResponse().toLowerCase();
        assertNotEquals("", explanation);

        log.info("Grammar explanation: {}", completedResponse.getFullResponse());
    }

    @Test
    void testAiStreamingValidation_EmptyPrompt() throws Exception {
        // Wait for connection
        assertTrue(connectionLatch.await(5, TimeUnit.SECONDS), "Should connect within 5 seconds");

        // Create invalid request (empty prompt)
        AiStreamingRequest request = AiStreamingRequest.builder()
                .requestId("test-validation-" + System.currentTimeMillis())
                .prompt("") // Empty prompt
                .promptMode(DIRECTLY_TRANSLATION.getMode())
                .targetLanguage(LanguageEnum.ZH_CN.getCode())
                .build();

        // Send request
        String requestJson = objectMapper.writeValueAsString(request);
        session.sendMessage(new TextMessage(requestJson));

        // Wait for error
        boolean errorReceived = errorLatch.await(10, TimeUnit.SECONDS);
        assertTrue(errorReceived, "Should receive error for invalid request");

        // Verify error response
        AiStreamingResponse errorResponse = receivedMessages.stream()
                .filter(msg -> "error".equals(msg.getType()))
                .findFirst()
                .orElse(null);

        assertNotNull(errorResponse, "Should have error response");
        assertEquals("Prompt cannot be empty", errorResponse.getMessage());
        assertEquals("INVALID_PROMPT", errorResponse.getErrorCode());

        log.info("Validation error received: {}", errorResponse.getMessage());
    }

    @Test
    void testAiStreamingValidation_InvalidPromptMode() throws Exception {
        // Wait for connection
        assertTrue(connectionLatch.await(5, TimeUnit.SECONDS), "Should connect within 5 seconds");

        // Create invalid request (invalid prompt mode)
        AiStreamingRequest request = AiStreamingRequest.builder()
                .requestId("test-invalid-mode-" + System.currentTimeMillis())
                .prompt("Test%20message")
                .promptMode("INVALID_MODE") // Invalid mode
                .targetLanguage(LanguageEnum.ZH_CN.getCode())
                .build();

        // Send request
        String requestJson = objectMapper.writeValueAsString(request);
        session.sendMessage(new TextMessage(requestJson));

        // Wait for error
        boolean errorReceived = errorLatch.await(10, TimeUnit.SECONDS);
        assertTrue(errorReceived, "Should receive error for invalid prompt mode");

        // Verify error response
        AiStreamingResponse errorResponse = receivedMessages.stream()
                .filter(msg -> "error".equals(msg.getType()))
                .findFirst()
                .orElse(null);

        assertNotNull(errorResponse, "Should have error response");
        assertTrue(errorResponse.getMessage().contains("Invalid prompt mode"));
        assertEquals("INVALID_PROMPT_MODE", errorResponse.getErrorCode());

        log.info("Invalid prompt mode error received: {}", errorResponse.getMessage());
    }

    @Test
    void testAiStreamingPerformance() throws Exception {
        // Wait for connection
        assertTrue(connectionLatch.await(5, TimeUnit.SECONDS), "Should connect within 5 seconds");

        long startTime = System.currentTimeMillis();

        // Create request
        AiStreamingRequest request = AiStreamingRequest.builder()
                .requestId("test-performance-" + System.currentTimeMillis())
                .prompt("This%20is%20a%20longer%20text%20for%20performance%20testing.%20It%20contains%20multiple%20sentences%20and%20should%20be%20processed%20efficiently.")
                .promptMode(TRANSLATION_AND_EXPLANATION.getMode())
                .targetLanguage(LanguageEnum.ZH_CN.getCode())
                .nativeLanguage(LanguageEnum.EN.getCode())
                .timestamp(startTime)
                .build();

        // Send request
        String requestJson = objectMapper.writeValueAsString(request);
        session.sendMessage(new TextMessage(requestJson));

        // Wait for completion
        boolean completed = completionLatch.await(90, TimeUnit.SECONDS);
        assertTrue(completed, "Performance test should complete within 45 seconds");

        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;

        // Verify performance metrics
        assertTrue(totalTime < 30000, "Should complete within 30 seconds for performance test");
        assertTrue(chunkCount.get() > 0, "Should receive streaming chunks");

        AiStreamingResponse completedResponse = receivedMessages.stream()
                .filter(msg -> "completed".equals(msg.getType()))
                .findFirst()
                .orElse(null);

        assertNotNull(completedResponse, "Should have completed response");
        assertNotNull(completedResponse.getProcessingDuration(), "Should have processing duration");

        log.info("Performance test completed in {}ms", totalTime);
        log.info("Server processing duration: {}ms", completedResponse.getProcessingDuration());
        log.info("Total chunks received: {}", chunkCount.get());
        log.info("Total messages received: {}", receivedMessages.size());
    }
}