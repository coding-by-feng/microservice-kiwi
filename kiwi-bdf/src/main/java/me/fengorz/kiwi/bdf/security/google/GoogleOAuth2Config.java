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

package me.fengorz.kiwi.bdf.security.google;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.sdk.constant.SecurityConstants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.redis.RedisTokenStore;

/**
 * Configuration for Google OAuth2 integration with Redis caching
 *
 * @Author Kason Zhan
 * @Date 2025-06-16
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class GoogleOAuth2Config {

    private final RedisConnectionFactory redisConnectionFactory;

    /**
     * Redis template specifically for Google OAuth2 operations
     * Using String serialization for both keys and values for compatibility
     *
     * @return RedisTemplate configured for Google OAuth2 operations
     */
    @Bean("googleOAuth2RedisTemplate")
    public RedisTemplate<String, String> googleOAuth2RedisTemplate() {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);
        
        // Use String serializer for both keys and values
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(stringSerializer);
        
        template.afterPropertiesSet();
        
        log.info("Google OAuth2 Redis template configured successfully");
        return template;
    }

    @Bean
    public TokenStore tokenStore() {
        RedisTokenStore tokenStore = new RedisTokenStore(redisConnectionFactory);
        tokenStore.setPrefix(SecurityConstants.PROJECT_PREFIX + SecurityConstants.OAUTH_PREFIX);
        return tokenStore;
    }

    /**
     * ObjectMapper bean for JSON serialization/deserialization
     * This is used by GoogleTokenCacheService for converting objects to/from JSON
     *
     * @return ObjectMapper instance
     */
    @Bean("googleOAuth2ObjectMapper")
    public ObjectMapper googleOAuth2ObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        
        // Configure ObjectMapper settings for Google OAuth2 operations
        objectMapper.findAndRegisterModules();
        
        log.info("Google OAuth2 ObjectMapper configured successfully");
        return objectMapper;
    }

}