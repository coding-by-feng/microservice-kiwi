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

package me.fengorz.kiwi.vocabulary.crawler.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import lombok.RequiredArgsConstructor;
import me.fengorz.kiwi.common.rabbitmq.config.CommonMQConfig;
import me.fengorz.kiwi.vocabulary.crawler.config.properties.MqProperties;

/**
 * @Author zhanshifeng @Date 2019/10/26 7:56 PM
 */
@Configuration
@Import({CommonMQConfig.class})
@RequiredArgsConstructor
public class RabbitMQConfig {

    private final MqProperties properties;

    private DirectExchange wordFromCambridge() {
        return new DirectExchange(properties.getWordFromCambridge().getExchange());
    }

    private DirectExchange phraseRunUpFromCambridge() {
        return new DirectExchange(properties.getPhraseRunUpFromCambridge().getExchange());
    }

    private DirectExchange phraseFromCambridge() {
        return new DirectExchange(properties.getPhraseFromCambridge().getExchange());
    }

    private DirectExchange pronunciationFromCambridge() {
        return new DirectExchange(properties.getPronunciationFromCambridge().getExchange());
    }

    @Bean
    public Binding wordFetch() {
        return BindingBuilder.bind(new Queue(properties.getWordFromCambridge().getFetchQueue())).to(wordFromCambridge())
            .with(properties.getWordFromCambridge().getFetchRouting());
    }

    @Bean
    public Binding wordRemove() {
        return BindingBuilder.bind(new Queue(properties.getWordFromCambridge().getRemoveQueue()))
            .to(wordFromCambridge()).with(properties.getWordFromCambridge().getRemoveRouting());
    }

    @Bean
    public Binding phraseRunUp() {
        return BindingBuilder.bind(new Queue(properties.getPhraseRunUpFromCambridge().getFetchQueue()))
            .to(phraseFromCambridge()).with(properties.getPhraseRunUpFromCambridge().getFetchRouting());
    }

    @Bean
    public Binding phraseFetch() {
        return BindingBuilder.bind(new Queue(properties.getPhraseFromCambridge().getFetchQueue()))
            .to(phraseRunUpFromCambridge()).with(properties.getPhraseFromCambridge().getFetchRouting());
    }

    @Bean
    public Binding phraseRemove() {
        return BindingBuilder.bind(new Queue(properties.getPhraseFromCambridge().getRemoveQueue()))
            .to(phraseFromCambridge()).with(properties.getPhraseFromCambridge().getRemoveRouting());
    }

    @Bean
    public Binding pronunciationFetch() {
        return BindingBuilder.bind(new Queue(properties.getPronunciationFromCambridge().getFetchQueue()))
            .to(pronunciationFromCambridge()).with(properties.getPronunciationFromCambridge().getFetchRouting());
    }

    @Bean
    public Binding pronunciationRemove() {
        return BindingBuilder.bind(new Queue(properties.getPronunciationFromCambridge().getRemoveQueue()))
            .to(pronunciationFromCambridge()).with(properties.getPronunciationFromCambridge().getRemoveRouting());
    }
}
