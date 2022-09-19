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

package me.fengorz.kiwi.dict.crawler.component.producer.word.async;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.collections4.ListUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.sdk.annotation.log.LogMarker;
import me.fengorz.kiwi.common.sdk.constant.GlobalConstants;
import me.fengorz.kiwi.common.sdk.util.lang.collection.KiwiCollectionUtils;
import me.fengorz.kiwi.dict.crawler.component.producer.base.AbstractProducer;
import me.fengorz.kiwi.dict.crawler.component.producer.base.MqProducer;
import me.fengorz.kiwi.dict.crawler.component.producer.base.MqSender;
import me.fengorz.kiwi.word.api.common.ApiCrawlerConstants;
import me.fengorz.kiwi.word.api.entity.FetchQueueDO;
import me.fengorz.kiwi.word.api.feign.DictFetchApi;
import me.fengorz.kiwi.word.api.util.WordApiUtils;

/**
 * 爬虫异常重启-消息队列生产者 @Author zhanshifeng @Date 2019/10/30 10:33 AM
 */
@Slf4j
@Component
public class ErrorResumeProducer extends AbstractProducer implements MqProducer {

    public ErrorResumeProducer(DictFetchApi dictFetchApi, MqSender mqSender) {
        super(dictFetchApi, mqSender);
        this.infoType = ApiCrawlerConstants.QUEUE_INFO_TYPE_WORD;
    }

    @Override
    public void produce() {
        // SpringUtils.getBean(ErrorResumeProducer.class).resumeDelPronunciationError();
        // SpringUtils.getBean(ErrorResumeProducer.class).resumeOverlap();
        this.resumeDelPronunciationError();
        // this.resumeOverlap();
    }

    @LogMarker
    public void resumeDelPronunciationError() {
        List<FetchQueueDO> list = new ArrayList<>();
        List<FetchQueueDO> delPronunciationFailList =
            dictFetchApi.pageQueue(ApiCrawlerConstants.STATUS_DEL_PRONUNCIATION_FAIL, 0, 20,
                ApiCrawlerConstants.QUEUE_INFO_TYPE_WORD).getData();
        if (KiwiCollectionUtils.isNotEmpty(delPronunciationFailList)) {
            list.addAll(delPronunciationFailList);
        }
        List<FetchQueueDO> delBaseFailList = (dictFetchApi.pageQueue(ApiCrawlerConstants.STATUS_DEL_BASE_FAIL, 0, 20,
            ApiCrawlerConstants.QUEUE_INFO_TYPE_WORD)).getData();
        if (KiwiCollectionUtils.isNotEmpty(delBaseFailList)) {
            list.addAll(delBaseFailList);
        }
        log.info("resumeDelPronunciationError list size is {}", list.size());
        if (KiwiCollectionUtils.isEmpty(list)) {
            return;
        }
        list.forEach(queue -> {
            queue.setWordId(0);
            this.execute(queue);
        });
    }

    @LogMarker
    public void resumeOverlap() {
        List<FetchQueueDO> list = new LinkedList<>();
        ListUtils.emptyIfNull(dictFetchApi.listOverlapAnyway().getData()).stream()
            .peek(wordName -> log.info("Overlapped wordName is {}", wordName)).map(wordName -> {
                FetchQueueDO queue = dictFetchApi.getAnyOne(WordApiUtils.encode(wordName)).getData();
                if (queue == null) {
                    log.info("The word queue does not exist, push it[{}] in a queue", wordName);
                    this.dictFetchApi.queryWord(WordApiUtils.encode(wordName));
                }
                return queue;
            }).peek(word -> log.info("Overlapped word is {}", word)).filter(Objects::nonNull)
            .collect(Collectors.toCollection(() -> list));
        log.info("resumeOverlap list size is {}", list.size());
        if (KiwiCollectionUtils.isEmpty(list)) {
            return;
        }
        list.forEach(queue -> {
            log.info("Data for the word {} is duplicate. Dirty data is to be deleted", queue.getWordName());
            queue.setIsLock(GlobalConstants.FLAG_YES);
            queue.setFetchStatus(ApiCrawlerConstants.STATUS_TO_DEL_BASE);
            dictFetchApi.updateQueueById(queue);
        });
    }

    /**
     * 异步调用爬虫待抓取队列的消息发送
     *
     * @param queue
     */
    @Async
    @Override
    protected void execute(FetchQueueDO queue) {
        queue.setIsLock(GlobalConstants.FLAG_YES);
        queue.setFetchStatus(ApiCrawlerConstants.STATUS_TO_FETCH);
        dictFetchApi.updateQueueById(queue);
    }
}
