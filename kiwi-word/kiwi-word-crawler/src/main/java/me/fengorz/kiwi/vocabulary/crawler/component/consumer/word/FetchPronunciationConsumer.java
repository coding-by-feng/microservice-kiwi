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

package me.fengorz.kiwi.vocabulary.crawler.component.consumer.word;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.vocabulary.crawler.component.consumer.base.AbstractConsumer;
import me.fengorz.kiwi.vocabulary.crawler.component.consumer.base.IConsumer;
import me.fengorz.kiwi.vocabulary.crawler.service.IFetchService;
import me.fengorz.kiwi.word.api.dto.queue.FetchPronunciationMqDTO;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

/**
* @Author zhanshifeng
 * @Date 2019/10/28 4:25 PM
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RabbitListener(bindings = @QueueBinding(value = @Queue(value = "${mq.config.pronunciationFromCambridge.fetchQueue}", autoDelete = "true"),
    exchange = @Exchange(value = "${mq.config.pronunciationFromCambridge.exchange}"),
    key = "${mq.config.pronunciationFromCambridge.fetchRouting}"))
public class FetchPronunciationConsumer extends AbstractConsumer<FetchPronunciationMqDTO>
    implements IConsumer<FetchPronunciationMqDTO> {

    private final IFetchService fetchService;

    @Resource(name = "fetchPronunciationThreadExecutor")
    private ThreadPoolTaskExecutor fetchPronunciationThreadExecutor;

    @Value("${crawler.config.max.pool.size}")
    private int maxPoolSize;

    @PostConstruct
    private void init() {
        super.taskExecutor = this.fetchPronunciationThreadExecutor;
        super.maxPoolSize = this.maxPoolSize;
        super.startWorkLog = "rabbitMQ fetch one pronunciation is 【{}】";
    }

    @Override
    @RabbitHandler
    public void consume(FetchPronunciationMqDTO dto) {
        super.work(dto);
    }

    @Override
    protected void execute(FetchPronunciationMqDTO dto) {
        fetchService.handle(dto);
    }

    @Override
    protected void errorCallback(FetchPronunciationMqDTO dto) {

    }
}
