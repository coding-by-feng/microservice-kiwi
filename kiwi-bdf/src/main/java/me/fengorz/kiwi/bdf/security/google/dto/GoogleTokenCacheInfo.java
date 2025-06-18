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

package me.fengorz.kiwi.bdf.security.google.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Google token cache information DTO
 *
 * @Author Kason Zhan
 * @Date 2025-06-16
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoogleTokenCacheInfo {

    /**
     * System OAuth2 token
     */
    private String systemToken;

    /**
     * Google OAuth2 access token
     */
    private String googleAccessToken;

    /**
     * Google OAuth2 refresh token
     */
    private String googleRefreshToken;

    /**
     * Token expiration in seconds
     */
    private Integer expiresIn;

    /**
     * Google user information
     */
    private GoogleUserInfo googleUserInfo;

    /**
     * Cache timestamp
     */
    private Long cacheTime;

    /**
     * Check if the Google token is expired
     *
     * @return true if expired, false otherwise
     */
    public boolean ifExpired() {
        if (cacheTime == null || expiresIn == null) {
            return true;
        }
        
        long currentTime = System.currentTimeMillis();
        long tokenAge = (currentTime - cacheTime) / 1000; // Convert to seconds
        
        return tokenAge >= expiresIn;
    }

    /**
     * Check if the Google token will expire soon (within 5 minutes)
     *
     * @return true if expiring soon, false otherwise
     */
    public boolean ifExpiringSoon() {
        if (cacheTime == null || expiresIn == null) {
            return true;
        }
        
        long currentTime = System.currentTimeMillis();
        long tokenAge = (currentTime - cacheTime) / 1000; // Convert to seconds
        
        return tokenAge >= (expiresIn - 300); // 5 minutes before expiration
    }

    /**
     * Get remaining time in seconds before token expires
     *
     * @return remaining time in seconds, or 0 if expired
     */
    @JsonIgnore
    public long getRemainingTimeInSeconds() {
        if (cacheTime == null || expiresIn == null) {
            return 0;
        }
        
        long currentTime = System.currentTimeMillis();
        long tokenAge = (currentTime - cacheTime) / 1000; // Convert to seconds
        long remaining = expiresIn - tokenAge;
        
        return Math.max(0, remaining);
    }
}