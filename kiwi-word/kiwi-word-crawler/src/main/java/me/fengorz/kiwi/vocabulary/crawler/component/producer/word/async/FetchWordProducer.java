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
import me.fengorz.kiwi.common.sdk.constant.GlobalConstants;
import me.fengorz.kiwi.vocabulary.crawler.component.producer.base.AbstractProducer;
import me.fengorz.kiwi.vocabulary.crawler.component.producer.base.IProducer;
import me.fengorz.kiwi.vocabulary.crawler.component.producer.base.ISender;
import me.fengorz.kiwi.word.api.common.WordCrawlerConstants;
import me.fengorz.kiwi.word.api.dto.queue.FetchWordMqDTO;
import me.fengorz.kiwi.word.api.entity.FetchQueueDO;
import me.fengorz.kiwi.word.api.feign.IBizAPI;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 抓取单词基本信息-消息队列生产者 @Author zhanshifeng @Date 2019/10/30 10:33 AM
 */
@Component
@Slf4j
public class FetchWordProducer extends AbstractProducer implements IProducer {

    public FetchWordProducer(IBizAPI bizAPI, ISender sender) {
        super(bizAPI, sender);
        this.infoType = WordCrawlerConstants.QUEUE_INFO_TYPE_WORD;
    }

    @Override
    public void produce() {
        super.produce(WordCrawlerConstants.STATUS_TO_FETCH);
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
        queue.setFetchTime(queue.getFetchTime() + 1);
        queue.setFetchResult(GlobalConstants.EMPTY);
        if (null == queue.getWordId() || 0 == queue.getWordId()) {
            queue.setFetchStatus(WordCrawlerConstants.STATUS_DOING_FETCH);
            if (Optional.of(bizAPI.updateQueueById(queue)).get().isSuccess()) {
                sender.fetchWord(
                        new FetchWordMqDTO().setWord(queue.getWordName()).setQueueId(queue.getQueueId()));
            }
        } else {
            // 删除老的数据
            queue.setFetchStatus(WordCrawlerConstants.STATUS_TO_DEL_BASE);
            bizAPI.updateQueueById(queue);
        }
    }
}
