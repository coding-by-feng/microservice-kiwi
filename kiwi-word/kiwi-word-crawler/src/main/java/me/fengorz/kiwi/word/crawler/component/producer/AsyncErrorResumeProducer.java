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

import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.api.constant.CommonConstants;
import me.fengorz.kiwi.common.sdk.util.lang.collection.KiwiCollectionUtils;
import me.fengorz.kiwi.word.api.common.WordCrawlerConstants;
import me.fengorz.kiwi.word.api.entity.WordFetchQueueDO;
import me.fengorz.kiwi.word.api.feign.IWordFetchAPI;
import me.fengorz.kiwi.word.crawler.component.producer.base.AbstractProducer;
import me.fengorz.kiwi.word.crawler.component.producer.base.IProducer;
import me.fengorz.kiwi.word.crawler.component.producer.base.ISender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;

/**
 * 爬虫异常重启-消息队列生产者
 *
 * @Author zhanshifeng
 * @Date 2019/10/30 10:33 AM
 */
@Component
@Slf4j
public class AsyncErrorResumeProducer extends AbstractProducer implements IProducer {

    public AsyncErrorResumeProducer(IWordFetchAPI api, ISender sender) {
        super(api, sender);
    }

    @Override
    public void produce() {
        List<WordFetchQueueDO> list;
        synchronized (barrier) {
            list = new LinkedList<>((wordFetchAPI.pageQueueLockIn(WordCrawlerConstants.STATUS_DEL_PRONUNCIATION_FAIL, 0, 20)).getData());
        }
        if (KiwiCollectionUtils.isEmpty(list)) {
            return;
        }
        synchronized (barrier) {
            list.forEach(this::execute);
        }
    }

    /**
     * 异步调用爬虫待抓取队列的消息发送
     *
     * @param queue
     */
    @Async
    @Override
    protected void execute(WordFetchQueueDO queue) {
        queue.setIsLock(CommonConstants.FLAG_NO);
        queue.setFetchStatus(WordCrawlerConstants.STATUS_TO_FETCH);
        queue.setWordId(0);
        wordFetchAPI.updateQueueById(queue);
    }
}
