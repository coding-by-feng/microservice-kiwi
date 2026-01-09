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
package me.fengorz.kiwi.domain.word.enumeration;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * Crawler Status Enumeration
 *
 * @author codingByFeng
 */
@Getter
public enum CrawlerStatusEnum {

    /** Status > 0: crawler normal or completed; Status < 0: crawler error; Status = 0: waiting to start */

    /** Partition marker */
    STATUS_PARTITION(-1),
    /** Query error */
    STATUS_TO_QUERY_ERROR(-404),
    /** Jsoup connection failed */
    STATUS_JSOUP_CONNECT_FAILED(-2),
    /** Fetch word failed */
    STATUS_FETCH_FAIL(-3),
    /** Delete pronunciation file failed */
    STATUS_DEL_PRONUNCIATION_FAIL(-4),
    /** Fetch pronunciation file failed */
    STATUS_TO_FETCH_PRONUNCIATION_FAIL(-5),
    /** Fetch related phrase failed */
    STATUS_FETCH_RELATED_PHRASE_FAIL(-6),
    /** Fetch phrase failed */
    STATUS_FETCH_PHRASE_FAIL(-7),
    /** Delete phrase failed */
    STATUS_DEL_PHRASE_FAIL(-8),

    /** To fetch */
    STATUS_TO_FETCH(0),
    /** Fetching base data */
    STATUS_DOING_FETCH(5),
    /** Waiting to delete base data */
    STATUS_TO_DEL_BASE(1),
    /** Deleting base data */
    STATUS_DOING_DEL_BASE(10),
    /** Waiting to delete pronunciation file */
    STATUS_TO_DEL_PRONUNCIATION(2),
    /** Deleting pronunciation file */
    STATUS_DOING_DEL_PRONUNCIATION(20),
    /** Waiting to fetch pronunciation file */
    STATUS_TO_FETCH_PRONUNCIATION(3),
    /** Fetching pronunciation file */
    STATUS_DOING_FETCH_PRONUNCIATION(30),
    /** Delete base data failed - crawler continues */
    STATUS_DEL_BASE_FAIL(100),
    /** All data fetched, waiting to fetch derived phrases */
    STATUS_ALL_SUCCESS(200),
    /** Derived phrases queued */
    STATUS_TO_FETCH_PHRASE(201),
    /** All crawler logic succeeded */
    STATUS_PERFECT_SUCCESS(666);

    private final int status;

    CrawlerStatusEnum(int status) {
        this.status = status;
    }

    private static final Map<Integer, CrawlerStatusEnum> STATUS_MAP = new HashMap<>();

    static {
        for (CrawlerStatusEnum crawlerStatusEnum : values()) {
            STATUS_MAP.put(crawlerStatusEnum.getStatus(), crawlerStatusEnum);
        }
    }

    public static CrawlerStatusEnum fromStatus(Integer status) {
        return STATUS_MAP.get(status);
    }
}
