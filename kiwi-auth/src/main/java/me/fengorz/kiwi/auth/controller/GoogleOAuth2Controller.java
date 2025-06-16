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

package me.fengorz.kiwi.auth.controller;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.auth.config.GoogleOAuth2Properties;
import me.fengorz.kiwi.auth.dto.GoogleOAuthRequest;
import me.fengorz.kiwi.auth.dto.GoogleOAuthResponse;
import me.fengorz.kiwi.auth.dto.GoogleUserInfo;
import me.fengorz.kiwi.auth.service.GoogleOAuth2Service;
import me.fengorz.kiwi.common.api.R;
import me.fengorz.kiwi.common.api.entity.EnhancerUser;
import me.fengorz.kiwi.common.sdk.constant.SecurityConstants;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.*;

/**
 * Google OAuth2 authentication controller
 *
 * @Author Kason Zhan
 * @Date 2025-06-16
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/oauth/google")
public class GoogleOAuth2Controller {

    private final GoogleOAuth2Service googleOAuth2Service;
    private final TokenStore tokenStore;
    private final GoogleOAuth2Properties googleOAuth2Properties;

    /**
     * Get Google OAuth2 authorization URL
     *
     * @param state Optional state parameter for CSRF protection
     * @return Authorization URL wrapped in R response
     */
    @GetMapping("/authorize")
    public R<Map<String, String>> getAuthorizationUrl(@RequestParam(required = false) String state) {
        try {
            String authUrl = googleOAuth2Service.getAuthorizationUrl(state);
            Map<String, String> result = new HashMap<>();
            result.put("authorizationUrl", authUrl);
            result.put("state", state);

            return R.success(result, "Authorization URL generated successfully");
        } catch (Exception e) {
            log.error("Error generating Google authorization URL", e);
            return R.failed("Failed to generate authorization URL: " + e.getMessage());
        }
    }

    /**
     * Handle Google OAuth2 callback and generate system tokens
     *
     * @param code     Authorization code from Google
     * @param state    State parameter for CSRF protection
     * @param error    Error parameter if authorization failed
     * @param response HTTP response for potential redirects
     * @return Authentication result with system tokens
     */
    @GetMapping("/callback")
    public void handleCallback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            HttpServletResponse response) throws IOException {

        if (StrUtil.isNotBlank(error)) {
            log.error("Google OAuth2 error: {}", error);
            // Redirect to frontend with error using homePage
            String errorUrl = googleOAuth2Properties.getHomePage() + "?active=login&error=" + URLEncoder.encode(error, "UTF-8");
            response.sendRedirect(errorUrl);
            return;
        }

        if (StrUtil.isBlank(code)) {
            String errorUrl = googleOAuth2Properties.getHomePage() + "?active=login&error=missing_code";
            response.sendRedirect(errorUrl);
            return;
        }

        try {
            // Exchange code for Google access token
            Map<String, Object> tokenResponse = googleOAuth2Service.exchangeCodeForToken(code);
            String googleAccessToken = (String) tokenResponse.get("access_token");

            if (StrUtil.isBlank(googleAccessToken)) {
                String errorUrl = googleOAuth2Properties.getHomePage() + "?active=login&error=token_failed";
                response.sendRedirect(errorUrl);
                return;
            }

            // Get user info from Google
            GoogleUserInfo googleUserInfo = googleOAuth2Service.getUserInfo(googleAccessToken);
            log.info("Google user info retrieved: {}", googleUserInfo);

            // Create your app's access token for this user
            GoogleOAuthResponse authResponse = buildGoogleOAuthResponse(googleUserInfo);

            // Redirect to frontend using homePage with the token
            String successUrl = String.format(
                    "%s?active=search&token=%s&user=%s",
                    googleOAuth2Properties.getHomePage(),
                    authResponse.getAccessToken(),
                    URLEncoder.encode(googleUserInfo.getName(), "UTF-8")
            );

            response.sendRedirect(successUrl);

        } catch (Exception e) {
            log.error("Error processing Google OAuth2 callback", e);
            String errorUrl = googleOAuth2Properties.getHomePage() + "?active=login&error=auth_failed";
            response.sendRedirect(errorUrl);
        }
    }

    /**
     * Login directly with Google access token
     *
     * @param request Request containing Google access token
     * @return Authentication result with system tokens
     */
    @PostMapping("/login")
    public R<GoogleOAuthResponse> loginWithGoogleToken(@RequestBody GoogleOAuthRequest.TokenRequest request) {
        String googleAccessToken = request.getAccessToken();

        if (StrUtil.isBlank(googleAccessToken)) {
            return R.failed("Google access token is required");
        }

        try {
            // Get user info from Google
            GoogleUserInfo googleUserInfo = googleOAuth2Service.getUserInfo(googleAccessToken);
            log.info("Google user info retrieved for direct login: {}", googleUserInfo);

            return R.success(buildGoogleOAuthResponse(googleUserInfo), "Google authentication successful");

        } catch (Exception e) {
            log.error("Error processing Google token login", e);
            return R.failed("Authentication failed: " + e.getMessage());
        }
    }

    @NotNull
    private GoogleOAuthResponse buildGoogleOAuthResponse(GoogleUserInfo googleUserInfo) {
        EnhancerUser enhancerUser = googleOAuth2Service.convertToEnhancerUser(googleUserInfo);

        // Generate system OAuth2 token
        OAuth2AccessToken systemToken = generateSystemToken(enhancerUser, googleUserInfo);

        // Create user info map - Java 8 compatible
        Map<String, Object> userInfoMap = new HashMap<>();
        userInfoMap.put("id", googleUserInfo.getId());
        userInfoMap.put("email", googleUserInfo.getEmail());
        userInfoMap.put("name", googleUserInfo.getName());
        userInfoMap.put("picture", googleUserInfo.getPicture());

        return GoogleOAuthResponse.builder()
                .accessToken(systemToken.getValue())
                .tokenType(systemToken.getTokenType())
                .expiresIn(systemToken.getExpiresIn())
                .scope(systemToken.getScope())
                .refreshToken(systemToken.getRefreshToken() != null ? systemToken.getRefreshToken().getValue() : null)
                .userInfo(userInfoMap)
                .build();
    }

    /**
     * Refresh Google access token
     *
     * @param request Request containing refresh token
     * @return New token response
     */
    @PostMapping("/refresh")
    public R<Map<String, Object>> refreshToken(@RequestBody GoogleOAuthRequest.RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        if (StrUtil.isBlank(refreshToken)) {
            return R.failed("Refresh token is required");
        }

        try {
            Map<String, Object> tokenResponse = googleOAuth2Service.refreshAccessToken(refreshToken);
            return R.success(tokenResponse, "Token refreshed successfully");
        } catch (Exception e) {
            log.error("Error refreshing Google token", e);
            return R.failed("Failed to refresh token: " + e.getMessage());
        }
    }

    /**
     * Generate system OAuth2 token for authenticated user
     *
     * @param enhancerUser   System user
     * @param googleUserInfo Google user information
     * @return System OAuth2 access token
     */
    private OAuth2AccessToken generateSystemToken(EnhancerUser enhancerUser, GoogleUserInfo googleUserInfo) {
        // Create authentication with EnhancerUser
        UsernamePasswordAuthenticationToken userAuth =
                new UsernamePasswordAuthenticationToken(enhancerUser, null, enhancerUser.getAuthorities());

        // Create OAuth2 request
        Map<String, String> requestParameters = new HashMap<>();
        requestParameters.put("grant_type", "google_sso");
        requestParameters.put("client_id", "google-sso-client");

        // Create scope set - Java 8 compatible
        Set<String> scopes = new HashSet<>();
        scopes.add("read");
        scopes.add("write");
        scopes.add("profile");
        scopes.add("email");

        OAuth2Request oAuth2Request = new OAuth2Request(
                requestParameters,
                "google-sso-client",
                enhancerUser.getAuthorities(),
                true,
                scopes,  // Java 8 compatible HashSet
                Collections.emptySet(),
                null,
                Collections.emptySet(),
                Collections.emptyMap()
        );

        OAuth2Authentication oAuth2Authentication = new OAuth2Authentication(oAuth2Request, userAuth);

        // Generate token
        DefaultOAuth2AccessToken accessToken = new DefaultOAuth2AccessToken(UUID.randomUUID().toString());
        accessToken.setExpiration(new Date(System.currentTimeMillis() + 30 * 24 * 60 * 60 * 1000L)); // 30 days
        accessToken.setScope(scopes);  // Java 8 compatible HashSet
        accessToken.setTokenType("Bearer");

        // Add additional information using EnhancerUser properties
        Map<String, Object> additionalInfo = new HashMap<>();
        additionalInfo.put(SecurityConstants.DETAILS_LICENSE, SecurityConstants.PROJECT_LICENSE);
        additionalInfo.put(SecurityConstants.DETAILS_USER_ID, enhancerUser.getId());
        additionalInfo.put(SecurityConstants.DETAILS_USERNAME, enhancerUser.getUsername());
        additionalInfo.put(SecurityConstants.DETAILS_DEPT_ID, enhancerUser.getDeptId());
        additionalInfo.put("google_user_id", googleUserInfo.getId());
        additionalInfo.put("auth_method", "google_sso");
        additionalInfo.put("is_admin", enhancerUser.getIsAdmin());
        accessToken.setAdditionalInformation(additionalInfo);

        // Store token
        tokenStore.storeAccessToken(accessToken, oAuth2Authentication);

        return accessToken;
    }
}