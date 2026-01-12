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
package me.fengorz.kiwi.security.google;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Google Token Validation Service
 *
 * @author codingByFeng
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleTokenValidationService {

    private static final String GOOGLE_TOKEN_INFO_URL = "https://www.googleapis.com/oauth2/v1/tokeninfo";
    private static final String GOOGLE_USER_INFO_URL = "https://www.googleapis.com/oauth2/v2/userinfo";

    private final RestTemplate restTemplate;
    private final GoogleTokenCacheService googleTokenCacheService;

    public boolean validateSystemToken(String systemToken) {
        log.debug("Validating system token");

        try {
            GoogleTokenCacheInfo googleTokenInfo = googleTokenCacheService.getGoogleTokenInfo(systemToken);

            if (googleTokenInfo != null) {
                log.debug("System token has Google token association");
                return validateGoogleTokenInfo(googleTokenInfo);
            } else {
                log.debug("System token has no Google token association");
                return true;
            }
        } catch (Exception e) {
            log.error("Error validating system token", e);
            return false;
        }
    }

    public boolean validateGoogleTokenInfo(GoogleTokenCacheInfo googleTokenInfo) {
        log.debug("Validating Google token info");

        try {
            if (googleTokenInfo.ifExpired()) {
                log.warn("Google token is expired");

                if (googleTokenInfo.getGoogleRefreshToken() != null) {
                    log.debug("Attempting to refresh expired Google token");
                    return refreshGoogleToken(googleTokenInfo);
                } else {
                    log.warn("No refresh token available for expired Google token");
                    return false;
                }
            }

            return validateGoogleTokenWithGoogle(googleTokenInfo.getGoogleAccessToken());
        } catch (Exception e) {
            log.error("Error validating Google token info", e);
            return false;
        }
    }

    public boolean validateGoogleTokenWithGoogle(String googleAccessToken) {
        log.debug("Validating Google access token with Google API");

        try {
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
     * Validate Google access token
     */
    public boolean validateGoogleToken(String googleAccessToken) {
        if (googleAccessToken == null || googleAccessToken.isEmpty()) {
            return false;
        }
        return validateGoogleTokenWithGoogle(googleAccessToken);
    }

    private boolean refreshGoogleToken(GoogleTokenCacheInfo googleTokenInfo) {
        log.debug("Refreshing Google token");

        try {
            googleTokenCacheService.extendGoogleTokenCacheExpiration(googleTokenInfo.getSystemToken(), 3600);
            log.info("Google token refreshed successfully");
            return true;
        } catch (Exception e) {
            log.error("Error refreshing Google token", e);
            return false;
        }
    }

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
                log.warn("Failed to get fresh Google user info");
                return null;
            }
        } catch (Exception e) {
            log.error("Error getting fresh Google user info", e);
            return null;
        }
    }

    public boolean isGoogleSSOToken(String systemToken) {
        try {
            return googleTokenCacheService.isGoogleTokenCached(systemToken);
        } catch (Exception e) {
            log.error("Error checking if token is Google SSO token", e);
            return false;
        }
    }
}
