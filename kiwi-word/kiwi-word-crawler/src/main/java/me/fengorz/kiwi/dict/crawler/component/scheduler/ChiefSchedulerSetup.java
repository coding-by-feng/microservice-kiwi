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

import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.sdk.annotation.log.LogMarker;
import me.fengorz.kiwi.common.sdk.util.spring.SpringUtils;
import me.fengorz.kiwi.dict.crawler.component.scheduler.base.Scheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 定时扫描对列表 @Author Kason Zhan @Date 2019/10/29 4:12 PM
 */
@Slf4j
@Component
public class ChiefSchedulerSetup {

    private static final long INTERVAL = 1000 * 8;

    @LogMarker(isPrintExecutionTime = true)
    @Scheduled(fixedDelay = INTERVAL)
    public void setup() {
        log.info("ChiefSchedulerSetup setup");
        Optional.ofNullable(SpringUtils.getBeansList(Scheduler.class))
            .ifPresent(list -> list.forEach(Scheduler::schedule));
    }
}
