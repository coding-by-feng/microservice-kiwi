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

import java.util.Optional;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.api.constant.CommonConstants;
import me.fengorz.kiwi.word.api.common.WordCrawlerConstants;
import me.fengorz.kiwi.word.api.dto.queue.WordFetchMessageDTO;
import me.fengorz.kiwi.word.api.dto.remote.WordFetchQueuePageDTO;
import me.fengorz.kiwi.word.api.entity.WordFetchQueueDO;
import me.fengorz.kiwi.word.api.feign.IWordFetchAPI;

/**
 * @Description TODO
 * @Author zhanshifeng
 * @Date 2019/10/30 10:33 AM
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AsyncFetchWordProducer implements IProducer {

    private final MqSender mqSender;
    private final IWordFetchAPI wordFetchAPI;

    @Override
    public void produce() {
        WordFetchQueueDO wordFetchQueue = new WordFetchQueueDO().setFetchStatus(WordCrawlerConstants.STATUS_TO_FETCH)
            .setIsValid(CommonConstants.FLAG_Y).setIsLock(CommonConstants.FLAG_NO);
        WordFetchQueuePageDTO wordFetchQueuePage =
            new WordFetchQueuePageDTO().setWordFetchQueue(wordFetchQueue).setPage(new Page<>(1, 20));
        Optional.of(wordFetchAPI.pageQueue(wordFetchQueuePage)).get().getData().forEach(this::fetchWord);
    }

    /**
     * 异步调用爬虫待抓取队列的消息发送
     *
     * @param queue
     */
    @Async
    public void fetchWord(WordFetchQueueDO queue) {
        queue.setIsLock(CommonConstants.FLAG_YES);
        Optional.of(wordFetchAPI.updateQueueById(queue)).ifPresent(response -> {
            if (response.isSuccess()) {
                mqSender.send(new WordFetchMessageDTO().setWord(queue.getWordName()).setQueueId(queue.getQueueId()));
            }
        });
    }

}
