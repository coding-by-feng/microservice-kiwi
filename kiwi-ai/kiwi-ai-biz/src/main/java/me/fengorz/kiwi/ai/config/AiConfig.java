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
import me.fengorz.kiwi.bdf.config.CoreConfig;
import me.fengorz.kiwi.common.db.config.DbConfig;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.redisson.spring.cache.CacheConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.security.web.firewall.DefaultHttpFirewall;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UrlPathHelper;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @Author Kason Zhan
 */
@Slf4j
@Configuration
@ComponentScan("me.fengorz.kiwi.ai.**")
@Import({CoreConfig.class, DbConfig.class, CacheConfig.class})
public class AiConfig {

    public AiConfig(RestTemplateProperties restTemplateProperties) {
        log.info("AiConfig...");
        this.restTemplateProperties = restTemplateProperties;
    }

    private final RestTemplateProperties restTemplateProperties;

    @Bean(name = "aiRestTemplate")
    public RestTemplate aiRestTemplate() throws NoSuchAlgorithmException, KeyManagementException {
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(restTemplateProperties.getHttpClient().getMaxTotalConnections());
        connectionManager.setDefaultMaxPerRoute(restTemplateProperties.getHttpClient().getMaxConnectionsPerRoute());
        connectionManager.setValidateAfterInactivity(restTemplateProperties.getHttpClient().getValidateAfterInactivity());

        // Create SSL context that trusts all certificates
        // Create a custom TrustManager that trusts all certificates
        TrustManager[] trustAllCerts = buildTrustAllCerts();
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustAllCerts, new SecureRandom());
        // Create SSL socket factory with no hostname verification
        SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(
                sslContext,
                NoopHostnameVerifier.INSTANCE
        );

        CloseableHttpClient httpClient = HttpClients.custom()
                .setSSLSocketFactory(sslSocketFactory)
                .setConnectionManager(connectionManager)
                .setConnectionTimeToLive(restTemplateProperties.getHttpClient().getConnectionTimeToLive(), TimeUnit.SECONDS)
                .setMaxConnTotal(restTemplateProperties.getHttpClient().getMaxConnTotal())
                .setMaxConnPerRoute(restTemplateProperties.getHttpClient().getMaxConnPerRoute())
                .build();

        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
        requestFactory.setConnectTimeout(restTemplateProperties.getRequestFactory().getConnectTimeout());
        requestFactory.setReadTimeout(restTemplateProperties.getRequestFactory().getReadTimeout());

        return new RestTemplate(requestFactory);
    }

    @Bean
    public UrlPathHelper urlPathHelper() {
        UrlPathHelper helper = new UrlPathHelper();
        helper.setUrlDecode(false); // Prevent double-decoding issues
        helper.setRemoveSemicolonContent(false);
        return helper;
    }

    @Bean
    public HttpFirewall allowUrlEncodedSlashHttpFirewall() {
        DefaultHttpFirewall firewall = new DefaultHttpFirewall();
        firewall.setAllowUrlEncodedSlash(true);
        return firewall;
    }

    private static TrustManager[] buildTrustAllCerts() {
        return new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType) {
                        // Trust all server certificates
                    }

                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType) {

                    }

                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                }
        };
    }

    @Bean
    @SuppressWarnings("unchecked")
    public RetryTemplate retryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();

        Map<Class<? extends Throwable>, Boolean> retryableExceptions = new HashMap<>();
        for (String exceptionClassName : restTemplateProperties.getRetry().getRetryableExceptions()) {
            try {
                Class<?> clazz = Class.forName(exceptionClassName);
                if (Throwable.class.isAssignableFrom(clazz)) {
                    retryableExceptions.put((Class<? extends Throwable>) clazz, Boolean.TRUE);
                }
            } catch (ClassNotFoundException e) {
                // Log or handle the exception (e.g., skip or throw a runtime exception)
                throw new IllegalStateException("Could not load exception class: " + exceptionClassName, e);
            }
        }

        // Configure SimpleRetryPolicy with max attempts and retryable exceptions
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(
                restTemplateProperties.getRetry().getMaxAttempts(),
                retryableExceptions
        );
        retryTemplate.setRetryPolicy(retryPolicy);

        FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
        backOffPolicy.setBackOffPeriod(restTemplateProperties.getRetry().getBackoffPeriod());
        retryTemplate.setBackOffPolicy(backOffPolicy);

        return retryTemplate;
    }

}
