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

package me.fengorz.kiwi.dict.crawler.common;

import lombok.experimental.UtilityClass;

/**
 * @Description TODO
 * @Author zhanshifeng
 * @Date 2022/8/28 01:08
 */
@UtilityClass
public class CrawlerConstants {

    public interface COMPONENT_BEAN_ID {
        String GENERATE_REVIEW_RECORD_DAILY_SCHEDULER = "generateReviewRecordDailyScheduler";
        String REFRESH_ALL_API_KEY_DAILY_SCHEDULER = "refreshAllApiKeyDailyScheduler";
    }

}
