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

package me.fengorz.kiwi.vocabulary.crawler.component.scheduler.base;

import lombok.RequiredArgsConstructor;
import me.fengorz.kiwi.common.sdk.util.lang.collection.KiwiCollectionUtils;
import me.fengorz.kiwi.word.api.entity.FetchQueueDO;
import me.fengorz.kiwi.word.api.exception.SchedulerException;
import me.fengorz.kiwi.word.api.feign.IBizAPI;

import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * @Author zhanshifeng
 * @Date 2020/7/29 2:16 PM
 */
@RequiredArgsConstructor
public abstract class AbstractScheduler implements IScheduler {

    protected final IBizAPI bizAPI;
    protected final Object barrier = new Object();
    protected CountDownLatch countDownLatch;

    protected abstract List<FetchQueueDO> getQueueDO(SchedulerDTO dto);

    private static final String COUNT_DOWN_LATCH_ERROR = "countDownLatch error!";

    protected void schedule(SchedulerDTO dto) {
        if (countDownLatch != null && countDownLatch.getCount() > 0) {
            return;
        }

        List<FetchQueueDO> list = getQueueDO(dto);
        if (KiwiCollectionUtils.isEmpty(list)) {
            return;
        }

        list.forEach(this::execute);
        if (countDownLatch == null) {
            return;
        }

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            throw new SchedulerException(COUNT_DOWN_LATCH_ERROR);
        }

    }

    protected abstract void execute(FetchQueueDO queue);
}
