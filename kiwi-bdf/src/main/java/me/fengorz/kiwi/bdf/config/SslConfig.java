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

package me.fengorz.kiwi.bdf.config;

import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.sdk.config.RestTemplateProperties;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

/**
 * @Author Kason Zhan
 */
@Slf4j
public abstract class SslConfig {

    public SslConfig(RestTemplateProperties restTemplateProperties) {
        log.info("SslConfig...");
        this.restTemplateProperties = restTemplateProperties;
    }

    protected final RestTemplateProperties restTemplateProperties;

    protected RestTemplate sslRestTemplate() throws NoSuchAlgorithmException, KeyManagementException {
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

}
