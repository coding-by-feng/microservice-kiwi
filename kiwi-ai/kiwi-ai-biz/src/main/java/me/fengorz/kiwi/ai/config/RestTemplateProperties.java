package me.fengorz.kiwi.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "rest-template")
public class RestTemplateProperties {

    private HttpClient httpClient = new HttpClient();
    private RequestFactory requestFactory = new RequestFactory();
    private Retry retry = new Retry();

    public static class HttpClient {
        private int maxTotalConnections = 100;
        private int maxConnectionsPerRoute = 20;
        private int validateAfterInactivity = 1000;
        private int connectionTimeToLive = 30;
        private int maxConnTotal = 100;
        private int maxConnPerRoute = 20;

        // Getters and setters
        public int getMaxTotalConnections() {
            return maxTotalConnections;
        }

        public void setMaxTotalConnections(int maxTotalConnections) {
            this.maxTotalConnections = maxTotalConnections;
        }

        public int getMaxConnectionsPerRoute() {
            return maxConnectionsPerRoute;
        }

        public void setMaxConnectionsPerRoute(int maxConnectionsPerRoute) {
            this.maxConnectionsPerRoute = maxConnectionsPerRoute;
        }

        public int getValidateAfterInactivity() {
            return validateAfterInactivity;
        }

        public void setValidateAfterInactivity(int validateAfterInactivity) {
            this.validateAfterInactivity = validateAfterInactivity;
        }

        public int getConnectionTimeToLive() {
            return connectionTimeToLive;
        }

        public void setConnectionTimeToLive(int connectionTimeToLive) {
            this.connectionTimeToLive = connectionTimeToLive;
        }

        public int getMaxConnTotal() {
            return maxConnTotal;
        }

        public void setMaxConnTotal(int maxConnTotal) {
            this.maxConnTotal = maxConnTotal;
        }

        public int getMaxConnPerRoute() {
            return maxConnPerRoute;
        }

        public void setMaxConnPerRoute(int maxConnPerRoute) {
            this.maxConnPerRoute = maxConnPerRoute;
        }
    }

    public static class RequestFactory {
        private int connectTimeout = 5000;
        private int readTimeout = 10000;

        // Getters and setters
        public int getConnectTimeout() {
            return connectTimeout;
        }

        public void setConnectTimeout(int connectTimeout) {
            this.connectTimeout = connectTimeout;
        }

        public int getReadTimeout() {
            return readTimeout;
        }

        public void setReadTimeout(int readTimeout) {
            this.readTimeout = readTimeout;
        }
    }

    public static class Retry {
        private int maxAttempts = 3;
        private List<String> retryableExceptions = Arrays.asList(
                "org.springframework.web.client.HttpServerErrorException",
                "java.net.SocketTimeoutException"
        );
        private int backoffPeriod = 1000;

        // Getters and setters
        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public List<String> getRetryableExceptions() {
            return retryableExceptions;
        }

        public void setRetryableExceptions(List<String> retryableExceptions) {
            this.retryableExceptions = retryableExceptions;
        }

        public int getBackoffPeriod() {
            return backoffPeriod;
        }

        public void setBackoffPeriod(int backoffPeriod) {
            this.backoffPeriod = backoffPeriod;
        }
    }

    // Getters and setters for outer class
    public HttpClient getHttpClient() {
        return httpClient;
    }

    public void setHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public RequestFactory getRequestFactory() {
        return requestFactory;
    }

    public void setRequestFactory(RequestFactory requestFactory) {
        this.requestFactory = requestFactory;
    }

    public Retry getRetry() {
        return retry;
    }

    public void setRetry(Retry retry) {
        this.retry = retry;
    }
}