package me.fengorz.kiwi.ai.ws;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfigWithAuth implements WebSocketConfigurer {

    private final AudioWebSocketHandler audioWebSocketHandler;
    private final AiStreamingWebSocketHandler aiStreamingWebSocketHandler;
    private final WebSocketAuthInterceptor webSocketAuthInterceptor;

    public WebSocketConfigWithAuth(AudioWebSocketHandler audioWebSocketHandler,
                                   AiStreamingWebSocketHandler aiStreamingWebSocketHandler,
                                   WebSocketAuthInterceptor webSocketAuthInterceptor) {
        this.audioWebSocketHandler = audioWebSocketHandler;
        this.aiStreamingWebSocketHandler = aiStreamingWebSocketHandler;
        this.webSocketAuthInterceptor = webSocketAuthInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(audioWebSocketHandler, "/ai/ws/stt/audio")
                .setAllowedOrigins("*")
                .addInterceptors(webSocketAuthInterceptor);

        // AI streaming WebSocket handler with authentication
        registry.addHandler(aiStreamingWebSocketHandler, "/ai/ws/stream")
                .setAllowedOrigins("*")
                .addInterceptors(webSocketAuthInterceptor);
    }
}