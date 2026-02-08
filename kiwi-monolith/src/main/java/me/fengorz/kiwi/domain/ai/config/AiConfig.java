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

import lombok.extern.slf4j.Slf4j;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.TimeUnit;

/**
 * AI Configuration with connection pooling for better performance
 *
 * @author codingByFeng
 */
@Slf4j
@Configuration
public class AiConfig {

    /**
     * Shared OkHttpClient with connection pooling for AI API calls.
     * This significantly improves performance by reusing connections.
     */
    @Bean("aiOkHttpClient")
    public OkHttpClient aiOkHttpClient() {
        ConnectionPool connectionPool = new ConnectionPool(
                50,              // max idle connections (increased from 20)
                5, TimeUnit.MINUTES   // keep-alive duration
        );

        OkHttpClient client = new OkHttpClient.Builder()
                .connectionPool(connectionPool)
                .connectTimeout(30, TimeUnit.SECONDS)
                // Increased from 120s to 300s (5min) for long AI streaming responses
                .readTimeout(300, TimeUnit.SECONDS)
                // Increased from 60s to 120s for sending large prompts
                .writeTimeout(120, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();

        log.info("AI OkHttpClient initialized with connection pool: maxIdleConnections=50, keepAlive=5min, readTimeout=300s");
        return client;
    }

    /**
     * RestTemplate with OkHttp3 backend for connection pooling.
     * This replaces SimpleClientHttpRequestFactory which creates new connections per request.
     */
    @Bean("aiRestTemplate")
    public RestTemplate aiRestTemplate(OkHttpClient aiOkHttpClient) {
        OkHttp3ClientHttpRequestFactory factory = new OkHttp3ClientHttpRequestFactory(aiOkHttpClient);
        log.info("AI RestTemplate initialized with OkHttp3 connection pooling");
        return new RestTemplate(factory);
    }

    @Bean("aiRetryTemplate")
    public RetryTemplate aiRetryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();

        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(3);
        retryTemplate.setRetryPolicy(retryPolicy);

        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(1000);
        backOffPolicy.setMultiplier(2.0);
        backOffPolicy.setMaxInterval(10000);
        retryTemplate.setBackOffPolicy(backOffPolicy);

        return retryTemplate;
    }
}
