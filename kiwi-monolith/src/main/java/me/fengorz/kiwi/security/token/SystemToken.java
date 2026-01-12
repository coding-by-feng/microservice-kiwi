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
package me.fengorz.kiwi.security.token;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

/**
 * System Token entity stored in Redis
 *
 * @author codingByFeng
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemToken implements Serializable {

    private static final long serialVersionUID = 1L;

    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Set<String> scope;
    private LocalDateTime expiresAt;
    private LocalDateTime refreshTokenExpiresAt;

    // User information
    private Integer userId;
    private String username;
    private Integer deptId;
    private String email;
    private String realName;
    private String avatar;
    private Boolean isAdmin;

    // Auth method: "google_sso" or "standard"
    private String authMethod;

    // Google-specific fields (only for google_sso)
    private String googleUserId;

    // Additional info
    private Map<String, Object> additionalInfo;

    @JsonIgnore
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    @JsonIgnore
    public boolean isRefreshTokenExpired() {
        return refreshTokenExpiresAt != null && LocalDateTime.now().isAfter(refreshTokenExpiresAt);
    }

    @JsonIgnore
    public long getExpiresIn() {
        if (expiresAt == null) {
            return 0;
        }
        long seconds = java.time.Duration.between(LocalDateTime.now(), expiresAt).getSeconds();
        return Math.max(0, seconds);
    }
}
