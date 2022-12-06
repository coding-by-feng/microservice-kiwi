/*
 *
 *   Copyright [2019~2025] [codingByFeng]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 *
 */

package me.fengorz.kiwi.dict.crawler.component.scheduler;

import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.mq.MqAwake;
import me.fengorz.kiwi.common.sdk.util.spring.SpringUtils;
import me.fengorz.kiwi.dict.crawler.component.producer.base.MqProducer;
import me.fengorz.kiwi.dict.crawler.component.scheduler.base.Setup;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 定时扫描对列表
 *
 * @Author zhanshifeng
 * @Date 2019/10/29 4:12 PM
 */
@Slf4j
@Component
public class ChiefProducerSchedulerSetup implements Setup {

    @MqAwake
    @Scheduled(fixedDelay = 10000L)
    public void produce() {
        for (MqProducer producer : Objects.requireNonNull(SpringUtils.getBeansList(MqProducer.class))) {
            try {
                if (!producer.getBarrier().tryAcquire(1, 100, TimeUnit.MILLISECONDS)) {
                    log.warn("Skip the current task[{}] while the previous task is in progress",
                        producer.getClass().getSimpleName());
                    continue;
                }
                try {
                    producer.produce();
                } finally {
                    producer.getBarrier().release();
                }
            } catch (InterruptedException e) {
                log.error("The producer thread is waiting for an exception, {}", e.getMessage(), e);
            }
        }
    }
}
