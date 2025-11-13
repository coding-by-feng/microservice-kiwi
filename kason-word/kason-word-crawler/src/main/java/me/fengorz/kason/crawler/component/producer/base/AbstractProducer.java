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

package me.fengorz.kason.crawler.component.producer.base;

import lombok.extern.slf4j.Slf4j;
import me.fengorz.kason.common.sdk.constant.GlobalConstants;
import me.fengorz.kason.common.sdk.util.lang.collection.KasonCollectionUtils;
import me.fengorz.kason.word.api.common.ApiCrawlerConstants;
import me.fengorz.kason.word.api.common.enumeration.CrawlerStatusEnum;
import me.fengorz.kason.word.api.entity.FetchQueueDO;
import me.fengorz.kason.word.api.feign.DictFetchApi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;

/**
 * @Author Kason Zhan
 * @Date 2020/7/29 2:16 PM
 */
@Slf4j
public abstract class AbstractProducer implements MqProducer {

    public final Semaphore barrier;
    protected final DictFetchApi dictFetchApi;
    protected final MqSender mqSender;
    protected Integer infoType;

    public AbstractProducer(DictFetchApi dictFetchApi, MqSender mqSender) {
        this.dictFetchApi = dictFetchApi;
        this.mqSender = mqSender;
        this.barrier = new Semaphore(2);
    }

    @Override
    public Semaphore getBarrier() {
        return this.barrier;
    }

    protected List<FetchQueueDO> getQueueDO(Integer status) {
        return dictFetchApi.pageQueueInLock(status, 0, 20, infoType).getData();
    }

    protected void produce(Integer... status) {
        try {
            barrier.acquire(1);
            List<FetchQueueDO> list = new ArrayList<>();
            for (Integer temp : status) {
                Optional.ofNullable(this.getQueueDO(temp)).ifPresent(list::addAll);
            }

            log.info("Fetching queue size is:{}, status is: {}", list.size(),
                    Arrays.stream(status).map(CrawlerStatusEnum::fromStatus).map(CrawlerStatusEnum::name)
                            .collect(Collectors.joining(GlobalConstants.SYMBOL_COMMA)));

            if (KasonCollectionUtils.isEmpty(list)) {
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
            dictFetchApi.updateQueueById(queue);
            log.info("Words[{}] repeatedly fetch exceptions, ready to clear historical exception data!", queue);
            return true;
        }
        return false;
    }
}
