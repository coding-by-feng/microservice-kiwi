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
package me.fengorz.kiwi.api.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.R;
import me.fengorz.kiwi.domain.upms.entity.SysUser;
import me.fengorz.kiwi.security.google.GoogleOAuth2Properties;
import me.fengorz.kiwi.security.google.GoogleOAuth2Service;
import me.fengorz.kiwi.security.google.GoogleTokenCacheInfo;
import me.fengorz.kiwi.security.google.GoogleTokenCacheService;
import me.fengorz.kiwi.security.google.GoogleUserInfo;
import me.fengorz.kiwi.security.token.SystemToken;
import me.fengorz.kiwi.security.token.SystemTokenService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Google OAuth2 Controller
 * Provides endpoints for Google SSO authentication
 *
 * @author codingByFeng
 */
@Slf4j
@RestController
@RequestMapping("/auth/oauth/google")
@RequiredArgsConstructor
@Tag(name = "Google OAuth", description = "Google OAuth2 authentication endpoints")
public class GoogleOAuthController {

    private final GoogleOAuth2Service googleOAuth2Service;
    private final GoogleOAuth2Properties googleOAuth2Properties;
    private final SystemTokenService systemTokenService;
    private final GoogleTokenCacheService googleTokenCacheService;

    /**
     * Get Google OAuth2 authorization URL
     */
    @GetMapping("/authorize")
    @Operation(summary = "Get Google authorization URL")
    public R<Map<String, String>> getAuthorizationUrl(@RequestParam(required = false) String state) {
        try {
            String authUrl = googleOAuth2Service.getAuthorizationUrl(state);
            Map<String, String> result = new HashMap<>();
            result.put("authorizationUrl", authUrl);
            result.put("state", state);
            return R.ok(result);
        } catch (Exception e) {
            log.error("Error generating Google authorization URL", e);
            return R.failed("Failed to generate authorization URL: " + e.getMessage());
        }
    }

    /**
     * Handle Google OAuth2 callback
     */
    @GetMapping("/callback")
    @Operation(summary = "Handle Google OAuth2 callback")
    public void handleCallback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            HttpServletResponse response) throws IOException {

        String homePage = googleOAuth2Properties.getHomePage();
        if (homePage == null) {
            homePage = "/";
        }

        if (StringUtils.hasText(error)) {
            log.error("Google OAuth2 error: {}", error);
            String errorUrl = homePage + "?active=login&error=" + URLEncoder.encode(error, StandardCharsets.UTF_8);
            response.sendRedirect(errorUrl);
            return;
        }

        if (!StringUtils.hasText(code)) {
            String errorUrl = homePage + "?active=login&error=missing_code";
            response.sendRedirect(errorUrl);
            return;
        }

        try {
            // Exchange code for Google access token
            Map<String, Object> tokenResponse = googleOAuth2Service.exchangeCodeForToken(code);
            String googleAccessToken = (String) tokenResponse.get("access_token");

            if (!StringUtils.hasText(googleAccessToken)) {
                String errorUrl = homePage + "?active=login&error=token_failed";
                response.sendRedirect(errorUrl);
                return;
            }

            // Get user info from Google
            GoogleUserInfo googleUserInfo = googleOAuth2Service.getUserInfo(googleAccessToken);
            log.info("Google user info retrieved: {}", googleUserInfo.getEmail());

            // Find or create user
            SysUser sysUser = googleOAuth2Service.findOrCreateUser(googleUserInfo);

            // Generate system token
            SystemToken systemToken = systemTokenService.generateGoogleToken(sysUser, googleUserInfo);

            // Cache Google token info
            cacheGoogleTokenInfo(systemToken.getAccessToken(), tokenResponse, googleUserInfo);

            // Redirect to frontend with token
            String successUrl = String.format(
                    "%s?active=search&token=%s&user=%s",
                    homePage,
                    systemToken.getAccessToken(),
                    URLEncoder.encode(googleUserInfo.getName(), StandardCharsets.UTF_8)
            );
            response.sendRedirect(successUrl);

        } catch (Exception e) {
            log.error("Error processing Google OAuth2 callback", e);
            String errorUrl = homePage + "?active=login&error=auth_failed";
            response.sendRedirect(errorUrl);
        }
    }

    /**
     * Login directly with Google access token
     */
    @PostMapping("/login")
    @Operation(summary = "Login with Google access token")
    public R<Map<String, Object>> loginWithGoogleToken(@RequestBody TokenRequest request) {
        String googleAccessToken = request.getAccessToken();

        if (!StringUtils.hasText(googleAccessToken)) {
            return R.failed("Google access token is required");
        }

        try {
            // Get user info from Google
            GoogleUserInfo googleUserInfo = googleOAuth2Service.getUserInfo(googleAccessToken);
            log.info("Google user info retrieved for direct login: {}", googleUserInfo.getEmail());

            // Find or create user
            SysUser sysUser = googleOAuth2Service.findOrCreateUser(googleUserInfo);

            // Generate system token
            SystemToken systemToken = systemTokenService.generateGoogleToken(sysUser, googleUserInfo);

            // Cache Google token info
            Map<String, Object> googleTokenResponse = new HashMap<>();
            googleTokenResponse.put("access_token", googleAccessToken);
            googleTokenResponse.put("token_type", "Bearer");
            googleTokenResponse.put("expires_in", 3600);
            cacheGoogleTokenInfo(systemToken.getAccessToken(), googleTokenResponse, googleUserInfo);

            // Build response
            Map<String, Object> response = systemTokenService.buildTokenResponse(systemToken);
            Map<String, Object> userInfoMap = new HashMap<>();
            userInfoMap.put("id", googleUserInfo.getId());
            userInfoMap.put("email", googleUserInfo.getEmail());
            userInfoMap.put("name", googleUserInfo.getName());
            userInfoMap.put("picture", googleUserInfo.getPicture());
            response.put("userInfo", userInfoMap);

            return R.ok(response);

        } catch (Exception e) {
            log.error("Error processing Google token login", e);
            return R.failed("Authentication failed: " + e.getMessage());
        }
    }

    /**
     * Refresh Google access token
     */
    @PostMapping("/refresh")
    @Operation(summary = "Refresh Google access token")
    public R<Map<String, Object>> refreshToken(@RequestBody RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        if (!StringUtils.hasText(refreshToken)) {
            return R.failed("Refresh token is required");
        }

        try {
            Map<String, Object> tokenResponse = googleOAuth2Service.refreshAccessToken(refreshToken);
            return R.ok(tokenResponse);
        } catch (Exception e) {
            log.error("Error refreshing Google token", e);
            return R.failed("Failed to refresh token: " + e.getMessage());
        }
    }

    /**
     * Logout and invalidate tokens
     */
    @PostMapping("/logout")
    @Operation(summary = "Logout and invalidate Google tokens")
    public R<Boolean> logout(@RequestParam String systemToken) {
        try {
            // Remove system token
            systemTokenService.removeAccessToken(systemToken);

            // Remove Google token cache
            googleTokenCacheService.removeGoogleTokenInfo(systemToken);

            return R.ok(true);
        } catch (Exception e) {
            log.error("Error during logout", e);
            return R.failed("Logout failed: " + e.getMessage());
        }
    }

    private void cacheGoogleTokenInfo(String systemToken, Map<String, Object> googleTokenResponse, GoogleUserInfo googleUserInfo) {
        String googleAccessToken = (String) googleTokenResponse.get("access_token");
        String googleRefreshToken = (String) googleTokenResponse.get("refresh_token");
        Integer expiresIn = (Integer) googleTokenResponse.get("expires_in");

        GoogleTokenCacheInfo cacheInfo = GoogleTokenCacheInfo.builder()
                .googleAccessToken(googleAccessToken)
                .googleRefreshToken(googleRefreshToken)
                .googleUserInfo(googleUserInfo)
                .expiresAt(LocalDateTime.now().plusSeconds(expiresIn != null ? expiresIn : 3600))
                .build();

        googleTokenCacheService.cacheGoogleTokenInfo(systemToken, cacheInfo);
    }

    @Data
    public static class TokenRequest {
        private String accessToken;
    }

    @Data
    public static class RefreshTokenRequest {
        private String refreshToken;
    }
}
