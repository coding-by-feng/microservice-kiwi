package me.fengorz.kason.ai.ws;

import lombok.extern.slf4j.Slf4j;
import me.fengorz.kason.ai.AiApplication;
import me.fengorz.kason.common.sdk.constant.EnvConstants;
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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@ActiveProfiles(EnvConstants.TEST)
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = AiApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class WsIntegrationTest {

    @LocalServerPort
    private int port;

    private CountDownLatch latch;
    private String receivedMessage;
    private WebSocketSession session;

    @BeforeEach
    void setUp() throws Exception {
        latch = new CountDownLatch(1);
        receivedMessage = null;

        // Connect to WebSocket endpoint
        String url = "ws://localhost:" + port + "/ai/ws/stt/audio";
        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();

        session = new StandardWebSocketClient().doHandshake(new TextWebSocketHandler() {
            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) {
                receivedMessage = message.getPayload();
                latch.countDown();
            }
        }, headers, new URI(url)).get(5, TimeUnit.SECONDS);
    }

    @AfterEach
    void tearDown() throws IOException {
        if (session != null && session.isOpen()) {
            session.close(CloseStatus.NORMAL);
        }
    }

    @Test
    void testWebSocketTranscription_Successful() throws Exception {
        // Load sample audio file
        byte[] audioBytes = Files.readAllBytes(Paths.get("src/test/resources/TNH-What-is-mindfulness.mp3"));
        if (audioBytes.length == 0) {
            fail("Sample audio file is empty or not found");
        }

        // Send binary message
        session.sendMessage(new org.springframework.web.socket.BinaryMessage(audioBytes));

        // Wait for response
        boolean received = latch.await(10, TimeUnit.SECONDS);

        // Assert
        assertTrue(received, "Did not receive WebSocket message within timeout");
        assertNotNull(receivedMessage, "No transcription received");
        assertFalse(receivedMessage.isEmpty(), "Transcription should not be empty");
        assertTrue(receivedMessage.toLowerCase().contains("hello"), "Transcription should contain 'hello'");
        System.out.println("Received transcription: " + receivedMessage);
    }
}