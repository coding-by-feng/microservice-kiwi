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
package me.fengorz.kiwi.common;

/**
 * Global constants used throughout the application.
 */
public final class GlobalConstants {

    private GlobalConstants() {
        // Prevent instantiation
    }

    // Base packages
    public static final String BASE_PACKAGES = "me.fengorz.kiwi";

    // Flag constants
    public static final Integer FLAG_YES = 1;
    public static final Integer FLAG_NO = 0;
    public static final Integer FLAG_DEL_YES = 1;
    public static final Integer FLAG_DEL_NO = 0;
    public static final Integer FLAG_VALID = 1;
    public static final Integer FLAG_INVALID = 0;
    public static final Integer FLAG_LOCK_YES = 1;
    public static final Integer FLAG_LOCK_NO = 0;

    // User status
    public static final Integer USER_LOCK_FLAG_NORMAL = 0;
    public static final Integer USER_LOCK_FLAG_LOCKED = 9;

    // Register sources
    public static final String REGISTER_SOURCE_LOCAL = "local";
    public static final String REGISTER_SOURCE_GOOGLE = "google";
    public static final String REGISTER_SOURCE_WECHAT = "wechat";
    public static final String REGISTER_SOURCE_QQ = "qq";

    // Word info types
    public static final Integer WORD_INFO_TYPE_WORD = 1;
    public static final Integer WORD_INFO_TYPE_PHRASE = 2;

    // Fetch status
    public static final Integer FETCH_STATUS_WAITING = 0;
    public static final Integer FETCH_STATUS_FETCHING = 1;
    public static final Integer FETCH_STATUS_SUCCESS = 2;
    public static final Integer FETCH_STATUS_FAIL = 3;

    // Default page size
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;

    // Cache key prefixes
    public static final String CACHE_KEY_USER = "user:";
    public static final String CACHE_KEY_WORD = "word:";
    public static final String CACHE_KEY_TOKEN = "token:";

    // Date time patterns
    public static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    public static final String DATE_PATTERN = "yyyy-MM-dd";
    public static final String TIME_PATTERN = "HH:mm:ss";
}
