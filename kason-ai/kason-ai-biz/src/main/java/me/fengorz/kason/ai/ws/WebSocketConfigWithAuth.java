package me.fengorz.kason.ai.ws;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfigWithAuth implements WebSocketConfigurer {

    private final AudioWebSocketHandler audioWebSocketHandler;
    private final AiStreamingWebSocketHandler aiStreamingWebSocketHandler;
    private final YtbSubtitleWebSocketHandler ytbSubtitleWebSocketHandler;
    private final WebSocketAuthInterceptor webSocketAuthInterceptor;

    public WebSocketConfigWithAuth(AudioWebSocketHandler audioWebSocketHandler,
                                   AiStreamingWebSocketHandler aiStreamingWebSocketHandler,
                                   YtbSubtitleWebSocketHandler ytbSubtitleWebSocketHandler,
                                   WebSocketAuthInterceptor webSocketAuthInterceptor) {
        this.audioWebSocketHandler = audioWebSocketHandler;
        this.aiStreamingWebSocketHandler = aiStreamingWebSocketHandler;
        this.ytbSubtitleWebSocketHandler = ytbSubtitleWebSocketHandler;
        this.webSocketAuthInterceptor = webSocketAuthInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // Audio WebSocket handler
        registry.addHandler(audioWebSocketHandler, "/ai/ws/stt/audio")
                .setAllowedOrigins("*")
                .addInterceptors(webSocketAuthInterceptor);

        // AI streaming WebSocket handler
        registry.addHandler(aiStreamingWebSocketHandler, "/ai/ws/stream")
                .setAllowedOrigins("*")
                .addInterceptors(webSocketAuthInterceptor);

        // YouTube subtitle WebSocket handler
        registry.addHandler(ytbSubtitleWebSocketHandler, "/ai/ws/ytb/subtitle")
                .setAllowedOrigins("*")
                .addInterceptors(webSocketAuthInterceptor);
    }
}