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

package me.fengorz.kiwi.dict.crawler.component.scheduler;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.dict.crawler.common.CrawlerConstants;
import me.fengorz.kiwi.dict.crawler.component.scheduler.base.DailyScheduler;
import me.fengorz.kiwi.word.api.feign.IBizAPI;

/**
 * @Description Daily generation of review count records @Author zhanshifeng @Date 2021/8/19 8:54 PM
 */
@Slf4j
@Component(CrawlerConstants.COMPONENT_BEAN_ID.GENERATE_REVIEW_RECORD_DAILY_SCHEDULER)
@EnableScheduling
@RequiredArgsConstructor
public class GenerateReviewRecordDailyScheduler implements DailyScheduler {

    private final IBizAPI api;

    @Override
    public void schedule() {
        log.info("Daily generation of review count records is starting.");
        api.createTheDays();
        log.info("Daily generation of review count records is end.");
    }

}
