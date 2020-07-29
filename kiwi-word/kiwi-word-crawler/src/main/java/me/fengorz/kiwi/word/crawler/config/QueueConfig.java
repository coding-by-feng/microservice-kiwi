/*
 *
 * Copyright [2019~2025] [zhanshifeng]
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

package me.fengorz.kiwi.word.crawler.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * @Description TODO
 * @Author zhanshifeng
 * @Date 2019/10/29 4:57 PM
 */
@Configuration
@ComponentScan(basePackages = "me.fengorz.kiwi.word.crawler")
@EnableScheduling
public class QueueConfig {

    @Value("${crawler.config.core.pool.size}")
    private int corePoolSize;

    @Value("${crawler.config.max.pool.size}")
    private int maxPoolSize;

    @Value("${crawler.config.queue.capacity}")
    private int queueCapacity;

    @Bean(name = "fetchWordThreadExecutor")
    public ThreadPoolTaskExecutor fetchWordThreadExecutor() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(this.corePoolSize);
        taskExecutor.setMaxPoolSize(this.maxPoolSize);
        taskExecutor.setQueueCapacity(this.queueCapacity);
        taskExecutor.initialize();
        return taskExecutor;
    }

    @Bean(name = "fetchPronunciationThreadExecutor")
    public ThreadPoolTaskExecutor fetchPronunciationThreadExecutor() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(this.corePoolSize);
        taskExecutor.setMaxPoolSize(this.maxPoolSize);
        taskExecutor.setQueueCapacity(this.queueCapacity);
        taskExecutor.initialize();
        return taskExecutor;
    }

}
