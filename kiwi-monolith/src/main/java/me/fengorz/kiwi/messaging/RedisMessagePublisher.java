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
package me.fengorz.kiwi.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.config.RedisMessageConfig;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis Message Publisher
 * Publishes messages to Redis Pub/Sub channels
 *
 * @author codingByFeng
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisMessagePublisher {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Publish a word fetch message
     */
    public void publishWordFetch(WordFetchMessage message) {
        log.debug("Publishing word fetch message: {}", message);
        redisTemplate.convertAndSend(RedisMessageConfig.TOPIC_WORD_FETCH, message);
    }

    /**
     * Publish a word crawl message
     */
    public void publishWordCrawl(WordCrawlMessage message) {
        log.debug("Publishing word crawl message: {}", message);
        redisTemplate.convertAndSend(RedisMessageConfig.TOPIC_WORD_CRAWL, message);
    }

    /**
     * Publish an AI task message
     */
    public void publishAiTask(AiTaskMessage message) {
        log.debug("Publishing AI task message: {}", message);
        redisTemplate.convertAndSend(RedisMessageConfig.TOPIC_AI_TASK, message);
    }

    /**
     * Publish a notification message
     */
    public void publishNotification(NotificationMessage message) {
        log.debug("Publishing notification message: {}", message);
        redisTemplate.convertAndSend(RedisMessageConfig.TOPIC_NOTIFICATION, message);
    }

    /**
     * Publish to a custom channel
     */
    public void publish(String channel, Object message) {
        log.debug("Publishing message to channel {}: {}", channel, message);
        redisTemplate.convertAndSend(channel, message);
    }
}
