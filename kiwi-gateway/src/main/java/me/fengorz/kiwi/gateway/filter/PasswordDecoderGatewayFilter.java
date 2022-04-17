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

import java.net.URI;
import java.util.HashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.sdk.constant.SecurityConstants;
import me.fengorz.kiwi.common.sdk.util.cipher.KiwiDecodeUtils;
import reactor.core.publisher.Mono;

/**
 * @Author zhanshifeng @Date 2019-09-06 14:24
 */
@Slf4j
@Component
public class PasswordDecoderGatewayFilter extends AbstractGatewayFilterFactory {

    // 这里定义成静态变量，性能比成员变量会更高，由于@Value不支持注入静态变量，所以通过setter注入。
    @Value("${security.encode.key:1234567812345678}")
    private static String encodeKey;

    @Override
    public GatewayFilter apply(Object config) {
        return (exchange, chain) -> {
            ServerHttpRequest httpRequest = exchange.getRequest();

            if (!StrUtil.containsAnyIgnoreCase(httpRequest.getURI().getPath(), SecurityConstants.URL_OAUTH_TOKEN_URL)) {
                return chain.filter(exchange);
            }

            URI uri = exchange.getRequest().getURI();
            String rawQuery = uri.getRawQuery();
            HashMap<String, String> decodeParamMap = HttpUtil.decodeParamMap(rawQuery, CharsetUtil.UTF_8);
            String password = decodeParamMap.get(SecurityConstants.KEY_PASSWORD);
            if (StrUtil.isNotBlank(password)) {
                try {
                    password = KiwiDecodeUtils.decryptAES(password, encodeKey);
                    System.out.println(password);
                } catch (Exception e) {
                    log.error("密码解密失败:{}", password);
                    return Mono.error(e);
                }
                decodeParamMap.put(SecurityConstants.KEY_PASSWORD, password.trim());
            }

            URI newUri =
                UriComponentsBuilder.fromUri(uri).replaceQuery(HttpUtil.toParams(decodeParamMap)).build(true).toUri();

            ServerHttpRequest newRequest = exchange.getRequest().mutate().uri(newUri).build();
            return chain.filter(exchange.mutate().request(newRequest).build());
        };
    }

    @Value("${security.encode.key:1234567812345678}")
    public void setEncodeKey(String encodeKey) {
        PasswordDecoderGatewayFilter.encodeKey = encodeKey;
    }
}
