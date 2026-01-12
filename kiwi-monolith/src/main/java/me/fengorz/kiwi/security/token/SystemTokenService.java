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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.domain.upms.entity.SysUser;
import me.fengorz.kiwi.security.KiwiUser;
import me.fengorz.kiwi.security.google.GoogleUserInfo;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * System Token Service - manages access tokens in Redis
 * Replaces the deprecated Spring Security OAuth2 TokenStore
 *
 * @author codingByFeng
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemTokenService {

    private static final String TOKEN_PREFIX = "kiwi:token:access:";
    private static final String REFRESH_TOKEN_PREFIX = "kiwi:token:refresh:";
    private static final String USER_TOKEN_PREFIX = "kiwi:token:user:";

    private static final long ACCESS_TOKEN_VALIDITY_SECONDS = 30 * 24 * 60 * 60L; // 30 days
    private static final long REFRESH_TOKEN_VALIDITY_SECONDS = 90 * 24 * 60 * 60L; // 90 days

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Generate a new system token for a KiwiUser (username/password login)
     */
    public SystemToken generateToken(KiwiUser user) {
        String accessTokenValue = UUID.randomUUID().toString();
        String refreshTokenValue = UUID.randomUUID().toString();

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime accessTokenExpiry = now.plusSeconds(ACCESS_TOKEN_VALIDITY_SECONDS);
        LocalDateTime refreshTokenExpiry = now.plusSeconds(REFRESH_TOKEN_VALIDITY_SECONDS);

        Set<String> scopes = new HashSet<>();
        scopes.add("read");
        scopes.add("write");
        scopes.add("profile");

        // Check if user has ROLE_ADMIN authority
        boolean isAdmin = user.getAuthorities().stream()
                .anyMatch(auth -> "ROLE_ADMIN".equals(auth.getAuthority()));

        SystemToken token = SystemToken.builder()
                .accessToken(accessTokenValue)
                .refreshToken(refreshTokenValue)
                .tokenType("Bearer")
                .scope(scopes)
                .expiresAt(accessTokenExpiry)
                .refreshTokenExpiresAt(refreshTokenExpiry)
                .userId(user.getUserId())
                .username(user.getUsername())
                .deptId(user.getDeptId())
                .email(user.getEmail())
                .realName(user.getRealName())
                .avatar(user.getAvatar())
                .isAdmin(isAdmin)
                .authMethod("standard")
                .build();

        storeToken(token);
        return token;
    }

    /**
     * Generate a new system token for Google SSO
     */
    public SystemToken generateGoogleToken(SysUser sysUser, GoogleUserInfo googleUserInfo) {
        String accessTokenValue = UUID.randomUUID().toString();
        String refreshTokenValue = UUID.randomUUID().toString();

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime accessTokenExpiry = now.plusSeconds(ACCESS_TOKEN_VALIDITY_SECONDS);
        LocalDateTime refreshTokenExpiry = now.plusSeconds(REFRESH_TOKEN_VALIDITY_SECONDS);

        Set<String> scopes = new HashSet<>();
        scopes.add("read");
        scopes.add("write");
        scopes.add("profile");
        scopes.add("email");

        SystemToken token = SystemToken.builder()
                .accessToken(accessTokenValue)
                .refreshToken(refreshTokenValue)
                .tokenType("Bearer")
                .scope(scopes)
                .expiresAt(accessTokenExpiry)
                .refreshTokenExpiresAt(refreshTokenExpiry)
                .userId(sysUser.getUserId())
                .username(sysUser.getUsername())
                .deptId(sysUser.getDeptId())
                .email(sysUser.getEmail())
                .realName(sysUser.getRealName())
                .avatar(sysUser.getAvatar())
                .isAdmin(false)
                .authMethod("google_sso")
                .googleUserId(googleUserInfo.getId())
                .build();

        storeToken(token);
        return token;
    }

    /**
     * Store token in Redis
     */
    private void storeToken(SystemToken token) {
        // Store access token
        String accessKey = TOKEN_PREFIX + token.getAccessToken();
        redisTemplate.opsForValue().set(accessKey, token, ACCESS_TOKEN_VALIDITY_SECONDS, TimeUnit.SECONDS);

        // Store refresh token mapping
        String refreshKey = REFRESH_TOKEN_PREFIX + token.getRefreshToken();
        redisTemplate.opsForValue().set(refreshKey, token.getAccessToken(), REFRESH_TOKEN_VALIDITY_SECONDS, TimeUnit.SECONDS);

        // Store user-to-token mapping for easy lookup/revocation
        String userKey = USER_TOKEN_PREFIX + token.getUserId();
        redisTemplate.opsForSet().add(userKey, token.getAccessToken());
        redisTemplate.expire(userKey, ACCESS_TOKEN_VALIDITY_SECONDS, TimeUnit.SECONDS);

        log.debug("Stored token for user: {}", token.getUsername());
    }

    /**
     * Read token by access token value
     */
    public SystemToken readAccessToken(String accessToken) {
        String key = TOKEN_PREFIX + accessToken;
        Object cached = redisTemplate.opsForValue().get(key);
        if (cached instanceof SystemToken) {
            return (SystemToken) cached;
        }
        return null;
    }

    /**
     * Read token by refresh token value
     */
    public SystemToken readByRefreshToken(String refreshToken) {
        String refreshKey = REFRESH_TOKEN_PREFIX + refreshToken;
        Object accessTokenValue = redisTemplate.opsForValue().get(refreshKey);
        if (accessTokenValue instanceof String) {
            return readAccessToken((String) accessTokenValue);
        }
        return null;
    }

    /**
     * Refresh an existing token
     */
    public SystemToken refreshToken(String refreshToken) {
        SystemToken existingToken = readByRefreshToken(refreshToken);
        if (existingToken == null || existingToken.isRefreshTokenExpired()) {
            log.warn("Invalid or expired refresh token: {}", refreshToken);
            return null;
        }

        // Remove old token
        removeAccessToken(existingToken.getAccessToken());

        // Create new token with same user info
        String newAccessTokenValue = UUID.randomUUID().toString();
        String newRefreshTokenValue = UUID.randomUUID().toString();

        LocalDateTime now = LocalDateTime.now();
        existingToken.setAccessToken(newAccessTokenValue);
        existingToken.setRefreshToken(newRefreshTokenValue);
        existingToken.setExpiresAt(now.plusSeconds(ACCESS_TOKEN_VALIDITY_SECONDS));
        existingToken.setRefreshTokenExpiresAt(now.plusSeconds(REFRESH_TOKEN_VALIDITY_SECONDS));

        storeToken(existingToken);
        return existingToken;
    }

    /**
     * Remove access token
     */
    public void removeAccessToken(String accessToken) {
        SystemToken token = readAccessToken(accessToken);
        if (token != null) {
            // Remove access token
            String accessKey = TOKEN_PREFIX + accessToken;
            redisTemplate.delete(accessKey);

            // Remove refresh token
            if (token.getRefreshToken() != null) {
                String refreshKey = REFRESH_TOKEN_PREFIX + token.getRefreshToken();
                redisTemplate.delete(refreshKey);
            }

            // Remove from user's token set
            String userKey = USER_TOKEN_PREFIX + token.getUserId();
            redisTemplate.opsForSet().remove(userKey, accessToken);

            log.debug("Removed token for user: {}", token.getUsername());
        }
    }

    /**
     * Remove all tokens for a user
     */
    public void removeAllUserTokens(Integer userId) {
        String userKey = USER_TOKEN_PREFIX + userId;
        Set<Object> tokens = redisTemplate.opsForSet().members(userKey);
        if (tokens != null) {
            for (Object tokenObj : tokens) {
                if (tokenObj instanceof String) {
                    removeAccessToken((String) tokenObj);
                }
            }
        }
        redisTemplate.delete(userKey);
        log.debug("Removed all tokens for user: {}", userId);
    }

    /**
     * Check if token is valid (exists and not expired)
     */
    public boolean isValidToken(String accessToken) {
        SystemToken token = readAccessToken(accessToken);
        return token != null && !token.isExpired();
    }

    /**
     * Build token response map for API responses
     */
    public Map<String, Object> buildTokenResponse(SystemToken token) {
        Map<String, Object> response = new HashMap<>();
        response.put("access_token", token.getAccessToken());
        response.put("token_type", token.getTokenType());
        response.put("expires_in", token.getExpiresIn());
        response.put("scope", token.getScope());
        response.put("refresh_token", token.getRefreshToken());
        return response;
    }

    /**
     * Build detailed token info for check_token endpoint
     */
    public Map<String, Object> buildTokenInfo(SystemToken token) {
        Map<String, Object> info = new HashMap<>();
        info.put("active", !token.isExpired());
        info.put("token_type", token.getTokenType());
        info.put("expires_in", token.getExpiresIn());
        info.put("scope", token.getScope());
        info.put("user_id", token.getUserId());
        info.put("user_name", token.getUsername());
        info.put("dept_id", token.getDeptId());
        info.put("auth_method", token.getAuthMethod());
        info.put("is_admin", token.getIsAdmin());

        if ("google_sso".equals(token.getAuthMethod())) {
            info.put("google_user_id", token.getGoogleUserId());
            info.put("email", token.getEmail());
        }

        return info;
    }
}
