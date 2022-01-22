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

package me.fengorz.kiwi.gateway.filter;

import me.fengorz.kiwi.common.sdk.constant.GlobalConstants;
import me.fengorz.kiwi.common.sdk.constant.SecurityConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.stream.Collectors;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.addOriginalRequestUrl;

/**
 * @Author zhanshifeng @Date 2019-09-06 14:54
 */
@Component
public class GenericRequestGlobalFilter implements GlobalFilter, Ordered {

    private static Long skipUrlSlashCount;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 1. 清洗请求头中from 参数
        ServerHttpRequest request =
                exchange
                        .getRequest()
                        .mutate()
                        .headers(httpHeaders -> httpHeaders.remove(SecurityConstants.KEY_HEADER_FROM))
                        .build();

        // 2. 重写StripPrefix
        addOriginalRequestUrl(exchange, request.getURI());
        String rawPath = request.getURI().getRawPath();
        String newPath =
                GlobalConstants.SYMBOL_FORWARD_SLASH
                        + Arrays.stream(
                        StringUtils.tokenizeToStringArray(
                                rawPath, GlobalConstants.SYMBOL_FORWARD_SLASH))
                        .skip(skipUrlSlashCount)
                        .collect(Collectors.joining(GlobalConstants.SYMBOL_FORWARD_SLASH));
        ServerHttpRequest newRequest = request.mutate().path(newPath).build();
        exchange.getAttributes().put(GATEWAY_REQUEST_URL_ATTR, newRequest.getURI());

        return chain.filter(exchange.mutate().request(newRequest.mutate().build()).build());
    }

    @Override
    public int getOrder() {
        return -1000;
    }

    @Value("${security.skip-url-slash-count:1}")
    public void setSkipUrlSlashCount(Long skipUrlSlashCount) {
        GenericRequestGlobalFilter.skipUrlSlashCount = skipUrlSlashCount;
    }
}
