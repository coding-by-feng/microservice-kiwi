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

package me.fengorz.kiwi.word.biz.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.bdf.cache.redis.config.CacheConfig;
import me.fengorz.kiwi.bdf.core.config.CoreConfig;
import me.fengorz.kiwi.bdf.core.config.LogAspectConfig;
import me.fengorz.kiwi.common.es.config.ESConfig;
import me.fengorz.kiwi.common.fastdfs.config.DfsConfig;
import me.fengorz.kiwi.common.sdk.config.UtilsBeanConfiguration;

/**
 * @Author zhanshifeng
 * @Date 2019/10/30 3:45 PM
 */
@Slf4j
@Configuration
@Import({CoreConfig.class, UtilsBeanConfiguration.class, LogAspectConfig.class, CacheConfig.class, DfsConfig.class,
    ESConfig.class})
public class WordBizConfig {
    public WordBizConfig() {
        log.info("WordBizConfig...");
    }
}
