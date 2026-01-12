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
package me.fengorz.kiwi.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import me.fengorz.kiwi.messaging.RedisMessageSubscriber;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis Message Queue Configuration
 * Uses Redis Pub/Sub for message queue functionality instead of RabbitMQ
 *
 * @author codingByFeng
 */
@Configuration
public class RedisMessageConfig {

    /**
     * Channel topics for different message types
     */
    public static final String TOPIC_WORD_FETCH = "kiwi:word:fetch";
    public static final String TOPIC_WORD_CRAWL = "kiwi:word:crawl";
    public static final String TOPIC_AI_TASK = "kiwi:ai:task";
    public static final String TOPIC_NOTIFICATION = "kiwi:notification";

    /**
     * Configure RedisTemplate with JSON serialization
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Configure ObjectMapper for JSON serialization
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        objectMapper.activateDefaultTyping(objectMapper.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL);
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Use Jackson2JsonRedisSerializer for value serialization
        Jackson2JsonRedisSerializer<Object> jsonSerializer = new Jackson2JsonRedisSerializer<>(objectMapper, Object.class);

        // Use StringRedisSerializer for key serialization
        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        template.afterPropertiesSet();

        return template;
    }

    /**
     * Redis Message Listener Container
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            MessageListenerAdapter wordFetchListenerAdapter,
            MessageListenerAdapter wordCrawlListenerAdapter,
            MessageListenerAdapter aiTaskListenerAdapter,
            MessageListenerAdapter notificationListenerAdapter) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);

        // Register listeners for each topic
        container.addMessageListener(wordFetchListenerAdapter, wordFetchTopic());
        container.addMessageListener(wordCrawlListenerAdapter, wordCrawlTopic());
        container.addMessageListener(aiTaskListenerAdapter, aiTaskTopic());
        container.addMessageListener(notificationListenerAdapter, notificationTopic());

        return container;
    }

    /**
     * Message listener adapters for each subscriber
     */
    @Bean
    public MessageListenerAdapter wordFetchListenerAdapter(RedisMessageSubscriber subscriber) {
        return new MessageListenerAdapter(subscriber, "handleWordFetchMessage");
    }

    @Bean
    public MessageListenerAdapter wordCrawlListenerAdapter(RedisMessageSubscriber subscriber) {
        return new MessageListenerAdapter(subscriber, "handleWordCrawlMessage");
    }

    @Bean
    public MessageListenerAdapter aiTaskListenerAdapter(RedisMessageSubscriber subscriber) {
        return new MessageListenerAdapter(subscriber, "handleAiTaskMessage");
    }

    @Bean
    public MessageListenerAdapter notificationListenerAdapter(RedisMessageSubscriber subscriber) {
        return new MessageListenerAdapter(subscriber, "handleNotificationMessage");
    }

    /**
     * Channel topics
     */
    @Bean
    public ChannelTopic wordFetchTopic() {
        return new ChannelTopic(TOPIC_WORD_FETCH);
    }

    @Bean
    public ChannelTopic wordCrawlTopic() {
        return new ChannelTopic(TOPIC_WORD_CRAWL);
    }

    @Bean
    public ChannelTopic aiTaskTopic() {
        return new ChannelTopic(TOPIC_AI_TASK);
    }

    @Bean
    public ChannelTopic notificationTopic() {
        return new ChannelTopic(TOPIC_NOTIFICATION);
    }
}
