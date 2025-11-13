/*
 *
 * Copyright [2019~2025] [zhanshifeng]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 *
 */

package me.fengorz.kason.auth.filter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kason.bdf.security.google.GoogleTokenCacheService;
import me.fengorz.kason.bdf.security.google.dto.GoogleTokenCacheInfo;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Debug filter to log token validation attempts and help troubleshoot token issues
 *
 * @Author Kason Zhan
 * @Date 2025-06-16
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DebugTokenFilter implements Filter {

    private final TokenStore tokenStore;
    private final GoogleTokenCacheService googleTokenCacheService;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        String requestURI = httpRequest.getRequestURI();
        String method = httpRequest.getMethod();
        
        // Log request details for debugging
        log.debug("Processing request: {} {}", method, requestURI);
        
        // Extract and analyze token if present
        String token = extractToken(httpRequest);
        if (token != null) {
            analyzeToken(token, requestURI);
        }
        
        // Continue with the filter chain
        chain.doFilter(request, response);
    }

    /**
     * Extract Bearer token from request
     */
    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            log.debug("Extracted Bearer token: {}", token);
            return token;
        }
        
        // Also check for token in query parameters (for some OAuth2 scenarios)
        String tokenParam = request.getParameter("access_token");
        if (StringUtils.hasText(tokenParam)) {
            log.debug("Extracted token from query parameter: {}", tokenParam);
            return tokenParam;
        }
        
        return null;
    }

    /**
     * Analyze token and provide detailed logging
     */
    private void analyzeToken(String token, String requestURI) {
        log.info("=== TOKEN ANALYSIS START ===");
        log.info("Request URI: {}", requestURI);
        log.info("Token: {}", token);
        
        try {
            // Check if token exists in TokenStore
            OAuth2AccessToken accessToken = tokenStore.readAccessToken(token);
            if (accessToken != null) {
                log.info("✓ Token found in TokenStore");
                log.info("  - Token Type: {}", accessToken.getTokenType());
                log.info("  - Expires In: {}", accessToken.getExpiresIn());
                log.info("  - Is Expired: {}", accessToken.isExpired());
                log.info("  - Scope: {}", accessToken.getScope());
                
                if (accessToken.getAdditionalInformation() != null) {
                    log.info("  - Additional Info: {}", accessToken.getAdditionalInformation());
                }
            } else {
                log.warn("✗ Token NOT found in TokenStore");
            }
            
            // Check if it's a Google SSO token
            GoogleTokenCacheInfo googleTokenInfo = googleTokenCacheService.getGoogleTokenInfo(token);
            if (googleTokenInfo != null) {
                log.info("✓ Google SSO token detected");
                log.info("  - Google User ID: {}", googleTokenInfo.getGoogleUserInfo().getId());
                log.info("  - Google Email: {}", googleTokenInfo.getGoogleUserInfo().getEmail());
                log.info("  - Google Name: {}", googleTokenInfo.getGoogleUserInfo().getName());
                log.info("  - Google Token Expired: {}", googleTokenInfo.ifExpired());
                log.info("  - Google Token Expiring Soon: {}", googleTokenInfo.ifExpiringSoon());
                log.info("  - Remaining Time: {} seconds", googleTokenInfo.getRemainingTimeInSeconds());
            } else {
                log.info("- No Google SSO association found");
            }
            
            // Check authentication
            if (accessToken != null) {
                OAuth2Authentication auth = tokenStore.readAuthentication(token);
                if (auth != null) {
                    log.info("✓ Authentication found");
                    log.info("  - Client ID: {}", auth.getOAuth2Request().getClientId());
                    log.info("  - Granted Authorities: {}", auth.getAuthorities());
                    log.info("  - User Principal: {}", auth.getPrincipal());
                } else {
                    log.warn("✗ No authentication found for token");
                }
            }
            
        } catch (Exception e) {
            log.error("Error analyzing token: {}", e.getMessage(), e);
        }
        
        log.info("=== TOKEN ANALYSIS END ===");
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("DebugTokenFilter initialized");
    }

    @Override
    public void destroy() {
        log.info("DebugTokenFilter destroyed");
    }
}