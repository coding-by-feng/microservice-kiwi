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

package me.fengorz.kiwi.ai.config;

import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.bdf.config.SslConfig;
import me.fengorz.kiwi.common.sdk.config.RestTemplateProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @Author Kason Zhan
 */
@Slf4j
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "youtube.video.batch.enabled", havingValue = "true")
public class BatchConfig extends SslConfig {

    public BatchConfig(RestTemplateProperties restTemplateProperties) {
        super(restTemplateProperties);
        log.info("BatchConfig...");
    }

}