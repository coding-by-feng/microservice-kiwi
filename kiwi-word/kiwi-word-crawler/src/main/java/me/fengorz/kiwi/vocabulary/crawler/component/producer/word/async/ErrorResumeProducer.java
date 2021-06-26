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

package me.fengorz.kiwi.vocabulary.crawler.component.producer.word.async;

import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.api.constant.CommonConstants;
import me.fengorz.kiwi.common.sdk.util.lang.collection.KiwiCollectionUtils;
import me.fengorz.kiwi.vocabulary.crawler.component.producer.base.AbstractProducer;
import me.fengorz.kiwi.vocabulary.crawler.component.producer.base.IProducer;
import me.fengorz.kiwi.vocabulary.crawler.component.producer.base.ISender;
import me.fengorz.kiwi.word.api.common.WordCrawlerConstants;
import me.fengorz.kiwi.word.api.entity.FetchQueueDO;
import me.fengorz.kiwi.word.api.feign.IBizAPI;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

/**
 * 爬虫异常重启-消息队列生产者
 *
 * @Author zhanshifeng
 * @Date 2019/10/30 10:33 AM
 */
@Component
@Slf4j
public class ErrorResumeProducer extends AbstractProducer implements IProducer {

    public ErrorResumeProducer(IBizAPI bizAPI, ISender sender) {
        super(bizAPI, sender);
        this.infoType = WordCrawlerConstants.QUEUE_INFO_TYPE_WORD;
    }

    @Override
    public void produce() {
        resumeDelPronunciationError();
        resumeOverlap();
    }

    private void resumeDelPronunciationError() {
        List<FetchQueueDO> list = new ArrayList<>();
        synchronized (barrier) {
            List<FetchQueueDO> delPronunciationFailList = (bizAPI.pageQueue(WordCrawlerConstants.STATUS_DEL_PRONUNCIATION_FAIL, 0, 20, WordCrawlerConstants.QUEUE_INFO_TYPE_WORD)).getData();
            if (KiwiCollectionUtils.isNotEmpty(delPronunciationFailList)) {
                list.addAll(delPronunciationFailList);
            }
            List<FetchQueueDO> delBaseFailList = (bizAPI.pageQueue(WordCrawlerConstants.STATUS_DEL_BASE_FAIL, 0, 20, WordCrawlerConstants.QUEUE_INFO_TYPE_WORD)).getData();
            if (KiwiCollectionUtils.isNotEmpty(delBaseFailList)) {
                list.addAll(delBaseFailList);
            }
        }
        if (KiwiCollectionUtils.isEmpty(list)) {
            return;
        }
        synchronized (barrier) {
            list.forEach(queue -> {
                queue.setWordId(0);
                this.execute(queue);
            });
        }
    }

    private void resumeOverlap() {
        List<FetchQueueDO> list = new LinkedList<>();
        synchronized (barrier) {
            Optional.ofNullable(bizAPI.listOverlapInUnLock()).ifPresent(wordNameList -> {
                List<String> wordList = wordNameList.getData();
                if (KiwiCollectionUtils.isEmpty(wordList)) {
                    return;
                }
                wordList.forEach(wordName -> Optional.ofNullable(bizAPI.getOneByWordName(wordName).getData()).ifPresent(list::add));
            });
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
    protected void execute(FetchQueueDO queue) {
        queue.setIsLock(CommonConstants.FLAG_YES);
        queue.setFetchStatus(WordCrawlerConstants.STATUS_TO_FETCH);
        bizAPI.updateQueueById(queue);
    }
}
