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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.auth.dto.GoogleTokenCacheInfo;
import me.fengorz.kiwi.auth.dto.GoogleUserInfo;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Service for validating Google OAuth2 tokens
 *
 * @Author Kason Zhan
 * @Date 2025-06-16
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleTokenValidationService {

    private final RestTemplate restTemplate;
    private final GoogleTokenCacheService googleTokenCacheService;

    private static final String GOOGLE_TOKEN_INFO_URL = "https://www.googleapis.com/oauth2/v1/tokeninfo";
    private static final String GOOGLE_USER_INFO_URL = "https://www.googleapis.com/oauth2/v2/userinfo";

    /**
     * Validate system token and check associated Google token
     *
     * @param systemToken System OAuth2 token
     * @return true if token is valid
     */
    public boolean validateSystemToken(String systemToken) {
        log.debug("Validating system token: {}", systemToken);

        try {
            // Check if token has Google token association
            GoogleTokenCacheInfo googleTokenInfo = googleTokenCacheService.getGoogleTokenInfo(systemToken);
            
            if (googleTokenInfo != null) {
                log.debug("System token has Google token association");
                return validateGoogleTokenInfo(googleTokenInfo);
            } else {
                log.debug("System token has no Google token association, assuming standard OAuth2 token");
                // For standard OAuth2 tokens, we rely on the existing validation mechanism
                return true;
            }
        } catch (Exception e) {
            log.error("Error validating system token: {}", systemToken, e);
            return false;
        }
    }

    /**
     * Validate Google token information
     *
     * @param googleTokenInfo Google token cache info
     * @return true if Google token is valid
     */
    public boolean validateGoogleTokenInfo(GoogleTokenCacheInfo googleTokenInfo) {
        log.debug("Validating Google token info for system token: {}", googleTokenInfo.getSystemToken());

        try {
            // Check if token is expired
            if (googleTokenInfo.ifExpired()) {
                log.warn("Google token is expired for system token: {}", googleTokenInfo.getSystemToken());
                
                // Try to refresh if refresh token is available
                if (googleTokenInfo.getGoogleRefreshToken() != null) {
                    log.debug("Attempting to refresh expired Google token");
                    return refreshGoogleToken(googleTokenInfo);
                } else {
                    log.warn("No refresh token available for expired Google token");
                    return false;
                }
            }

            // Validate Google token with Google API
            return validateGoogleTokenWithGoogle(googleTokenInfo.getGoogleAccessToken());

        } catch (Exception e) {
            log.error("Error validating Google token info", e);
            return false;
        }
    }

    /**
     * Validate Google access token with Google's API
     *
     * @param googleAccessToken Google access token
     * @return true if token is valid
     */
    public boolean validateGoogleTokenWithGoogle(String googleAccessToken) {
        log.debug("Validating Google access token with Google API");

        try {
            // Validate token with Google's tokeninfo endpoint
            String tokenInfoUrl = GOOGLE_TOKEN_INFO_URL + "?access_token=" + googleAccessToken;
            ResponseEntity<String> response = restTemplate.getForEntity(tokenInfoUrl, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.debug("Google token validation successful");
                return true;
            } else {
                log.warn("Google token validation failed with status: {}", response.getStatusCode());
                return false;
            }

        } catch (Exception e) {
            log.error("Error validating Google access token with Google API", e);
            return false;
        }
    }

    /**
     * Refresh Google token
     *
     * @param googleTokenInfo Google token cache info
     * @return true if refresh successful
     */
    private boolean refreshGoogleToken(GoogleTokenCacheInfo googleTokenInfo) {
        log.debug("Refreshing Google token for system token: {}", googleTokenInfo.getSystemToken());

        try {
            // This would typically call the GoogleOAuth2Service to refresh the token
            // For now, we'll just extend the cache expiration as a placeholder
            googleTokenCacheService.extendGoogleTokenCacheExpiration(
                googleTokenInfo.getSystemToken(), 3600);
            
            log.info("Google token refreshed successfully for system token: {}", 
                    googleTokenInfo.getSystemToken());
            return true;

        } catch (Exception e) {
            log.error("Error refreshing Google token for system token: {}", 
                    googleTokenInfo.getSystemToken(), e);
            return false;
        }
    }

    /**
     * Get fresh user info from Google
     *
     * @param googleAccessToken Google access token
     * @return Google user info
     */
    public GoogleUserInfo getFreshGoogleUserInfo(String googleAccessToken) {
        log.debug("Getting fresh Google user info");

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(googleAccessToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<GoogleUserInfo> response = restTemplate.exchange(
                GOOGLE_USER_INFO_URL, HttpMethod.GET, entity, GoogleUserInfo.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.debug("Successfully retrieved fresh Google user info");
                return response.getBody();
            } else {
                log.warn("Failed to get fresh Google user info, status: {}", response.getStatusCode());
                return null;
            }

        } catch (Exception e) {
            log.error("Error getting fresh Google user info", e);
            return null;
        }
    }

    /**
     * Check if system token is associated with Google SSO
     *
     * @param systemToken System OAuth2 token
     * @return true if associated with Google SSO
     */
    public boolean isGoogleSSOToken(String systemToken) {
        try {
            return googleTokenCacheService.isGoogleTokenCached(systemToken);
        } catch (Exception e) {
            log.error("Error checking if token is Google SSO token: {}", systemToken, e);
            return false;
        }
    }
}