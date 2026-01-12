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

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Redis Message Subscriber
 * Handles messages received from Redis Pub/Sub channels
 *
 * @author codingByFeng
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisMessageSubscriber {

    private final ObjectMapper objectMapper;

    /**
     * Handle word fetch messages
     */
    public void handleWordFetchMessage(String message) {
        try {
            log.info("Received word fetch message: {}", message);
            WordFetchMessage fetchMessage = objectMapper.readValue(message, WordFetchMessage.class);
            processWordFetch(fetchMessage);
        } catch (Exception e) {
            log.error("Error processing word fetch message: {}", message, e);
        }
    }

    /**
     * Handle word crawl messages
     */
    public void handleWordCrawlMessage(String message) {
        try {
            log.info("Received word crawl message: {}", message);
            WordCrawlMessage crawlMessage = objectMapper.readValue(message, WordCrawlMessage.class);
            processWordCrawl(crawlMessage);
        } catch (Exception e) {
            log.error("Error processing word crawl message: {}", message, e);
        }
    }

    /**
     * Handle AI task messages
     */
    public void handleAiTaskMessage(String message) {
        try {
            log.info("Received AI task message: {}", message);
            AiTaskMessage taskMessage = objectMapper.readValue(message, AiTaskMessage.class);
            processAiTask(taskMessage);
        } catch (Exception e) {
            log.error("Error processing AI task message: {}", message, e);
        }
    }

    /**
     * Handle notification messages
     */
    public void handleNotificationMessage(String message) {
        try {
            log.info("Received notification message: {}", message);
            NotificationMessage notificationMessage = objectMapper.readValue(message, NotificationMessage.class);
            processNotification(notificationMessage);
        } catch (Exception e) {
            log.error("Error processing notification message: {}", message, e);
        }
    }

    /**
     * Process word fetch - override in subclass or inject service
     */
    protected void processWordFetch(WordFetchMessage message) {
        log.info("Processing word fetch for: {}", message.getWordName());
        // TODO: Inject WordFetchService and process
    }

    /**
     * Process word crawl - override in subclass or inject service
     */
    protected void processWordCrawl(WordCrawlMessage message) {
        log.info("Processing word crawl for: {}", message.getWordName());
        // TODO: Inject WordCrawlService and process
    }

    /**
     * Process AI task - override in subclass or inject service
     */
    protected void processAiTask(AiTaskMessage message) {
        log.info("Processing AI task: {}", message.getTaskType());
        // TODO: Inject AIService and process
    }

    /**
     * Process notification - override in subclass or inject service
     */
    protected void processNotification(NotificationMessage message) {
        log.info("Processing notification for user: {}", message.getUserId());
        // TODO: Inject NotificationService and process
    }
}
