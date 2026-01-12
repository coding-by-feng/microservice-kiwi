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
package me.fengorz.kiwi.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

/**
 * Filter to log all HTTP request and response details
 *
 * @author codingByFeng
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnProperty(name = "kiwi.logging.http.enabled", havingValue = "true", matchIfMissing = false)
public class RequestResponseLoggingFilter extends OncePerRequestFilter {

    private static final int MAX_PAYLOAD_LENGTH = 10000;

    @Value("${kiwi.logging.http.include-headers:true}")
    private boolean includeHeaders;

    @Value("${kiwi.logging.http.include-body:true}")
    private boolean includeBody;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Skip actuator endpoints to reduce noise
        if (request.getRequestURI().startsWith("/actuator")) {
            filterChain.doFilter(request, response);
            return;
        }

        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        long startTime = System.currentTimeMillis();

        try {
            filterChain.doFilter(requestWrapper, responseWrapper);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            logRequest(requestWrapper);
            logResponse(responseWrapper, duration);
            responseWrapper.copyBodyToResponse();
        }
    }

    private void logRequest(ContentCachingRequestWrapper request) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n").append("=".repeat(80)).append("\n");
        sb.append(">>> REQUEST >>>\n");
        sb.append(String.format("Method: %s\n", request.getMethod()));
        sb.append(String.format("URI: %s\n", request.getRequestURI()));

        String queryString = request.getQueryString();
        if (queryString != null) {
            sb.append(String.format("Query: %s\n", queryString));
        }

        // Log headers
        if (includeHeaders) {
            sb.append("Headers:\n");
            Collections.list(request.getHeaderNames()).forEach(headerName -> {
                // Skip sensitive headers
                if (!headerName.equalsIgnoreCase("Authorization") &&
                    !headerName.equalsIgnoreCase("Cookie")) {
                    sb.append(String.format("  %s: %s\n", headerName, request.getHeader(headerName)));
                } else {
                    sb.append(String.format("  %s: [REDACTED]\n", headerName));
                }
            });
        }

        // Log request body
        if (includeBody) {
            byte[] content = request.getContentAsByteArray();
            if (content.length > 0) {
                String body = getContentAsString(content);
                sb.append(String.format("Body: %s\n", body));
            }
        }

        log.info(sb.toString());
    }

    private void logResponse(ContentCachingResponseWrapper response, long duration) {
        StringBuilder sb = new StringBuilder();
        sb.append("<<< RESPONSE <<<\n");
        sb.append(String.format("Status: %d\n", response.getStatus()));
        sb.append(String.format("Duration: %d ms\n", duration));

        // Log response headers
        if (includeHeaders) {
            sb.append("Headers:\n");
            response.getHeaderNames().forEach(headerName -> {
                sb.append(String.format("  %s: %s\n", headerName, response.getHeader(headerName)));
            });
        }

        // Log response body
        if (includeBody) {
            byte[] content = response.getContentAsByteArray();
            if (content.length > 0) {
                String body = getContentAsString(content);
                sb.append(String.format("Body: %s\n", body));
            }
        }

        sb.append("=".repeat(80));
        log.info(sb.toString());
    }

    private String getContentAsString(byte[] content) {
        if (content.length > MAX_PAYLOAD_LENGTH) {
            return new String(content, 0, MAX_PAYLOAD_LENGTH, StandardCharsets.UTF_8) + "... [TRUNCATED]";
        }
        return new String(content, StandardCharsets.UTF_8);
    }
}
