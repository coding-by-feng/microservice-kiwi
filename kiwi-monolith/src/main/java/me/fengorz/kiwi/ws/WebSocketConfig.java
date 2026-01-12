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

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket Configuration
 *
 * @deprecated These WebSocket endpoints are deprecated. Please migrate to SSE/REST endpoints:
 * <ul>
 *     <li>/api/ai/ws/stream -> /api/ai/sse/stream (GET or POST)</li>
 *     <li>/api/ai/ws/ytb/subtitle -> /api/ai/sse/ytb/subtitle (GET or POST)</li>
 *     <li>/api/ai/ws/stt/audio -> /api/ai/audio/speech-to-text (POST with multipart/form-data)</li>
 * </ul>
 * SSE provides better compatibility with proxies, load balancers, and is simpler to implement on the client side.
 *
 * @author codingByFeng
 */
@Deprecated
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final AiStreamingWebSocketHandler aiStreamingWebSocketHandler;
    private final YtbSubtitleWebSocketHandler ytbSubtitleWebSocketHandler;
    private final AudioWebSocketHandler audioWebSocketHandler;
    private final WebSocketAuthInterceptor webSocketAuthInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // AI streaming WebSocket handler
        registry.addHandler(aiStreamingWebSocketHandler, "/api/ai/ws/stream")
                .setAllowedOrigins("*")
                .addInterceptors(webSocketAuthInterceptor);

        // YouTube subtitle WebSocket handler
        registry.addHandler(ytbSubtitleWebSocketHandler, "/api/ai/ws/ytb/subtitle")
                .setAllowedOrigins("*")
                .addInterceptors(webSocketAuthInterceptor);

        // Audio WebSocket handler
        registry.addHandler(audioWebSocketHandler, "/api/ai/ws/stt/audio")
                .setAllowedOrigins("*")
                .addInterceptors(webSocketAuthInterceptor);
    }
}
