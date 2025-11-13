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

package me.fengorz.kason.gateway.filter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * @Description TODO
 * @Author Kason Zhan
 * @Date 2022/9/25 20:36
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GenericHeaderGlobalFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        log.info("path is: {}", path);

        ServerHttpResponse response = exchange.getResponse();
        response.getHeaders().forEach((name, value) -> log.info("Header name: {}, value: {}", name, value));

        // if (StringUtils.startsWithAny(path, cacheControlApiProperties.getApi().toArray(new String[0]))) {
        // ServerHttpResponse response = exchange.getResponse();
        // log.info("Enabled cache-control header.");
        // response.getHeaders().remove(Header.CACHE_CONTROL.toString());
        // response.getHeaders().remove(Header.PRAGMA.toString());
        // response.getHeaders().remove(GlobalConstants.HEADERS.HEADER_EXPIRES_UPPER_CASE);
        // response.getHeaders().remove(GlobalConstants.HEADERS.HEADER_EXPIRES_LOWER_CASE);
        // response.getHeaders().add(Header.CACHE_CONTROL.toString(),
        // CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic().toString());
        //
        // return chain.filter(exchange.mutate().response(response).build());
        // }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

}
