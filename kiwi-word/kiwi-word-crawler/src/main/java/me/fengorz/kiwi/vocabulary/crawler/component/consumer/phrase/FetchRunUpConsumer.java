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

package me.fengorz.kiwi.vocabulary.crawler.component.consumer.phrase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.vocabulary.crawler.component.consumer.base.AbstractConsumer;
import me.fengorz.kiwi.vocabulary.crawler.component.consumer.base.IConsumer;
import me.fengorz.kiwi.vocabulary.crawler.service.IFetchService;
import me.fengorz.kiwi.word.api.dto.queue.FetchPhraseRunUpMqDTO;
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
@RabbitListener(bindings = @QueueBinding(value = @Queue(value = "${mq.config.phrase.fetch.queue.runUp}", autoDelete = "true"),
        exchange = @Exchange(value = "${mq.config.phrase.fetch.exchange}"),
        key = "${mq.config.phrase.fetch.routing.cambridge.runUp}"))
public class FetchRunUpConsumer extends AbstractConsumer<FetchPhraseRunUpMqDTO> implements IConsumer<FetchPhraseRunUpMqDTO> {

    private final IFetchService fetchService;

    @Resource(name = "fetchWordThreadExecutor")
    private ThreadPoolTaskExecutor fetchWordThreadExecutor;

    @Value("${crawler.config.max.pool.size}")
    private int maxPoolSize;

    @PostConstruct
    private void init() {
        super.taskExecutor = this.fetchWordThreadExecutor;
        super.maxPoolSize = this.maxPoolSize;
        super.startWorkLog = "rabbitMQ fetch one run-up of phrase is 【{}】";
    }

    @Override
    @RabbitHandler
    public void consume(FetchPhraseRunUpMqDTO dto) {
        super.work(dto);
    }

    @Override
    protected void execute(FetchPhraseRunUpMqDTO dto) {
        fetchService.retrievePhrase(dto);
    }

    @Override
    protected void errorCallback(FetchPhraseRunUpMqDTO dto) {
        // TODO ZSF 增加一个抓取队列状态恢复到待抓取的接口，防止数据抓取丢失
    }

}
