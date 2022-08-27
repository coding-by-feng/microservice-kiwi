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

package me.fengorz.kiwi.vocabulary.crawler.component.producer.base;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Semaphore;

import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.sdk.constant.GlobalConstants;
import me.fengorz.kiwi.common.sdk.util.lang.collection.KiwiCollectionUtils;
import me.fengorz.kiwi.word.api.common.ApiCrawlerConstants;
import me.fengorz.kiwi.word.api.entity.FetchQueueDO;
import me.fengorz.kiwi.word.api.feign.IBizAPI;

/**
 * @Author zhanshifeng
 * @Date 2020/7/29 2:16 PM
 */
@Slf4j
public abstract class AbstractProducer implements MQProducer {

    protected final IBizAPI bizApi;
    protected final MQSender mqSender;
    protected Integer infoType;
    public final Semaphore barrier;

    public AbstractProducer(IBizAPI bizApi, MQSender mqSender) {
        this.bizApi = bizApi;
        this.mqSender = mqSender;
        this.barrier = new Semaphore(2);
    }

    @Override
    public Semaphore getBarrier() {
        return this.barrier;
    }

    protected List<FetchQueueDO> getQueueDO(Integer status) {
        return bizApi.pageQueueLockIn(status, 0, 20, infoType).getData();
    }

    protected void produce(Integer... status) {
        try {
            barrier.acquire(1);
            List<FetchQueueDO> list = new LinkedList<>();
            for (Integer temp : status) {
                Optional.ofNullable(this.getQueueDO(temp)).ifPresent(list::addAll);
            }
            if (KiwiCollectionUtils.isEmpty(list)) {
                return;
            }

            // 列表里面每一批查到数据处理完之前先上锁
            list.forEach(this::execute);
        } catch (InterruptedException e) {
            log.error("The producer thread is waiting for an exception, {}", e.getMessage(), e);
        } finally {
            barrier.release();
        }
    }

    protected abstract void execute(FetchQueueDO queue);

    protected boolean isCleanUp(FetchQueueDO queue) {
        if (ApiCrawlerConstants.WORD_MAX_FETCH_LIMITED_TIME < queue.getFetchTime()) {
            queue.setFetchTime(0);
            queue.setIsLock(GlobalConstants.FLAG_NO);
            queue.setFetchStatus(ApiCrawlerConstants.STATUS_TO_DEL_BASE);
            queue.setFetchResult(GlobalConstants.EMPTY);
            bizApi.updateQueueById(queue);
            log.info("Words[{}] repeatedly fetch exceptions, ready to clear historical exception data!", queue);
            return true;
        }
        return false;
    }
}
