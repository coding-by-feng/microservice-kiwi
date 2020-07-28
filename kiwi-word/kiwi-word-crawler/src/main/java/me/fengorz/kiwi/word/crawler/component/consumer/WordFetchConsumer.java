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

package me.fengorz.kiwi.word.crawler.component.consumer;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.Resource;

import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.word.api.dto.queue.WordFetchMessageDTO;
import me.fengorz.kiwi.word.crawler.service.IWordFetchService;

/**
 * @Description TODO
 * @Author zhanshifeng
 * @Date 2019/10/28 4:25 PM
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RabbitListener(bindings = @QueueBinding(value = @Queue(value = "${mq.config.word.fetch.queue}", autoDelete = "true"),
    exchange = @Exchange(value = "${mq.config.word.fetch.exchange}"),
    key = "${mq.config.word.fetch.routing.cambridge}"))
public class WordFetchConsumer implements IConsumer<WordFetchMessageDTO> {

    private final IWordFetchService wordFetchService;

    @Resource(name = "concurrentFetchWordThreadExecutor")
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;

    @Value("${crawler.config.max.pool.size}")
    private int maxPoolSize;

    private ReentrantLock lock = new ReentrantLock();

    @Override
    @RabbitHandler
    public void consume(WordFetchMessageDTO wordFetchMessageDTO) {
        this.lock.lock();
        try {
            log.info("rabbitMQ fetch one word is " + wordFetchMessageDTO);
            // 线程池如果满了的话，先睡眠一段时间，等待有空闲的现场出来
            while (threadPoolTaskExecutor.getActiveCount() == maxPoolSize) {
                try {
                    log.info("threadPoolTaskExecutor is full, sleep 1s!");
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    log.error("WordFetchConsumer.fetch sleep error!", e);
                    // TODO ZSF 增加一个抓取队列状态恢复到待抓取的接口，防止数据抓取丢失
                    return;
                }
            }

            while (true) {
                try {
                    threadPoolTaskExecutor.execute(() -> {
                        wordFetchService.handle(wordFetchMessageDTO);
                    });
                    break;
                } catch (RejectedExecutionException e) {
                    try {
                        log.info("threadPoolTaskExecutor is full, sleep 1s!");
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                        log.error("WordFetchConsumer.fetch sleep error!", ie);
                        return;
                    }
                }
            }
        } finally {
            this.lock.unlock();
        }
    }

}
