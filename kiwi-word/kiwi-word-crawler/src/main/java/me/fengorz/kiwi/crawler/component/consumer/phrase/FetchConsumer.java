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

package me.fengorz.kiwi.crawler.component.consumer.phrase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.crawler.component.consumer.base.AbstractConsumer;
import me.fengorz.kiwi.crawler.component.consumer.base.IConsumer;
import me.fengorz.kiwi.crawler.config.properties.CrawlerConfigProperties;
import me.fengorz.kiwi.crawler.service.FetchService;
import me.fengorz.kiwi.word.api.dto.queue.FetchPhraseMqDTO;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

/**
 * @Author Kason Zhan @Date 2019/10/28 4:25 PM
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RabbitListener(bindings = @QueueBinding(
    value = @Queue(value = "${mq.config.phraseFromCambridge.fetchQueue}", autoDelete = "false"),
    exchange = @Exchange(value = "${mq.config.phraseFromCambridge.exchange}"),
    key = "${mq.config.phraseFromCambridge.fetchRouting}"))
public class FetchConsumer extends AbstractConsumer<FetchPhraseMqDTO> implements IConsumer<FetchPhraseMqDTO> {

    private final FetchService fetchService;
    private final CrawlerConfigProperties crawlerConfigProperties;

    @Resource(name = "fetchWordThreadExecutor")
    private ThreadPoolTaskExecutor fetchWordThreadExecutor;

    @PostConstruct
    private void init() {
        super.taskExecutor = this.fetchWordThreadExecutor;
        super.maxPoolSize = this.crawlerConfigProperties.getMaxPoolSize();
        super.startWorkLog = "rabbitMQ fetch one phrase is 【{}】";
    }

    @Override
    @RabbitHandler
    public void consume(FetchPhraseMqDTO dto) {
        super.work(dto);
    }

    @Override
    protected void execute(FetchPhraseMqDTO dto) {
        fetchService.handle(dto);
    }

    @Override
    protected void errorCallback(FetchPhraseMqDTO dto, Exception e) {
        // TODO ZSF 增加一个抓取队列状态恢复到待抓取的接口，防止数据抓取丢失
    }
}
