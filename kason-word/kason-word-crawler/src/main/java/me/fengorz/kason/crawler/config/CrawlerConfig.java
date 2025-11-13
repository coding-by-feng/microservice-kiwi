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

package me.fengorz.kason.crawler.config;

import lombok.extern.slf4j.Slf4j;
import me.fengorz.kason.bdf.config.CoreConfig;
import me.fengorz.kason.common.cache.redis.config.CacheConfig;
import me.fengorz.kason.common.db.config.DbConfig;
import me.fengorz.kason.common.dfs.DfsConfig;
import me.fengorz.kason.common.mq.rabbitmq.config.CommonMQConfig;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @Author Kason Zhan
 * @Date 2019/10/30 3:45 PM
 */
@Slf4j
@Configuration
@ComponentScan("me.fengorz.kason.crawler.**")
@Import({CoreConfig.class, DbConfig.class, CacheConfig.class,DfsConfig.class, EnablerAspectConfig.class,
        CommonMQConfig.class})
public class CrawlerConfig {

    public CrawlerConfig() {
        log.info("CrawlerConfig is initializing.");
    }
}
