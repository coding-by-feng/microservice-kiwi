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

package me.fengorz.kason.crawler.component.producer.word.async;

import lombok.extern.slf4j.Slf4j;
import me.fengorz.kason.common.sdk.annotation.log.LogMarker;
import me.fengorz.kason.common.sdk.constant.GlobalConstants;
import me.fengorz.kason.common.sdk.util.lang.collection.KasonCollectionUtils;
import me.fengorz.kason.crawler.component.producer.base.AbstractProducer;
import me.fengorz.kason.crawler.component.producer.base.MqProducer;
import me.fengorz.kason.crawler.component.producer.base.MqSender;
import me.fengorz.kason.word.api.common.ApiCrawlerConstants;
import me.fengorz.kason.word.api.common.enumeration.CrawlerStatusEnum;
import me.fengorz.kason.word.api.common.enumeration.WordTypeEnum;
import me.fengorz.kason.word.api.entity.FetchQueueDO;
import me.fengorz.kason.word.api.feign.DictFetchApi;
import me.fengorz.kason.word.api.util.WordApiUtils;
import org.apache.commons.collections4.ListUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 爬虫异常重启-消息队列生产者 @Author Kason Zhan @Date 2019/10/30 10:33 AM
 */
@Slf4j
@Component
public class ErrorResumeProducer extends AbstractProducer implements MqProducer {

    public ErrorResumeProducer(DictFetchApi dictFetchApi, MqSender mqSender) {
        super(dictFetchApi, mqSender);
        this.infoType = WordTypeEnum.WORD.getType();
    }

    @Override
    public void produce() {
        // SpringUtils.getBean(ErrorResumeProducer.class).resumeDelPronunciationError();
        // SpringUtils.getBean(ErrorResumeProducer.class).resumeOverlap();
        log.info("ErrorResumeProducer produce method is starting");
        this.resumeDelPronunciationError();
        log.info("ErrorResumeProducer produce method has ended");
        // this.resumeOverlap();
    }

    @LogMarker
    public void resumeDelPronunciationError() {
        List<FetchQueueDO> list = new ArrayList<>();
        List<FetchQueueDO> delPronunciationFailList =
            dictFetchApi.pageQueue(ApiCrawlerConstants.STATUS_DEL_PRONUNCIATION_FAIL, 0, 20,
                WordTypeEnum.WORD.getType()).getData();
        if (KasonCollectionUtils.isNotEmpty(delPronunciationFailList)) {
            list.addAll(delPronunciationFailList);
        }
        List<FetchQueueDO> delBaseFailList = (dictFetchApi.pageQueue(ApiCrawlerConstants.STATUS_DEL_BASE_FAIL, 0, 20,
            WordTypeEnum.WORD.getType())).getData();
        if (KasonCollectionUtils.isNotEmpty(delBaseFailList)) {
            list.addAll(delBaseFailList);
        }
        log.info("resumeDelPronunciationError list size is {}", list.size());
        if (KasonCollectionUtils.isEmpty(list)) {
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
        if (KasonCollectionUtils.isEmpty(list)) {
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
        queue.setFetchStatus(CrawlerStatusEnum.STATUS_TO_FETCH.getStatus());
        dictFetchApi.updateQueueById(queue);
    }
}
