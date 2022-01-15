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

package me.fengorz.kiwi.vocabulary.crawler.config.properties;

import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

/** @Author zhanshifeng @Date 2020/10/15 8:37 PM */
@Data
@Configuration
@RefreshScope
@ConditionalOnExpression("!'${mq.config}'.isEmpty()")
@ConfigurationProperties(prefix = "mq.config")
public class MqProperties {

  private MqExchange wordFromCambridge;
  private MqExchange phraseRunUpFromCambridge;
  private MqExchange phraseFromCambridge;
  private MqExchange pronunciationFromCambridge;
}
