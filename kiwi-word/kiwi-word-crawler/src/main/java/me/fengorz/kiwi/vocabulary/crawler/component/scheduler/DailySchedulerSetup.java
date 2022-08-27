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

package me.fengorz.kiwi.vocabulary.crawler.component.scheduler;

import java.util.Optional;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.sdk.util.spring.SpringUtils;
import me.fengorz.kiwi.vocabulary.crawler.common.CrawlerConstants;
import me.fengorz.kiwi.vocabulary.crawler.component.scheduler.base.DailyScheduler;

@Component
@Slf4j
public class DailySchedulerSetup {

    private static final String FETCH_PARAPHRASE_SCHEDULER = "fetchParaphraseScheduler";

    /**
     * Runs every day at 0 o'clock in the morning.
     */
    @Scheduled(cron = "0 0 0 */1 * ?")
    public void setupAt0Clock() {
        Optional.of(SpringUtils.getBean(CrawlerConstants.COMPONENT_BEAN_ID.GENERATE_REVIEW_RECORD_DAILY_SCHEDULER,
            DailyScheduler.class)).ifPresent(DailyScheduler::schedule);
    }

    /**
     * Runs every day at 6 o'clock in the morning.
     */
    @Scheduled(cron = "0 0 6 */1 * ?")
    public void setupAt6Clock() {
        Optional.of(SpringUtils.getBean(CrawlerConstants.COMPONENT_BEAN_ID.REFRESH_ALL_API_KEY_DAILY_SCHEDULER,
            DailyScheduler.class)).ifPresent(DailyScheduler::schedule);
    }
}
