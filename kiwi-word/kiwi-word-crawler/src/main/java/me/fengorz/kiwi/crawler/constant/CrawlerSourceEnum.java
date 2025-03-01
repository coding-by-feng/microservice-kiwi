/*
 *
 * Copyright [2019~2025] [codingByFeng]
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

package me.fengorz.kiwi.crawler.constant;

import me.fengorz.kiwi.common.sdk.constant.GlobalConstants;

/**
 * @author zhanshifeng
 */
public enum CrawlerSourceEnum implements CrawlerSource {
    CAMBRIDGE_CHINESE("Cambridge", "Chinese"), CAMBRIDGE_ENGLISH("Cambridge", "English"),
    COLLINS_CHINESE("Collins", "Chinese");

    private final String source;
    private final String language;

    CrawlerSourceEnum(String source, String language) {
        this.source = source;
        this.language = language;
    }

    @Override
    public String get() {
        return this.language + GlobalConstants.SYMBOL_DELIMITER_STR + this.source;
    }
}
