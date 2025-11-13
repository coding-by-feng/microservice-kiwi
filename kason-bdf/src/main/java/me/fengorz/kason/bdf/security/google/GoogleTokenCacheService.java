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

package me.fengorz.kason.bdf.security.google;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kason.bdf.security.google.dto.GoogleTokenCacheInfo;
import me.fengorz.kason.bdf.security.google.dto.GoogleUserInfo;
import me.fengorz.kason.common.sdk.constant.SecurityConstants;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Service for managing Google OAuth2 token cache in Redis
 *
 * @Author Kason Zhan
 * @Date 2025-06-16
 */
@Slf4j
@Service
public class GoogleTokenCacheService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    // Redis key prefixes
    private static final String GOOGLE_TOKEN_PREFIX = SecurityConstants.PROJECT_PREFIX + "google:token:";
    private static final String GOOGLE_REFRESH_PREFIX = SecurityConstants.PROJECT_PREFIX + "google:refresh:";
    private static final String SYSTEM_TOKEN_MAPPING_PREFIX = SecurityConstants.PROJECT_PREFIX + "google:mapping:";

    public GoogleTokenCacheService(@Qualifier("googleOAuth2RedisTemplate") RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Cache Google token information in Redis
     *
     * @param systemToken        System OAuth2 token
     * @param googleAccessToken  Google access token
     * @param googleRefreshToken Google refresh token (can be null)
     * @param expiresIn         Token expiration in seconds
     * @param googleUserInfo    Google user information
     */
    public void cacheGoogleTokenInfo(String systemToken, String googleAccessToken, 
                                   String googleRefreshToken, Integer expiresIn, 
                                   GoogleUserInfo googleUserInfo) {
        try {
            // Create cache info object
            GoogleTokenCacheInfo cacheInfo = GoogleTokenCacheInfo.builder()
                    .systemToken(systemToken)
                    .googleAccessToken(googleAccessToken)
                    .googleRefreshToken(googleRefreshToken)
                    .expiresIn(expiresIn)
                    .googleUserInfo(googleUserInfo)
                    .cacheTime(System.currentTimeMillis())
                    .build();

            String cacheInfoJson = objectMapper.writeValueAsString(cacheInfo);

            // Cache with system token as key
            String googleTokenKey = GOOGLE_TOKEN_PREFIX + systemToken;
            redisTemplate.opsForValue().set(googleTokenKey, cacheInfoJson, Duration.ofSeconds(expiresIn + 300)); // Add 5 minutes buffer

            // Create mapping from refresh token to system token (if refresh token exists)
            if (googleRefreshToken != null) {
                String refreshMappingKey = GOOGLE_REFRESH_PREFIX + googleRefreshToken;
                redisTemplate.opsForValue().set(refreshMappingKey, systemToken, Duration.ofDays(30)); // Longer expiration for refresh token mapping
            }

            // Create reverse mapping from system token to google token info
            String systemMappingKey = SYSTEM_TOKEN_MAPPING_PREFIX + systemToken;
            redisTemplate.opsForValue().set(systemMappingKey, cacheInfoJson, Duration.ofSeconds(expiresIn + 300));

            log.info("Cached Google token info for system token: {}", systemToken);

        } catch (JsonProcessingException e) {
            log.error("Error caching Google token info for system token: {}", systemToken, e);
            throw new RuntimeException("Failed to cache Google token info", e);
        }
    }

    /**
     * Get Google token information by system token
     *
     * @param systemToken System OAuth2 token
     * @return Google token cache info or null if not found
     */
    public GoogleTokenCacheInfo getGoogleTokenInfo(String systemToken) {
        try {
            String googleTokenKey = GOOGLE_TOKEN_PREFIX + systemToken;
            String cacheInfoJson = redisTemplate.opsForValue().get(googleTokenKey);

            if (cacheInfoJson != null) {
                return objectMapper.readValue(cacheInfoJson, GoogleTokenCacheInfo.class);
            }

            log.debug("No cached Google token info found for system token: {}", systemToken);
            return null;

        } catch (JsonProcessingException e) {
            log.error("Error reading Google token info for system token: {}", systemToken, e);
            return null;
        }
    }

    /**
     * Get system token by Google refresh token
     *
     * @param googleRefreshToken Google refresh token
     * @return System token or null if not found
     */
    public String getSystemTokenByGoogleRefreshToken(String googleRefreshToken) {
        String refreshMappingKey = GOOGLE_REFRESH_PREFIX + googleRefreshToken;
        String systemToken = redisTemplate.opsForValue().get(refreshMappingKey);
        
        if (systemToken != null) {
            log.debug("Found system token for Google refresh token");
            return systemToken;
        }

        log.debug("No system token found for Google refresh token");
        return null;
    }

    /**
     * Update Google token cache with new access token
     *
     * @param systemToken        System OAuth2 token
     * @param newGoogleAccessToken New Google access token
     * @param googleRefreshToken Google refresh token
     */
    public void updateGoogleTokenCache(String systemToken, String newGoogleAccessToken, String googleRefreshToken) {
        try {
            // Get existing cache info
            GoogleTokenCacheInfo existingInfo = getGoogleTokenInfo(systemToken);
            
            if (existingInfo != null) {
                // Update with new access token
                existingInfo.setGoogleAccessToken(newGoogleAccessToken);
                existingInfo.setCacheTime(System.currentTimeMillis());

                String updatedCacheInfoJson = objectMapper.writeValueAsString(existingInfo);

                // Update cache
                String googleTokenKey = GOOGLE_TOKEN_PREFIX + systemToken;
                redisTemplate.opsForValue().set(googleTokenKey, updatedCacheInfoJson, 
                                              Duration.ofSeconds(existingInfo.getExpiresIn() + 300));

                String systemMappingKey = SYSTEM_TOKEN_MAPPING_PREFIX + systemToken;
                redisTemplate.opsForValue().set(systemMappingKey, updatedCacheInfoJson, 
                                              Duration.ofSeconds(existingInfo.getExpiresIn() + 300));

                log.info("Updated Google token cache for system token: {}", systemToken);
            } else {
                log.warn("No existing Google token cache found for system token: {}", systemToken);
            }

        } catch (JsonProcessingException e) {
            log.error("Error updating Google token cache for system token: {}", systemToken, e);
            throw new RuntimeException("Failed to update Google token cache", e);
        }
    }

    /**
     * Remove Google token cache
     *
     * @param systemToken System OAuth2 token
     */
    public void removeGoogleTokenCache(String systemToken) {
        try {
            // Get existing info to clean up refresh token mapping
            GoogleTokenCacheInfo existingInfo = getGoogleTokenInfo(systemToken);

            // Remove main cache entries
            String googleTokenKey = GOOGLE_TOKEN_PREFIX + systemToken;
            String systemMappingKey = SYSTEM_TOKEN_MAPPING_PREFIX + systemToken;
            
            redisTemplate.delete(googleTokenKey);
            redisTemplate.delete(systemMappingKey);

            // Remove refresh token mapping
            if (existingInfo != null && existingInfo.getGoogleRefreshToken() != null) {
                String refreshMappingKey = GOOGLE_REFRESH_PREFIX + existingInfo.getGoogleRefreshToken();
                redisTemplate.delete(refreshMappingKey);
            }

            log.info("Removed Google token cache for system token: {}", systemToken);

        } catch (Exception e) {
            log.error("Error removing Google token cache for system token: {}", systemToken, e);
        }
    }

    /**
     * Check if Google token is cached for system token
     *
     * @param systemToken System OAuth2 token
     * @return true if cached, false otherwise
     */
    public boolean isGoogleTokenCached(String systemToken) {
        String googleTokenKey = GOOGLE_TOKEN_PREFIX + systemToken;
        return Boolean.TRUE.equals(redisTemplate.hasKey(googleTokenKey));
    }

    /**
     * Get all cached Google token keys (for admin purposes)
     *
     * @return Set of cached token keys
     */
    public Set<String> getAllCachedGoogleTokenKeys() {
        return redisTemplate.keys(GOOGLE_TOKEN_PREFIX + "*");
    }

    /**
     * Extend Google token cache expiration
     *
     * @param systemToken System OAuth2 token
     * @param additionalSeconds Additional seconds to extend
     */
    public void extendGoogleTokenCacheExpiration(String systemToken, long additionalSeconds) {
        String googleTokenKey = GOOGLE_TOKEN_PREFIX + systemToken;
        String systemMappingKey = SYSTEM_TOKEN_MAPPING_PREFIX + systemToken;
        
        redisTemplate.expire(googleTokenKey, additionalSeconds, TimeUnit.SECONDS);
        redisTemplate.expire(systemMappingKey, additionalSeconds, TimeUnit.SECONDS);
        
        log.info("Extended Google token cache expiration for system token: {} by {} seconds", 
                systemToken, additionalSeconds);
    }

    /**
     * Clean up expired Google token caches
     */
    public void cleanupExpiredGoogleTokens() {
        Set<String> allKeys = getAllCachedGoogleTokenKeys();
        int cleanedCount = 0;
        
        for (String key : allKeys) {
            try {
                String cacheInfoJson = redisTemplate.opsForValue().get(key);
                if (cacheInfoJson != null) {
                    GoogleTokenCacheInfo cacheInfo = objectMapper.readValue(cacheInfoJson, GoogleTokenCacheInfo.class);
                    
                    // Check if token is expired (with some buffer)
                    long tokenAge = (System.currentTimeMillis() - cacheInfo.getCacheTime()) / 1000;
                    if (tokenAge > cacheInfo.getExpiresIn() + 600) { // 10 minutes buffer
                        String systemToken = cacheInfo.getSystemToken();
                        removeGoogleTokenCache(systemToken);
                        cleanedCount++;
                    }
                }
            } catch (Exception e) {
                log.warn("Error checking expiration for key: {}", key, e);
            }
        }
        
        if (cleanedCount > 0) {
            log.info("Cleaned up {} expired Google token caches", cleanedCount);
        }
    }
}