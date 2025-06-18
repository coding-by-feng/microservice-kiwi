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

package me.fengorz.kiwi.auth.endpoint;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.auth.service.GoogleTokenValidationService;
import me.fengorz.kiwi.bdf.security.google.GoogleTokenCacheService;
import me.fengorz.kiwi.bdf.security.google.dto.GoogleTokenCacheInfo;
import me.fengorz.kiwi.common.api.R;
import me.fengorz.kiwi.common.api.entity.EnhancerUser;
import me.fengorz.kiwi.common.sdk.constant.SecurityConstants;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Enhanced token validation endpoint that supports both OAuth2 and Google tokens
 *
 * @Author Kason Zhan
 * @Date 2025-06-16
 */
@Slf4j
@Primary
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestController
@RequiredArgsConstructor
@RequestMapping("/oauth")
public class GoogleTokenEndpoint {

    private final TokenStore tokenStore;
    private final GoogleTokenCacheService googleTokenCacheService;
    private final GoogleTokenValidationService googleTokenValidationService;

    @DeleteMapping("/logout")
    public R<Boolean> logout(@RequestHeader(value = org.springframework.http.HttpHeaders.AUTHORIZATION, required = false) String authHeader) {
        if (StrUtil.isBlank(authHeader)) {
            return R.failed("退出失败，token 为空");
        }

        String tokenValue = authHeader.replace(OAuth2AccessToken.BEARER_TYPE, StrUtil.EMPTY).trim();
        OAuth2AccessToken accessToken = tokenStore.readAccessToken(tokenValue);
        if (accessToken == null || StrUtil.isBlank(accessToken.getValue())) {
            return R.success();
        }

        // Remove OAuth2 tokens
        tokenStore.removeAccessToken(accessToken);
        Optional.ofNullable(accessToken.getRefreshToken()).ifPresent(tokenStore::removeRefreshToken);

        // Remove Google token cache if exists
        try {
            googleTokenCacheService.removeGoogleTokenCache(tokenValue);
            log.info("Removed Google token cache during logout for token: {}", tokenValue);
        } catch (Exception e) {
            log.warn("Error removing Google token cache during logout: {}", e.getMessage());
        }

        return R.success();
    }


    /**
     * Enhanced check_token endpoint that supports Google SSO tokens
     *
     * @param token Access token to validate
     * @return Token validation result with user info
     */
    @PostMapping("/check_token")
    public R<Map<String, Object>> checkToken(@RequestParam("token") String token) {
        log.info("Checking token: {}", token);

        try {
            // Read OAuth2 access token
            OAuth2AccessToken accessToken = tokenStore.readAccessToken(token);
            if (accessToken == null) {
                log.warn("Token not found in token store: {}", token);
                return R.failed("Invalid token");
            }

            // Check if token is expired
            if (accessToken.isExpired()) {
                log.warn("Token is expired: {}", token);
                return R.failed("Token expired");
            }

            // Read authentication info
            OAuth2Authentication authentication = tokenStore.readAuthentication(token);
            if (authentication == null) {
                log.warn("No authentication found for token: {}", token);
                return R.failed("Invalid token");
            }

            // Build response
            Map<String, Object> response = buildTokenResponse(authentication, token);

            // Check if it's a Google SSO token and add additional info
            GoogleTokenCacheInfo googleTokenInfo = googleTokenCacheService.getGoogleTokenInfo(token);
            if (googleTokenInfo != null) {
                log.debug("Token is associated with Google SSO");
                addGoogleTokenInfo(response, googleTokenInfo);
                
                // Validate Google token
                boolean isGoogleTokenValid = googleTokenValidationService.validateGoogleTokenInfo(googleTokenInfo);
                response.put("google_token_valid", isGoogleTokenValid);
                
                if (!isGoogleTokenValid) {
                    log.warn("Google token validation failed for system token: {}", token);
                    response.put("google_token_status", "invalid");
                }
            }

            log.info("Token validation successful for token: {}", token);
            return R.success(response);

        } catch (Exception e) {
            log.error("Error during token validation for token: {}", token, e);
            return R.failed("Token validation error: " + e.getMessage());
        }
    }

    /**
     * Get token info endpoint
     *
     * @param token Access token
     * @return Token information
     */
    @GetMapping("/token_info")
    public R<Map<String, Object>> getTokenInfo(@RequestParam("token") String token) {
        log.info("Getting token info for: {}", token);

        try {
            OAuth2AccessToken accessToken = tokenStore.readAccessToken(token);
            if (accessToken == null) {
                log.warn("Token not found: {}", token);
                return R.failed("Token not found");
            }

            Map<String, Object> tokenInfo = new HashMap<>();
            tokenInfo.put("active", !accessToken.isExpired());
            tokenInfo.put("token_type", accessToken.getTokenType());
            tokenInfo.put("expires_in", accessToken.getExpiresIn());
            tokenInfo.put("scope", accessToken.getScope());

            // Add Google token info if available
            GoogleTokenCacheInfo googleTokenInfo = googleTokenCacheService.getGoogleTokenInfo(token);
            if (googleTokenInfo != null) {
                tokenInfo.put("auth_method", "google_sso");
                tokenInfo.put("google_user_id", googleTokenInfo.getGoogleUserInfo().getId());
                tokenInfo.put("google_email", googleTokenInfo.getGoogleUserInfo().getEmail());
                tokenInfo.put("google_token_expires_in", googleTokenInfo.getRemainingTimeInSeconds());
                tokenInfo.put("google_token_expired", googleTokenInfo.ifExpired());
            } else {
                tokenInfo.put("auth_method", "standard");
            }

            return R.success(tokenInfo);

        } catch (Exception e) {
            log.error("Error getting token info for token: {}", token, e);
            return R.failed("Error getting token info: " + e.getMessage());
        }
    }

    /**
     * Validate token endpoint (simple validation)
     *
     * @param token Access token
     * @return Validation result
     */
    @PostMapping("/validate_token")
    public R<Boolean> validateToken(@RequestParam("token") String token) {
        log.debug("Validating token: {}", token);

        try {
            // Check OAuth2 token
            OAuth2AccessToken accessToken = tokenStore.readAccessToken(token);
            if (accessToken == null || accessToken.isExpired()) {
                log.debug("Token is invalid or expired: {}", token);
                return R.success(false);
            }

            // Additional validation for Google SSO tokens
            boolean isValid = googleTokenValidationService.validateSystemToken(token);
            
            log.debug("Token validation result for {}: {}", token, isValid);
            return R.success(isValid);

        } catch (Exception e) {
            log.error("Error validating token: {}", token, e);
            return R.success(false);
        }
    }

    /**
     * Build standard token response
     */
    private Map<String, Object> buildTokenResponse(OAuth2Authentication authentication, String token) {
        Map<String, Object> response = new HashMap<>();
        
        // Add OAuth2 request info
        response.put("client_id", authentication.getOAuth2Request().getClientId());
        response.put("scope", authentication.getOAuth2Request().getScope());
        response.put("active", true);

        // Add user info if available
        if (authentication.getUserAuthentication() instanceof UsernamePasswordAuthenticationToken) {
            UsernamePasswordAuthenticationToken userAuth = 
                (UsernamePasswordAuthenticationToken) authentication.getUserAuthentication();
            
            if (userAuth.getPrincipal() instanceof EnhancerUser) {
                EnhancerUser user = (EnhancerUser) userAuth.getPrincipal();
                response.put(SecurityConstants.DETAILS_USER_ID, user.getId());
                response.put(SecurityConstants.DETAILS_USERNAME, user.getUsername());
                response.put(SecurityConstants.DETAILS_DEPT_ID, user.getDeptId());
                response.put("authorities", user.getAuthorities());
            }
        }

        return response;
    }

    /**
     * Add Google token information to response
     */
    private void addGoogleTokenInfo(Map<String, Object> response, GoogleTokenCacheInfo googleTokenInfo) {
        response.put("auth_method", "google_sso");
        response.put("google_user_id", googleTokenInfo.getGoogleUserInfo().getId());
        response.put("google_email", googleTokenInfo.getGoogleUserInfo().getEmail());
        response.put("google_name", googleTokenInfo.getGoogleUserInfo().getName());
        response.put("google_picture", googleTokenInfo.getGoogleUserInfo().getPicture());
        response.put("google_token_expires_in", googleTokenInfo.getRemainingTimeInSeconds());
        response.put("google_token_expired", googleTokenInfo.ifExpired());
        response.put("google_token_expiring_soon", googleTokenInfo.ifExpiringSoon());
    }
}