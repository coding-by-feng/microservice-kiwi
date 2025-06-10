package me.fengorz.kiwi.ai.ws;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final AudioWebSocketHandler audioWebSocketHandler;
    private final AiStreamingWebSocketHandler aiStreamingWebSocketHandler;


    public WebSocketConfig(AudioWebSocketHandler audioWebSocketHandler,
                           AiStreamingWebSocketHandler aiStreamingWebSocketHandler) {
        this.audioWebSocketHandler = audioWebSocketHandler;
        this.aiStreamingWebSocketHandler = aiStreamingWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(audioWebSocketHandler, "/ai/ws/stt/audio")
                .setAllowedOrigins("*");

        // New AI streaming WebSocket handler
        registry.addHandler(aiStreamingWebSocketHandler, "/ai/ws/stream")
                .setAllowedOrigins("*");
    }
}