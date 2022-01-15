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

package me.fengorz.kiwi.common.es.config;

import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.RestClients;
import org.springframework.data.elasticsearch.config.AbstractElasticsearchConfiguration;

/** @Author zhanshifeng @Date 2020/11/16 8:35 PM */
@Configuration
public class ESConfig extends AbstractElasticsearchConfiguration {

  static {
    System.setProperty("es.set.netty.runtime.available.processors", "false");
  }

  @Value("${spring.elasticsearch.rest.uris}")
  private String hostAndPort;

  @Override
  @Bean
  public RestHighLevelClient elasticsearchClient() {

    System.out.println("----------------------->>>>>>>>>>>>>>>>>>>>>" + hostAndPort);

    final ClientConfiguration clientConfiguration =
        ClientConfiguration.builder().connectedTo(hostAndPort).build();

    return RestClients.create(clientConfiguration).rest();
  }
}
