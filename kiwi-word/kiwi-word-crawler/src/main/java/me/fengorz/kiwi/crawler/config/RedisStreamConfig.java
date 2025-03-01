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

package me.fengorz.kiwi.crawler.config;

import me.fengorz.kiwi.crawler.redismq.ListenerMessage;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.Subscription;

import java.time.Duration;

/**
 * @Description Redis MQ configuration.
 * @Author Kason Zhan
 * @Date 2022/8/28 10:40
 */
// @Component
public class RedisStreamConfig {

    private ListenerMessage streamListener;

    // @Bean
    public Subscription subscription(RedisConnectionFactory factory) {
        StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String,
            MapRecord<String, String, String>> build =
                StreamMessageListenerContainer.StreamMessageListenerContainerOptions.builder()
                    .pollTimeout(Duration.ofSeconds(1)).build();
        StreamMessageListenerContainer<String, MapRecord<String, String, String>> listenerContainer =
            StreamMessageListenerContainer.create(factory, build);
        Subscription subscription = listenerContainer.receiveAutoAck(Consumer.from("kiwi", "test"),
            StreamOffset.create("kiwi", ReadOffset.lastConsumed()), streamListener);
        listenerContainer.start();
        return subscription;
    }

}
