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

package me.fengorz.kiwi.common.tts.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.tts.model.BaiduTtsProperties;
import me.fengorz.kiwi.common.tts.model.TtsProperties;

/**
 * @Author zhanshifeng
 * @Date 2019/10/30 3:45 PM
 */
@Slf4j
@Configuration
public class TtsConfig {

    public TtsConfig() {
        log.info("TtsConfig...");
    }

    /**
     * @Bean 会用方法名作为默认的bean name注入到Spring Context.
     * @return
     */
    @Bean
    @ConfigurationProperties(prefix = "tts.voicerss")
    public TtsProperties ttProperties() {
        return new TtsProperties();
    }

    @Bean
    @ConfigurationProperties(prefix = "tts.baidu")
    public BaiduTtsProperties baiduTtsProperties() {
        return new BaiduTtsProperties();
    }

}
