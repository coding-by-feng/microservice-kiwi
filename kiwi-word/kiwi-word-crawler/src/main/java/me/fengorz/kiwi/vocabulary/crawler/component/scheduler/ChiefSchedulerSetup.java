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
import me.fengorz.kiwi.vocabulary.crawler.component.scheduler.base.Scheduler;

/**
 * 定时扫描对列表 @Author zhanshifeng @Date 2019/10/29 4:12 PM
 */
@Component
@Slf4j
public class ChiefSchedulerSetup {

    private static final long INTERVAL = 1000 * 8;

    @Scheduled(fixedDelay = INTERVAL)
    public void setup() {
        Optional.ofNullable(SpringUtils.getBeansList(Scheduler.class))
            .ifPresent(list -> list.forEach(Scheduler::schedule));
    }
}
