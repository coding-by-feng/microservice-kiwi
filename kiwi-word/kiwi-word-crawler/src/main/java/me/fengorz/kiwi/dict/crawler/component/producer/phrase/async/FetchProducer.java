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

package me.fengorz.kiwi.dict.crawler.component.producer.phrase.async;

import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.sdk.constant.GlobalConstants;
import me.fengorz.kiwi.dict.crawler.component.producer.base.AbstractProducer;
import me.fengorz.kiwi.dict.crawler.component.producer.base.MqProducer;
import me.fengorz.kiwi.dict.crawler.component.producer.base.MqSender;
import me.fengorz.kiwi.word.api.common.ApiCrawlerConstants;
import me.fengorz.kiwi.word.api.common.enumeration.CrawlerStatusEnum;
import me.fengorz.kiwi.word.api.common.enumeration.WordTypeEnum;
import me.fengorz.kiwi.word.api.dto.queue.FetchPhraseMqDTO;
import me.fengorz.kiwi.word.api.entity.FetchQueueDO;
import me.fengorz.kiwi.word.api.feign.DictFetchApi;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 抓取词组基本信息-消息队列生产者 @Author Kason Zhan @Date 2019/10/30 10:33 AM
 */
@Component
@Slf4j
public class FetchProducer extends AbstractProducer implements MqProducer {

    public FetchProducer(DictFetchApi dictFetchApi, MqSender MQSender) {
        super(dictFetchApi, MQSender);
        this.infoType = WordTypeEnum.PHRASE.getType();
    }

    @Override
    public void produce() {
        super.produce(CrawlerStatusEnum.STATUS_TO_FETCH.getStatus());
    }

    @Async
    @Override
    protected void execute(FetchQueueDO queue) {
        queue.setIsLock(GlobalConstants.FLAG_YES);
        queue.setFetchTime(queue.getFetchTime() + 1);
        if (isCleanUp(queue)) {
            return;
        }
        if (null == queue.getWordId() || 0 == queue.getWordId()) {
            queue.setFetchStatus(ApiCrawlerConstants.STATUS_DOING_FETCH);
            if (Optional.of(dictFetchApi.updateQueueById(queue)).get().isSuccess()) {
                mqSender.fetchPhrase(new FetchPhraseMqDTO().setQueueId(queue.getQueueId())
                    .setPhrase(queue.getWordName()).setDerivation(queue.getDerivation()));
            }
        } else {
            // 删除老的数据
            queue.setFetchStatus(ApiCrawlerConstants.STATUS_TO_DEL_BASE);
            dictFetchApi.updateQueueById(queue);
        }
    }
}
