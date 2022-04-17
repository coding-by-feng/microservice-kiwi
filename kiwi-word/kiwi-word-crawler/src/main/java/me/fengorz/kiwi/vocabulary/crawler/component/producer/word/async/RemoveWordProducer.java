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

package me.fengorz.kiwi.vocabulary.crawler.component.producer.word.async;

import java.util.Optional;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.sdk.constant.GlobalConstants;
import me.fengorz.kiwi.vocabulary.crawler.component.producer.base.AbstractProducer;
import me.fengorz.kiwi.vocabulary.crawler.component.producer.base.MQProducer;
import me.fengorz.kiwi.vocabulary.crawler.component.producer.base.MQSender;
import me.fengorz.kiwi.word.api.common.WordCrawlerConstants;
import me.fengorz.kiwi.word.api.dto.queue.RemoveMqDTO;
import me.fengorz.kiwi.word.api.entity.FetchQueueDO;
import me.fengorz.kiwi.word.api.feign.IBizAPI;

/**
 * 老旧单词数据清除--消息队列生产者 @Author zhanshifeng @Date 2019/10/30 10:33 AM
 */
@Component
@Slf4j
public class RemoveWordProducer extends AbstractProducer implements MQProducer {

    public RemoveWordProducer(IBizAPI bizAPI, MQSender MQSender) {
        super(bizAPI, MQSender);
        this.infoType = WordCrawlerConstants.QUEUE_INFO_TYPE_WORD;
    }

    @Override
    public void produce() {
        super.produce(WordCrawlerConstants.STATUS_TO_DEL_BASE);
    }

    @Override
    @Async
    public void execute(FetchQueueDO queue) {
        queue.setIsLock(GlobalConstants.FLAG_YES);
        queue.setFetchStatus(WordCrawlerConstants.STATUS_DOING_DEL_BASE);
        if (Optional.of(bizAPI.updateQueueById(queue)).get().isSuccess()) {
            MQSender.removeWord(new RemoveMqDTO().setQueueId(queue.getQueueId()));
        }
    }
}
