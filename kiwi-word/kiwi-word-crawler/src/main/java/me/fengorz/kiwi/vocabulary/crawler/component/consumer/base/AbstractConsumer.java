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

package me.fengorz.kiwi.vocabulary.crawler.component.consumer.base;

import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.word.api.dto.queue.MqDTO;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Objects;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
* @Author zhanshifeng
 * @Date 2020/7/29 4:34 PM
 */
@Slf4j
public abstract class AbstractConsumer<T extends MqDTO> {

    protected ReentrantLock lock = new ReentrantLock();
    protected ThreadPoolTaskExecutor threadPoolTaskExecutor;
    protected Integer maxPoolSize;
    protected String startWorkLog;

    protected void work(T dto) {

        Objects.requireNonNull(threadPoolTaskExecutor);
        Objects.requireNonNull(maxPoolSize);
        Objects.requireNonNull(startWorkLog);

        this.lock.lock();
        try {
            log.info(startWorkLog, dto);
            // 线程池如果满了的话，先睡眠一段时间，等待有空闲的现场出来
            while (threadPoolTaskExecutor.getActiveCount() == maxPoolSize) {
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    log.error("threadPoolTaskExecutor sleep error!", e);
                    this.errorCallback(dto);
                    return;
                }
            }

            while (true) {
                try {
                    threadPoolTaskExecutor.execute(() -> {
                        this.execute(dto);
                    });
                    break;
                } catch (RejectedExecutionException e) {
                    try {
                        TimeUnit.SECONDS.sleep(1);
                    } catch (InterruptedException ie) {
                        log.error("threadPoolTaskExecutor sleep error!", ie);
                        this.errorCallback(dto);
                        return;
                    }
                }
            }
        } finally {
            this.lock.unlock();
        }
    }

    protected abstract void execute(T dto);

    protected abstract void errorCallback(T dto);

}
