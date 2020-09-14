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

package me.fengorz.kiwi.word.crawler.component;

import me.fengorz.kiwi.word.api.dto.queue.FetchPronunciationMqDTO;
import me.fengorz.kiwi.word.api.dto.queue.FetchWordMqDTO;
import me.fengorz.kiwi.word.api.dto.queue.RemovePronunciatioinMqDTO;
import me.fengorz.kiwi.word.api.dto.queue.RemoveWordMqDTO;
import me.fengorz.kiwi.word.crawler.component.producer.base.ISender;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
* @Author zhanshifeng
 * @Date 2019/10/28 9:20 AM
 */
@Component
public class MqSender implements ISender {

    @Autowired
    private AmqpTemplate amqpTemplate;

    @Value("${mq.config.word.fetch.exchange}")
    private String wordFetchExchange;

    @Value("${mq.config.word.fetch.routing.cambridge}")
    private String wordFetchRoutingKey;

    @Value("${mq.config.pronunciation.fetch.exchange}")
    private String pronunciationFetchExchange;

    @Value("${mq.config.pronunciation.fetch.routing.cambridge}")
    private String pronunciationFetchRoutingKey;

    @Value("${mq.config.word.remove.exchange}")
    private String wordRemoveExchange;

    @Value("${mq.config.word.remove.routing.cambridge}")
    private String wordRemoveRoutingKey;

    @Value("${mq.config.pronunciation.remove.exchange}")
    private String pronunciationRemoveExchange;

    @Value("${mq.config.pronunciation.remove.routing.cambridge}")
    private String pronunciationRemoveRoutingKey;

    @Override
    public void fetchWord(FetchWordMqDTO dto) {
        this.amqpTemplate.convertAndSend(this.wordFetchExchange, this.wordFetchRoutingKey, dto);
    }

    @Override
    public void fetchPronunciation(FetchPronunciationMqDTO dto) {
        this.amqpTemplate.convertAndSend(this.pronunciationFetchExchange, this.pronunciationFetchRoutingKey, dto);
    }

    @Override
    public void removeWord(RemoveWordMqDTO dto) {
        this.amqpTemplate.convertAndSend(this.wordRemoveExchange, this.wordRemoveRoutingKey, dto);
    }

    @Override
    public void removePronunciation(RemovePronunciatioinMqDTO dto) {
        this.amqpTemplate.convertAndSend(this.pronunciationRemoveExchange, this.pronunciationRemoveRoutingKey, dto);
    }

}
