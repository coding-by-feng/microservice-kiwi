package me.fengorz.kiwi.ai.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.fengorz.kiwi.ai.api.model.request.YtbSubtitleRequest;
import me.fengorz.kiwi.ai.api.model.response.YtbSubtitleResponse;
import me.fengorz.kiwi.ai.service.ytb.YtbSubtitleStreamingService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.web.socket.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Unit tests for YtbSubtitleWebSocketHandler
 *
 * These tests validate:
 * - Streaming chunk aggregation into fullContent
 * - RequestType handling (scrolling bypasses translation => language null)
 */
public class YtbSubtitleWebSocketHandlerTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private YtbSubtitleStreamingService streamingService;
    private YtbSubtitleWebSocketHandler handler;

    @Before
    public void setUp() {
        streamingService = Mockito.mock(YtbSubtitleStreamingService.class);
        handler = new YtbSubtitleWebSocketHandler(streamingService);
    }

    @Test
    public void testChunkAggregationAndCompletion() throws Exception {
        // Arrange: mock streaming to emit two chunks then complete
        Mockito.doAnswer(invocation -> {
            String videoUrl = invocation.getArgument(0);
            String language = invocation.getArgument(1);
            @SuppressWarnings("unchecked")
            java.util.function.Consumer<String> onChunk = invocation.getArgument(2);
            @SuppressWarnings("unchecked")
            java.util.function.Consumer<Exception> onError = invocation.getArgument(3);
            Runnable onComplete = invocation.getArgument(4);

            // Simulate streaming
            onChunk.accept("Hello ");
            onChunk.accept("World!");
            onComplete.run();
            return null;
        }).when(streamingService).streamSubtitleTranslation(
                Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

        TestSession session = new TestSession("sess-1");
        handler.afterConnectionEstablished(session);

        YtbSubtitleRequest req = new YtbSubtitleRequest()
                .setVideoUrl("https://www.youtube.com/watch?v=dQw4w9WgXcQ")
                .setLanguage("zh-CN")
                .setRequestType("translated");
        String payload = mapper.writeValueAsString(req);

        // Act
        handler.handleTextMessage(session, new TextMessage(payload));

        // Assert: expect STARTED, CHUNK, CHUNK, COMPLETED messages
        List<YtbSubtitleResponse> responses = session.parsed();
        // There is also a CONNECTED message sent on establish
        Assert.assertTrue("Should have at least 4 messages (connected + started + 2 chunks + completed)", responses.size() >= 5);

        // Find last COMPLETED
        YtbSubtitleResponse completed = null;
        for (int i = responses.size() - 1; i >= 0; i--) {
            if (responses.get(i).ifCompleted()) {
                completed = responses.get(i);
                break;
            }
        }
        Assert.assertNotNull("Completed message should exist", completed);
        Assert.assertEquals("Hello World!", completed.getFullContent());
    }

    @Test
    public void testScrollingBypassesTranslation_languageIsNull() throws Exception {
        // Arrange: capture language parameter passed to streaming service
        ArgumentCaptor<String> languageCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.doAnswer(invocation -> {
            // Immediately complete without chunks
            @SuppressWarnings("unchecked")
            java.util.function.Consumer<String> onChunk = invocation.getArgument(2);
            Runnable onComplete = invocation.getArgument(4);
            onComplete.run();
            return null;
        }).when(streamingService).streamSubtitleTranslation(
                Mockito.anyString(), languageCaptor.capture(), Mockito.any(), Mockito.any(), Mockito.any());

        TestSession session = new TestSession("sess-2");
        handler.afterConnectionEstablished(session);

        YtbSubtitleRequest req = new YtbSubtitleRequest()
                .setVideoUrl("https://youtu.be/abcdefghijk")
                .setRequestType("scrolling");
        String payload = mapper.writeValueAsString(req);

        // Act
        handler.handleTextMessage(session, new TextMessage(payload));

        // Assert: language passed to service should be null when requestType is 'scrolling'
        Assert.assertNull("Language should be null for scrolling requests", languageCaptor.getValue());
    }

    // ------------------------------------------
    // Test helper session to capture messages
    // ------------------------------------------
    private class TestSession implements WebSocketSession {
        private final String id;
        private final List<TextMessage> messages = new ArrayList<>();
        private boolean open = true;

        TestSession(String id) { this.id = id; }

        List<YtbSubtitleResponse> parsed() throws IOException {
            List<YtbSubtitleResponse> list = new ArrayList<>();
            for (TextMessage m : messages) {
                list.add(mapper.readValue(m.getPayload(), YtbSubtitleResponse.class));
            }
            return list;
        }

        @Override public String getId() { return id; }
        @Override public URI getUri() { return URI.create("ws://localhost/test"); }

        @Override
        public HttpHeaders getHandshakeHeaders() {
            return null;
        }

        @Override public Map<String, Object> getAttributes() { return null; }
        @Override public Principal getPrincipal() { return null; }
        @Override public InetSocketAddress getLocalAddress() { return new InetSocketAddress(0); }
        @Override public InetSocketAddress getRemoteAddress() { return new InetSocketAddress(0); }
        @Override public String getAcceptedProtocol() { return null; }
        @Override public void setTextMessageSizeLimit(int messageSizeLimit) { }
        @Override public int getTextMessageSizeLimit() { return 64 * 1024; }
        @Override public void setBinaryMessageSizeLimit(int messageSizeLimit) { }
        @Override public int getBinaryMessageSizeLimit() { return 64 * 1024; }
        @Override public List<WebSocketExtension> getExtensions() { return new ArrayList<>(); }
        @Override public void sendMessage(WebSocketMessage<?> message) { messages.add((TextMessage) message); }
        @Override public boolean isOpen() { return open; }
        @Override public void close() { this.open = false; }
        @Override public void close(CloseStatus status) { this.open = false; }
    }
}

