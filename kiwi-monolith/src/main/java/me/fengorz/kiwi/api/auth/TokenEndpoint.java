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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.R;
import me.fengorz.kiwi.security.google.GoogleTokenCacheInfo;
import me.fengorz.kiwi.security.google.GoogleTokenCacheService;
import me.fengorz.kiwi.security.google.GoogleTokenValidationService;
import me.fengorz.kiwi.security.token.SystemToken;
import me.fengorz.kiwi.security.token.SystemTokenService;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Token Endpoint
 * Provides token validation and introspection endpoints
 *
 * @author codingByFeng
 */
@Slf4j
@RestController
@RequestMapping("/oauth")
@RequiredArgsConstructor
@Tag(name = "Token", description = "Token validation and management endpoints")
public class TokenEndpoint {

    private final SystemTokenService systemTokenService;
    private final GoogleTokenCacheService googleTokenCacheService;
    private final GoogleTokenValidationService googleTokenValidationService;

    /**
     * Logout endpoint
     */
    @DeleteMapping("/logout")
    @Operation(summary = "Logout and invalidate token")
    public R<Boolean> logout(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader) {
        if (!StringUtils.hasText(authHeader)) {
            return R.failed("Token is required");
        }

        String tokenValue = authHeader.replace("Bearer ", "").trim();

        try {
            // Remove system token
            systemTokenService.removeAccessToken(tokenValue);

            // Remove Google token cache if exists
            try {
                googleTokenCacheService.removeGoogleTokenInfo(tokenValue);
            } catch (Exception e) {
                log.warn("Error removing Google token cache during logout: {}", e.getMessage());
            }

            return R.ok(true);
        } catch (Exception e) {
            log.error("Error during logout", e);
            return R.failed("Logout failed: " + e.getMessage());
        }
    }

    /**
     * Check token endpoint - detailed token validation
     */
    @PostMapping("/check_token")
    @Operation(summary = "Check and validate token")
    public R<Map<String, Object>> checkToken(@RequestParam("token") String token) {
        log.debug("Checking token: {}", token);

        try {
            SystemToken systemToken = systemTokenService.readAccessToken(token);
            if (systemToken == null) {
                log.warn("Token not found: {}", token);
                return R.failed("Invalid token");
            }

            if (systemToken.isExpired()) {
                log.warn("Token is expired: {}", token);
                return R.failed("Token expired");
            }

            // Build response
            Map<String, Object> response = systemTokenService.buildTokenInfo(systemToken);

            // Check if it's a Google SSO token and add additional info
            if ("google_sso".equals(systemToken.getAuthMethod())) {
                GoogleTokenCacheInfo googleTokenInfo = googleTokenCacheService.getGoogleTokenInfo(token);
                if (googleTokenInfo != null) {
                    addGoogleTokenInfo(response, googleTokenInfo);

                    // Validate Google token
                    boolean isGoogleTokenValid = googleTokenValidationService.validateGoogleToken(
                            googleTokenInfo.getGoogleAccessToken());
                    response.put("google_token_valid", isGoogleTokenValid);

                    if (!isGoogleTokenValid) {
                        log.warn("Google token validation failed for system token: {}", token);
                        response.put("google_token_status", "invalid");
                    }
                }
            }

            log.debug("Token validation successful for token: {}", token);
            return R.ok(response);

        } catch (Exception e) {
            log.error("Error during token validation for token: {}", token, e);
            return R.failed("Token validation error: " + e.getMessage());
        }
    }

    /**
     * Get token info endpoint
     */
    @GetMapping("/token_info")
    @Operation(summary = "Get token information")
    public R<Map<String, Object>> getTokenInfo(@RequestParam("token") String token) {
        log.debug("Getting token info for: {}", token);

        try {
            SystemToken systemToken = systemTokenService.readAccessToken(token);
            if (systemToken == null) {
                log.warn("Token not found: {}", token);
                return R.failed("Token not found");
            }

            Map<String, Object> tokenInfo = new HashMap<>();
            tokenInfo.put("active", !systemToken.isExpired());
            tokenInfo.put("token_type", systemToken.getTokenType());
            tokenInfo.put("expires_in", systemToken.getExpiresIn());
            tokenInfo.put("scope", systemToken.getScope());
            tokenInfo.put("auth_method", systemToken.getAuthMethod());

            // Add Google token info if available
            if ("google_sso".equals(systemToken.getAuthMethod())) {
                GoogleTokenCacheInfo googleTokenInfo = googleTokenCacheService.getGoogleTokenInfo(token);
                if (googleTokenInfo != null && googleTokenInfo.getGoogleUserInfo() != null) {
                    tokenInfo.put("google_user_id", googleTokenInfo.getGoogleUserInfo().getId());
                    tokenInfo.put("google_email", googleTokenInfo.getGoogleUserInfo().getEmail());
                    tokenInfo.put("google_token_expires_in", googleTokenInfo.getRemainingTimeInSeconds());
                    tokenInfo.put("google_token_expired", googleTokenInfo.isExpired());
                }
            }

            return R.ok(tokenInfo);

        } catch (Exception e) {
            log.error("Error getting token info for token: {}", token, e);
            return R.failed("Error getting token info: " + e.getMessage());
        }
    }

    /**
     * Validate token endpoint (simple validation)
     */
    @PostMapping("/validate_token")
    @Operation(summary = "Validate token")
    public R<Boolean> validateToken(@RequestParam("token") String token) {
        log.debug("Validating token: {}", token);

        try {
            boolean isValid = systemTokenService.isValidToken(token);

            // Additional validation for Google SSO tokens
            if (isValid) {
                SystemToken systemToken = systemTokenService.readAccessToken(token);
                if (systemToken != null && "google_sso".equals(systemToken.getAuthMethod())) {
                    GoogleTokenCacheInfo googleTokenInfo = googleTokenCacheService.getGoogleTokenInfo(token);
                    if (googleTokenInfo != null) {
                        isValid = googleTokenValidationService.validateGoogleToken(
                                googleTokenInfo.getGoogleAccessToken());
                    }
                }
            }

            log.debug("Token validation result for {}: {}", token, isValid);
            return R.ok(isValid);

        } catch (Exception e) {
            log.error("Error validating token: {}", token, e);
            return R.ok(false);
        }
    }

    /**
     * Add Google token information to response
     */
    private void addGoogleTokenInfo(Map<String, Object> response, GoogleTokenCacheInfo googleTokenInfo) {
        if (googleTokenInfo.getGoogleUserInfo() != null) {
            response.put("google_user_id", googleTokenInfo.getGoogleUserInfo().getId());
            response.put("google_email", googleTokenInfo.getGoogleUserInfo().getEmail());
            response.put("google_name", googleTokenInfo.getGoogleUserInfo().getName());
            response.put("google_picture", googleTokenInfo.getGoogleUserInfo().getPicture());
        }
        response.put("google_token_expires_in", googleTokenInfo.getRemainingTimeInSeconds());
        response.put("google_token_expired", googleTokenInfo.isExpired());
        response.put("google_token_expiring_soon", googleTokenInfo.isExpiringSoon());
    }
}
