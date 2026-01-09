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

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Message for word crawl operations
 *
 * @author codingByFeng
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WordCrawlMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Word name to crawl
     */
    private String wordName;

    /**
     * Word ID if already exists
     */
    private Integer wordId;

    /**
     * Target URL to crawl from
     */
    private String targetUrl;

    /**
     * Crawl type (dictionary, pronunciation, example, etc.)
     */
    private String crawlType;

    /**
     * Priority level
     */
    private Integer priority;

    /**
     * Timestamp when message was created
     */
    private LocalDateTime createdAt;

    /**
     * Number of retry attempts
     */
    private Integer retryCount;
}
