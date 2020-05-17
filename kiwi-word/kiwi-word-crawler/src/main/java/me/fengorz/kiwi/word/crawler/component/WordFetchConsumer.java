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

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.word.api.dto.fetch.WordMessageDTO;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.stereotype.Component;

/**
 * @Description TODO
 * @Author codingByFeng
 * @Date 2019/10/28 4:25 PM
 */
@Component
@RabbitListener(bindings = @QueueBinding(value = @Queue(value = "${mq.config.wordFetch.queue.name}",
        autoDelete = "true"),
        exchange = @Exchange(value = "${mq.config.wordFetch.exchange}", type = ExchangeTypes.DIRECT),
        key = "${mq.config.wordFetch.routing.key}"))
@Slf4j
@AllArgsConstructor
public class WordFetchConsumer {

    // private final IJsoupService jsoupService;
    // private final IRemoteWordFetchService remoteWordFetchService;
    private final AsyncConcurrentConsumer asyncConcurrentConsumer;

    @RabbitHandler
    public void fetch(WordMessageDTO wordMessage) throws InterruptedException {
        log.info("rabbitMQ fetch one word is " + wordMessage);
        this.asyncConcurrentConsumer.asyncFetchWord(wordMessage);
        Thread.sleep(1500);
    }

}
