/*
 * Copyright [2019~2025] [codingByFeng]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.fengorz.kiwi.domain.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.Serializable;

/**
 * Grok API Properties Configuration
 *
 * @author codingByFeng
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai.grok.api")
public class GrokApiProperties implements Serializable {

    private static final long serialVersionUID = -367572969083407339L;

    private String key;
    private String endpoint;
    private String model;
    private Integer threadPromptsLineSize = 50;
    private Integer threadPoolSize;
    private Integer threadTimeoutSecs = 300;
}
