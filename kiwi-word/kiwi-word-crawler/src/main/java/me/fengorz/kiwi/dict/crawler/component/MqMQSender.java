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

package me.fengorz.kiwi.dict.crawler.component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.dict.crawler.component.producer.base.MqSender;
import me.fengorz.kiwi.dict.crawler.config.properties.MqConfigProperties;
import me.fengorz.kiwi.dict.crawler.config.properties.MqExchange;
import me.fengorz.kiwi.word.api.dto.queue.*;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.stereotype.Component;

/**
 * @Author Kason Zhan
 * @Date 2019/10/28 9:20 AM
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MqMQSender implements MqSender {

    private final AmqpTemplate amqpTemplate;
    private final MqConfigProperties properties;

    @Override
    public void fetchWord(FetchWordMqDTO dto) {
        log.info("MQ(fetch word) is sending.");
        MqExchange exchange = properties.getWordFromCambridge();
        amqpTemplate.convertAndSend(exchange.getExchange(), exchange.getFetchRouting(), dto);
    }

    @Override
    public void fetchPhraseRunUp(FetchPhraseRunUpMqDTO dto) {
        MqExchange exchange = properties.getPhraseRunUpFromCambridge();
        amqpTemplate.convertAndSend(exchange.getExchange(), exchange.getFetchRouting(), dto);
    }

    @Override
    public void fetchPhrase(FetchPhraseMqDTO dto) {
        MqExchange exchange = properties.getPhraseFromCambridge();
        amqpTemplate.convertAndSend(exchange.getExchange(), exchange.getFetchRouting(), dto);
    }

    @Override
    public void fetchPronunciation(FetchPronunciationMqDTO dto) {
        MqExchange exchange = properties.getPronunciationFromCambridge();
        amqpTemplate.convertAndSend(exchange.getExchange(), exchange.getFetchRouting(), dto);
    }

    @Override
    public void removeWord(RemoveMqDTO dto) {
        MqExchange exchange = properties.getWordFromCambridge();
        amqpTemplate.convertAndSend(exchange.getExchange(), exchange.getRemoveRouting(), dto);
    }

    @Override
    public void removePhrase(RemoveMqDTO dto) {
        MqExchange exchange = properties.getPhraseFromCambridge();
        amqpTemplate.convertAndSend(exchange.getExchange(), exchange.getRemoveRouting(), dto);
    }

    @Override
    public void removePronunciation(RemovePronunciatioinMqDTO dto) {
        MqExchange exchange = properties.getPronunciationFromCambridge();
        amqpTemplate.convertAndSend(exchange.getExchange(), exchange.getRemoveRouting(), dto);
    }
}
