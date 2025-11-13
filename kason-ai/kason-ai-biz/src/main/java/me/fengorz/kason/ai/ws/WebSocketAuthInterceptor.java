package me.fengorz.kason.ai.ws;

import lombok.extern.slf4j.Slf4j;
import me.fengorz.kason.bdf.security.google.GoogleTokenCacheService;
import me.fengorz.kason.bdf.security.google.dto.GoogleTokenCacheInfo;
import me.fengorz.kason.common.api.entity.EnhancerUser;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    private final TokenStore tokenStore;
    private final GoogleTokenCacheService googleTokenCacheService;

    public WebSocketAuthInterceptor(TokenStore tokenStore, GoogleTokenCacheService googleTokenCacheService) {
        this.tokenStore = tokenStore;
        this.googleTokenCacheService = googleTokenCacheService;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        log.debug("WebSocket handshake starting for: {}", request.getURI());

        try {
            // Extract token from Authorization header or query parameter
            String token = extractTokenFromRequest(request);
            
            if (token != null) {
                // Validate token and get user information
                Integer userId = validateTokenAndGetUserId(token);
                
                if (userId != null) {
                    // Store user ID in WebSocket session attributes
                    attributes.put("userId", userId);
                    attributes.put("token", token);
                    log.info("WebSocket handshake successful for user: {}", userId);
                    return true;
                } else {
                    log.warn("Invalid token provided for WebSocket connection");
                }
            } else {
                log.warn("No authentication token provided for WebSocket connection");
            }
        } catch (Exception e) {
            log.error("Error during WebSocket authentication: {}", e.getMessage(), e);
        }

        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                              WebSocketHandler wsHandler, Exception exception) {
        if (exception != null) {
            log.error("WebSocket handshake failed", exception);
        } else {
            log.debug("WebSocket handshake completed successfully");
        }
    }

    /**
     * Extract token from Authorization header or query parameter
     */
    private String extractTokenFromRequest(ServerHttpRequest request) {
        // Try Authorization header first
        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        // Try query parameter as fallback
        String query = request.getURI().getQuery();
        if (query != null) {
            Map<String, String> queryParams = parseQueryString(query);
            return queryParams.get("access_token");
        }

        return null;
    }

    /**
     * Parse query string into key-value pairs
     */
    private Map<String, String> parseQueryString(String query) {
        Map<String, String> params = new HashMap<>();
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=", 2);
            if (keyValue.length == 2) {
                try {
                    String key = URLDecoder.decode(keyValue[0], "UTF-8");
                    String value = URLDecoder.decode(keyValue[1], "UTF-8");
                    params.put(key, value);
                } catch (UnsupportedEncodingException e) {
                    log.warn("Error decoding query parameter: {}", pair);
                }
            }
        }
        return params;
    }

    /**
     * Validate token and extract user ID
     */
    private Integer validateTokenAndGetUserId(String token) {
        try {
            // Try to read from token store
            OAuth2Authentication authentication = tokenStore.readAuthentication(token);
            
            if (authentication != null && authentication.getUserAuthentication() != null) {
                Authentication userAuth = authentication.getUserAuthentication();
                
                if (userAuth.getPrincipal() instanceof EnhancerUser) {
                    EnhancerUser user = (EnhancerUser) userAuth.getPrincipal();
                    return user.getId();
                }
            }

            // If not found in token store, could also check Google token cache
            GoogleTokenCacheInfo googleTokenInfo = googleTokenCacheService.getGoogleTokenInfo(token);
            if (googleTokenInfo != null && !googleTokenInfo.ifExpired()) {
                // Extract user ID from Google token info if available
                // This would require modifications to store user ID in GoogleTokenCacheInfo
                log.debug("Found Google token info for token");
            }

        } catch (Exception e) {
            log.error("Error validating token: {}", e.getMessage(), e);
        }
        
        return null;
    }
}
