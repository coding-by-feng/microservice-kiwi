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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * Google Token Cache Service
 *
 * @author codingByFeng
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleTokenCacheService {

    private static final String GOOGLE_TOKEN_CACHE_PREFIX = "google:token:";
    private static final long DEFAULT_CACHE_EXPIRATION_SECONDS = 3600;

    private final RedisTemplate<String, Object> redisTemplate;

    public void cacheGoogleTokenInfo(String systemToken, GoogleTokenCacheInfo googleTokenInfo) {
        String key = GOOGLE_TOKEN_CACHE_PREFIX + systemToken;
        redisTemplate.opsForValue().set(key, googleTokenInfo, DEFAULT_CACHE_EXPIRATION_SECONDS, TimeUnit.SECONDS);
        log.debug("Cached Google token info for system token: {}", systemToken);
    }

    public GoogleTokenCacheInfo getGoogleTokenInfo(String systemToken) {
        String key = GOOGLE_TOKEN_CACHE_PREFIX + systemToken;
        Object cached = redisTemplate.opsForValue().get(key);
        if (cached instanceof GoogleTokenCacheInfo) {
            return (GoogleTokenCacheInfo) cached;
        }
        return null;
    }

    public boolean isGoogleTokenCached(String systemToken) {
        String key = GOOGLE_TOKEN_CACHE_PREFIX + systemToken;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    public void removeGoogleTokenInfo(String systemToken) {
        String key = GOOGLE_TOKEN_CACHE_PREFIX + systemToken;
        redisTemplate.delete(key);
        log.debug("Removed Google token info for system token: {}", systemToken);
    }

    public void extendGoogleTokenCacheExpiration(String systemToken, long additionalSeconds) {
        String key = GOOGLE_TOKEN_CACHE_PREFIX + systemToken;
        GoogleTokenCacheInfo info = getGoogleTokenInfo(systemToken);
        if (info != null) {
            info.setExpiresAt(LocalDateTime.now().plusSeconds(additionalSeconds));
            redisTemplate.opsForValue().set(key, info, additionalSeconds, TimeUnit.SECONDS);
            log.debug("Extended Google token cache expiration for system token: {}", systemToken);
        }
    }
}
