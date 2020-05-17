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

package me.fengorz.kiwi.word.crawler.component;

import me.fengorz.kiwi.word.api.dto.fetch.WordMessageDTO;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @Description TODO
 * @Author codingByFeng
 * @Date 2019/10/28 9:20 AM
 */
@Component
public class WordFetchProducer {

    @Autowired
    private AmqpTemplate amqpTemplate;

    @Value("${mq.config.wordFetch.exchange}")
    private String exchange;

    @Value("${mq.config.wordFetch.routing.key}")
    private String routingKey;

    public void send(WordMessageDTO wordMessage) {
        this.amqpTemplate.convertAndSend(this.exchange, this.routingKey, wordMessage);
    }


}
