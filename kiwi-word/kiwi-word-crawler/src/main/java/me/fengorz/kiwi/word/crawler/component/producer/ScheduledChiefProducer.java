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

package me.fengorz.kiwi.word.crawler.component.producer;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @Description TODO
 * @Author zhanshifeng
 * @Date 2019/10/29 4:12 PM
 */
@Service("scheduledProducer")
@Slf4j
@RequiredArgsConstructor
public class ScheduledChiefProducer implements IProducer {

    private final AsyncFetchWordProducer asyncFetchWordProducer;

    @Override
    @Scheduled(fixedDelay = 2000L)
    public void produce() {
        asyncFetchWordProducer.produce();
    }

}
