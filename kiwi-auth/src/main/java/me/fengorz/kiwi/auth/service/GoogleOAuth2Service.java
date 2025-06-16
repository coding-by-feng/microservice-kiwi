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

package me.fengorz.kiwi.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.auth.config.GoogleOAuth2Properties;
import me.fengorz.kiwi.auth.dto.GoogleUserInfo;
import me.fengorz.kiwi.common.api.entity.EnhancerUser;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;

/**
 * Google OAuth2 service for handling authentication
 *
 * @Author Kason Zhan
 * @Date 2025-06-16
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleOAuth2Service {

    private static final String GOOGLE_AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String GOOGLE_TOKEN_URL = "https://www.googleapis.com/oauth2/v4/token";
    private static final String GOOGLE_USER_INFO_URL = "https://www.googleapis.com/oauth2/v2/userinfo";

    private final GoogleOAuth2Properties googleOAuth2Properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final GoogleUserService googleUserService;

    /**
     * Generate Google OAuth2 authorization URL
     *
     * @param state Optional state parameter for CSRF protection
     * @return Authorization URL
     */
    public String getAuthorizationUrl(String state) throws UnsupportedEncodingException {
        log.info("Generating Google OAuth2 authorization URL with state: {}",
                state != null ? "[PROVIDED]" : "[NOT_PROVIDED]");

        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(GOOGLE_AUTH_URL)
                    .queryParam("client_id", googleOAuth2Properties.getClientId())
                    .queryParam("redirect_uri", googleOAuth2Properties.getRedirectUri())
                    .queryParam("scope", String.join(" ", googleOAuth2Properties.getScopes()))
                    .queryParam("response_type", "code")
                    .queryParam("access_type", "offline")
                    .queryParam("prompt", "consent");

            if (state != null && !state.trim().isEmpty()) {
                builder.queryParam("state", URLEncoder.encode(state, String.valueOf(StandardCharsets.UTF_8)));
                log.debug("State parameter added to authorization URL");
            }

            String authUrl = builder.toUriString();
            log.info("Successfully generated Google OAuth2 authorization URL");
            log.debug("Authorization URL parameters - client_id: {}, redirect_uri: {}, scopes: {}",
                    googleOAuth2Properties.getClientId(),
                    googleOAuth2Properties.getRedirectUri(),
                    String.join(", ", googleOAuth2Properties.getScopes()));

            return authUrl;
        } catch (UnsupportedEncodingException e) {
            log.error("Failed to encode state parameter for authorization URL: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error while generating authorization URL: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate authorization URL", e);
        }
    }

    /**
     * Exchange authorization code for access token
     *
     * @param code Authorization code from Google
     * @return Access token response
     */
    public Map<String, Object> exchangeCodeForToken(String code) {
        log.info("Starting token exchange for authorization code");
        log.debug("Authorization code length: {}", code != null ? code.length() : 0);

        if (code == null || code.trim().isEmpty()) {
            log.error("Authorization code is null or empty");
            throw new IllegalArgumentException("Authorization code cannot be null or empty");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", googleOAuth2Properties.getClientId());
        body.add("client_secret", googleOAuth2Properties.getClientSecret());
        body.add("code", code);
        body.add("grant_type", "authorization_code");
        body.add("redirect_uri", googleOAuth2Properties.getRedirectUri());

        log.debug("Token exchange request prepared - client_id: {}, redirect_uri: {}, grant_type: authorization_code",
                googleOAuth2Properties.getClientId(), googleOAuth2Properties.getRedirectUri());

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            log.debug("Sending token exchange request to Google");
            ResponseEntity<String> response = restTemplate.postForEntity(GOOGLE_TOKEN_URL, request, String.class);

            log.info("Received response from Google token endpoint - Status: {}", response.getStatusCode());

            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> tokenResponse = objectMapper.readValue(response.getBody(), Map.class);

                // Log token info without exposing sensitive data
                boolean hasAccessToken = tokenResponse.containsKey("access_token");
                boolean hasRefreshToken = tokenResponse.containsKey("refresh_token");
                Object expiresIn = tokenResponse.get("expires_in");
                Object tokenType = tokenResponse.get("token_type");

                log.info("Token exchange successful - access_token: {}, refresh_token: {}, expires_in: {}, token_type: {}",
                        hasAccessToken ? "PRESENT" : "MISSING",
                        hasRefreshToken ? "PRESENT" : "MISSING",
                        expiresIn, tokenType);

                return tokenResponse;
            } else {
                log.error("Token exchange failed - Status: {}, Body: {}",
                        response.getStatusCode(), response.getBody());
                throw new RuntimeException("Failed to exchange authorization code for token");
            }
        } catch (IOException e) {
            log.error("Error parsing token response from Google: {}", e.getMessage(), e);
            throw new RuntimeException("Error parsing token response", e);
        } catch (Exception e) {
            log.error("Unexpected error during token exchange: {}", e.getMessage(), e);
            throw new RuntimeException("Error exchanging code for token", e);
        }
    }

    /**
     * Get user information from Google using access token
     *
     * @param accessToken Google access token
     * @return Google user information
     */
    public GoogleUserInfo getUserInfo(String accessToken) {
        log.info("Fetching user information from Google");

        if (accessToken == null || accessToken.trim().isEmpty()) {
            log.error("Access token is null or empty");
            throw new IllegalArgumentException("Access token cannot be null or empty");
        }

        log.debug("Access token length: {}", accessToken.length());

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            log.debug("Sending request to Google user info endpoint");
            ResponseEntity<String> response = restTemplate.exchange(
                    GOOGLE_USER_INFO_URL, HttpMethod.GET, entity, String.class);

            log.info("Received response from Google user info endpoint - Status: {}", response.getStatusCode());

            if (response.getStatusCode() == HttpStatus.OK) {
                GoogleUserInfo userInfo = objectMapper.readValue(response.getBody(), GoogleUserInfo.class);

                // Log user info without exposing sensitive data
                log.info("Successfully retrieved user information - id: {}, email: {}, verified_email: {}, name: {}",
                        userInfo.getId() != null ? "[PRESENT]" : "[MISSING]",
                        userInfo.getEmail() != null ? "[PRESENT]" : "[MISSING]",
                        userInfo.getVerifiedEmail(),
                        userInfo.getName() != null ? "[PRESENT]" : "[MISSING]");

                log.debug("User info details - email: {}, name: {}, picture: {}",
                        userInfo.getEmail(), userInfo.getName(),
                        userInfo.getPicture() != null ? "[PRESENT]" : "[MISSING]");

                return userInfo;
            } else {
                log.error("Failed to get user info from Google - Status: {}, Body: {}",
                        response.getStatusCode(), response.getBody());
                throw new RuntimeException("Failed to get user information from Google");
            }
        } catch (IOException e) {
            log.error("Error parsing user info response from Google: {}", e.getMessage(), e);
            throw new RuntimeException("Error parsing user info response", e);
        } catch (Exception e) {
            log.error("Unexpected error while getting user info from Google: {}", e.getMessage(), e);
            throw new RuntimeException("Error getting user info from Google", e);
        }
    }

    /**
     * Convert Google user info to EnhancerUser
     *
     * @param googleUserInfo Google user information
     * @return EnhancerUser for the system
     */
    public EnhancerUser convertToEnhancerUser(GoogleUserInfo googleUserInfo) {
        log.info("Converting Google user info to EnhancerUser");

        if (googleUserInfo == null) {
            log.error("GoogleUserInfo is null, cannot convert to EnhancerUser");
            throw new IllegalArgumentException("GoogleUserInfo cannot be null");
        }

        log.debug("Converting user with email: {}, id: {}",
                googleUserInfo.getEmail(), googleUserInfo.getId());

        try {
            EnhancerUser enhancerUser = googleUserService.findOrCreateUser(googleUserInfo);

            if (enhancerUser != null) {
                log.info("Successfully converted Google user to EnhancerUser - user_id: {}",
                        enhancerUser.getId() != null ? enhancerUser.getId() : "[NEW_USER]");
            } else {
                log.warn("EnhancerUser conversion resulted in null user");
            }

            return enhancerUser;
        } catch (Exception e) {
            log.error("Error converting Google user info to EnhancerUser: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to convert Google user info", e);
        }
    }

    /**
     * Refresh access token using refresh token
     *
     * @param refreshToken Refresh token
     * @return New token response
     */
    public Map<String, Object> refreshAccessToken(String refreshToken) {
        log.info("Starting access token refresh");

        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            log.error("Refresh token is null or empty");
            throw new IllegalArgumentException("Refresh token cannot be null or empty");
        }

        log.debug("Refresh token length: {}", refreshToken.length());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", googleOAuth2Properties.getClientId());
        body.add("client_secret", googleOAuth2Properties.getClientSecret());
        body.add("refresh_token", refreshToken);
        body.add("grant_type", "refresh_token");

        log.debug("Token refresh request prepared - client_id: {}, grant_type: refresh_token",
                googleOAuth2Properties.getClientId());

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            log.debug("Sending token refresh request to Google");
            ResponseEntity<String> response = restTemplate.postForEntity(GOOGLE_TOKEN_URL, request, String.class);

            log.info("Received response from Google token refresh endpoint - Status: {}", response.getStatusCode());

            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> tokenResponse = objectMapper.readValue(response.getBody(), Map.class);

                // Log token info without exposing sensitive data
                boolean hasAccessToken = tokenResponse.containsKey("access_token");
                Object expiresIn = tokenResponse.get("expires_in");
                Object tokenType = tokenResponse.get("token_type");

                log.info("Token refresh successful - access_token: {}, expires_in: {}, token_type: {}",
                        hasAccessToken ? "PRESENT" : "MISSING", expiresIn, tokenType);

                return tokenResponse;
            } else {
                log.error("Token refresh failed - Status: {}, Body: {}",
                        response.getStatusCode(), response.getBody());
                throw new RuntimeException("Failed to refresh access token");
            }
        } catch (IOException e) {
            log.error("Error parsing refresh token response from Google: {}", e.getMessage(), e);
            throw new RuntimeException("Error parsing refresh token response", e);
        } catch (Exception e) {
            log.error("Unexpected error during token refresh: {}", e.getMessage(), e);
            throw new RuntimeException("Error refreshing access token", e);
        }
    }
}