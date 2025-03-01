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

package me.fengorz.kiwi.crawler.component.producer.phrase.async;

import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.sdk.constant.GlobalConstants;
import me.fengorz.kiwi.crawler.component.producer.base.AbstractProducer;
import me.fengorz.kiwi.crawler.component.producer.base.MqProducer;
import me.fengorz.kiwi.crawler.component.producer.base.MqSender;
import me.fengorz.kiwi.word.api.common.ApiCrawlerConstants;
import me.fengorz.kiwi.word.api.common.enumeration.WordTypeEnum;
import me.fengorz.kiwi.word.api.dto.queue.RemoveMqDTO;
import me.fengorz.kiwi.word.api.entity.FetchQueueDO;
import me.fengorz.kiwi.word.api.feign.DictFetchApi;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 老旧单词数据清除--消息队列生产者 @Author Kason Zhan @Date 2019/10/30 10:33 AM
 */
@Component
@Slf4j
public class RemovePhraseProducer extends AbstractProducer implements MqProducer {

    public RemovePhraseProducer(DictFetchApi dictFetchApi, MqSender MQSender) {
        super(dictFetchApi, MQSender);
        this.infoType = WordTypeEnum.PHRASE.getType();
    }

    @Override
    public void produce() {
        log.info("RemovePhraseProducer produce method is starting");
        super.produce(ApiCrawlerConstants.STATUS_TO_DEL_BASE);
        log.info("RemovePhraseProducer produce method has ended");

    }

    @Override
    @Async
    public void execute(FetchQueueDO queue) {
        queue.setIsLock(GlobalConstants.FLAG_YES);
        queue.setFetchStatus(ApiCrawlerConstants.STATUS_DOING_DEL_BASE);
        if (Optional.of(dictFetchApi.updateQueueById(queue)).get().isSuccess()) {
            mqSender.removePhrase(new RemoveMqDTO().setQueueId(queue.getQueueId()));
        }
    }
}
