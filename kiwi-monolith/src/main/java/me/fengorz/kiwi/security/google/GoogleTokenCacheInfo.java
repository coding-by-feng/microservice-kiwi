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

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Google Token Cache Information
 *
 * @author codingByFeng
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoogleTokenCacheInfo implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final long EXPIRING_SOON_THRESHOLD_SECONDS = 300; // 5 minutes

    private String systemToken;
    private String googleAccessToken;
    private String googleRefreshToken;
    private LocalDateTime expiresAt;
    private GoogleUserInfo googleUserInfo;

    // Legacy fields for backward compatibility
    private String email;
    private String googleUserId;

    @JsonIgnore
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Alias for isExpired() for backward compatibility
     */
    @JsonIgnore
    public boolean ifExpired() {
        return isExpired();
    }

    @JsonIgnore
    public boolean isExpiringSoon() {
        if (expiresAt == null) {
            return false;
        }
        long remainingSeconds = getRemainingTimeInSeconds();
        return remainingSeconds > 0 && remainingSeconds < EXPIRING_SOON_THRESHOLD_SECONDS;
    }

    /**
     * Alias for isExpiringSoon() for backward compatibility
     */
    @JsonIgnore
    public boolean ifExpiringSoon() {
        return isExpiringSoon();
    }

    @JsonIgnore
    public long getRemainingTimeInSeconds() {
        if (expiresAt == null) {
            return 0;
        }
        Duration duration = Duration.between(LocalDateTime.now(), expiresAt);
        return Math.max(0, duration.getSeconds());
    }
}
