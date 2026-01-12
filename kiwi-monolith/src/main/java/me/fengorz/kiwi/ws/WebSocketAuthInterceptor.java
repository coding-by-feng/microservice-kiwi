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
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.security.KiwiUser;
import me.fengorz.kiwi.security.token.SystemToken;
import me.fengorz.kiwi.security.token.SystemTokenService;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket Authentication Interceptor
 * Validates access tokens during WebSocket handshake
 *
 * @author codingByFeng
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    private final SystemTokenService systemTokenService;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        log.debug("[WS-AUTH] Before handshake - URI: {}", request.getURI());

        // First check if already authenticated via Spring Security
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof KiwiUser) {
            KiwiUser kiwiUser = (KiwiUser) authentication.getPrincipal();
            attributes.put("userId", kiwiUser.getUserId());
            attributes.put("username", kiwiUser.getUsername());
            log.info("[WS-AUTH] User authenticated via SecurityContext: {}", kiwiUser.getUsername());
            return true;
        }

        // Fallback: Extract access token from query parameter
        String accessToken = null;
        if (request instanceof ServletServerHttpRequest) {
            ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
            accessToken = servletRequest.getServletRequest().getParameter("access_token");
        }

        if (accessToken == null || accessToken.isEmpty()) {
            String query = request.getURI().getQuery();
            if (query != null) {
                for (String param : query.split("&")) {
                    String[] keyValue = param.split("=");
                    if (keyValue.length == 2 && "access_token".equals(keyValue[0])) {
                        accessToken = keyValue[1];
                        break;
                    }
                }
            }
        }

        if (accessToken == null || accessToken.isEmpty()) {
            log.warn("[WS-AUTH] No access token provided");
            return false;
        }

        try {
            SystemToken systemToken = systemTokenService.readAccessToken(accessToken);
            if (systemToken == null || systemToken.isExpired()) {
                log.warn("[WS-AUTH] Invalid or expired token");
                return false;
            }

            attributes.put("userId", systemToken.getUserId());
            attributes.put("username", systemToken.getUsername());
            log.info("[WS-AUTH] User authenticated via token: {}", systemToken.getUsername());
            return true;

        } catch (Exception e) {
            log.error("[WS-AUTH] Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        if (exception != null) {
            log.error("[WS-AUTH] Handshake error: {}", exception.getMessage());
        } else {
            log.debug("[WS-AUTH] Handshake completed successfully");
        }
    }
}
