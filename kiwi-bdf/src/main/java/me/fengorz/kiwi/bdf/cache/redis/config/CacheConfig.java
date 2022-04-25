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

package me.fengorz.kiwi.bdf.cache.redis.config;

import java.time.Duration;
import java.util.Objects;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.AdviceMode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.bdf.cache.redis.CacheKeyGenerator;
import me.fengorz.kiwi.common.sdk.constant.CacheConstants;

/**
 * @Description 缓存配置类
 * @Author zhanshifeng
 * @Date 2019-09-29 10:39
 */
@Slf4j
@Configuration
@EnableCaching(mode = AdviceMode.ASPECTJ)
@AutoConfigureBefore(RedisAutoConfiguration.class)
public class CacheConfig {

    private final RedisConnectionFactory factory;

    public CacheConfig(RedisConnectionFactory factory) {
        log.info("CacheConfig...");
        this.factory = factory;
    }

    @Bean(name = CacheConstants.CACHE_KEY_GENERATOR_BEAN)
    public CacheKeyGenerator cacheKeyGenerator() {
        return new CacheKeyGenerator();
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setKeySerializer(StringRedisSerializer.UTF_8);
        redisTemplate.setHashKeySerializer(StringRedisSerializer.UTF_8);
        redisTemplate.setValueSerializer(StringRedisSerializer.UTF_8);
        redisTemplate.setHashValueSerializer(StringRedisSerializer.UTF_8);
        redisTemplate.setConnectionFactory(factory);
        return redisTemplate;
    }

    @Bean(name = CacheConstants.CACHE_MANAGER_WORD)
    public CacheManager cacheManager(final RedisTemplate<String, ?> redisTemplate) {
        return new RedisCacheManager(
            RedisCacheWriter.lockingRedisCacheWriter(Objects.requireNonNull(redisTemplate.getConnectionFactory())),
            buildCacheConfiguration());
    }

    private RedisCacheConfiguration buildCacheConfiguration() {
        RedisSerializationContext.SerializationPair<Object> pair =
            RedisSerializationContext.SerializationPair.fromSerializer(new JdkSerializationRedisSerializer());
        return RedisCacheConfiguration.defaultCacheConfig().serializeValuesWith(pair).entryTtl(Duration.ZERO);
    }

}
