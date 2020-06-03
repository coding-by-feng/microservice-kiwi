/*
 *
 *   Copyright [2019~2025] [zhanshifeng]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 *
 */

package me.fengorz.kiwi.gateway.filter;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.api.R;
import me.fengorz.kiwi.common.api.constant.SecurityConstants;
import me.fengorz.kiwi.common.api.exception.AuthException;
import me.fengorz.kiwi.common.sdk.config.FilterIgnorePropertiesConfig;
import me.fengorz.kiwi.common.sdk.web.WebTools;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * @Author zhanshifeng
 * @Date 2019-09-06 16:52
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ValidateCodeGatewayFilter extends AbstractGatewayFilterFactory {

    private final ObjectMapper objectMapper;
    private final RedisTemplate redisTemplate;
    private final FilterIgnorePropertiesConfig filterIgnorePropertiesConfig;

    @Override
    public GatewayFilter apply(Object config) {
        return (exchange, chain) -> {

            ServerHttpRequest httpRequest = exchange.getRequest();

            // 非登录请求pass
            if (!StrUtil.containsAnyIgnoreCase(httpRequest.getURI().getPath(), SecurityConstants.URL_OAUTH_TOKEN_URL)) {
                return chain.filter(exchange);
            }

            // 刷新token的请求pass
            String grantType = httpRequest.getQueryParams().getFirst(SecurityConstants.KEY_GRANT_TYPE);
            if (StrUtil.equals(SecurityConstants.REFRESH_TOKEN, grantType)) {
                return chain.filter(exchange);
            }

            try {
                // 部分终端直接放行，不校验验证码
                String authorization = WebTools.decodeAuthorization(httpRequest);
                if (filterIgnorePropertiesConfig.getClients().contains(authorization)) {
                    return chain.filter(exchange);
                }

                checkCode(httpRequest);
            } catch (Exception e) {
                ServerHttpResponse httpResponse = exchange.getResponse();
                httpResponse.setStatusCode(HttpStatus.UNAUTHORIZED);

                try {
                    return httpResponse.writeWith(Mono.just(httpResponse.bufferFactory()
                            .wrap(objectMapper.writeValueAsBytes(R.failed(e.getMessage())))));
                } catch (JsonProcessingException ex) {
                    log.error("httpResponse 流输出异常" , e);
                }
            }

            return chain.filter(exchange);
        };
    }

    private void checkCode(ServerHttpRequest request) throws AuthException {
        String code = request.getQueryParams().getFirst(SecurityConstants.KEY_CODE);

        if (StrUtil.isBlank(code)) {
            throw new AuthException("验证码不能为空");
        }

        // TODO 这个randomStr的作用是？应该是redis存储验证码的key
        String randomStr = request.getQueryParams().getFirst("randomStr");
        if (StrUtil.isBlank(randomStr)) {
            randomStr = request.getQueryParams().getFirst(SecurityConstants.KEY_MOBILE);
        }

        if (StrUtil.isBlank(randomStr)) {
            throw new AuthException("验证码已失效");
        }

        String key = SecurityConstants.DEFAULT_CODE_KEY + randomStr;
        if (!redisTemplate.hasKey(key)) {
            throw new AuthException("验证码已失效");
        }

        Object codeObj = redisTemplate.opsForValue().get(key);
        if (codeObj == null) {
            throw new AuthException("验证码已失效");
        }

        String saveCode = codeObj.toString();
        if (StrUtil.isBlank(saveCode)) {
            redisTemplate.delete(key);
            throw new AuthException("验证码不合法");
        }

        if (!StrUtil.equals(code, saveCode)) {
            redisTemplate.delete(key);
            throw new AuthException("验证码不合法");
        }

        redisTemplate.delete(key);
    }
}
