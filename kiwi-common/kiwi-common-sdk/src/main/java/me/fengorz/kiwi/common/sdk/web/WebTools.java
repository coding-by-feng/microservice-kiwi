/*
 *
 *   Copyright [2019~2025] [codingByFeng]
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

package me.fengorz.kiwi.common.sdk.web;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.StrUtil;
import me.fengorz.kiwi.common.api.constant.SecurityConstants;
import me.fengorz.kiwi.common.api.exception.AuthException;
import org.apache.commons.lang3.CharEncoding;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.util.WebUtils;

import java.nio.charset.StandardCharsets;

/**
 * s
 *
 * @Author codingByFeng
 * @Date 2019-09-07 21:11
 */
public class WebTools extends WebUtils {

    /**
     * 从request获取Authorization并解密
     *
     * @param request
     * @return
     */
    public static String decodeAuthorization(ServerHttpRequest request) {
        String authorization = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (!StrUtil.startWith(authorization, SecurityConstants.KEY_HEADER_BASIC_)) {
            throw new AuthException("请求头中client信息不能为空");
        }

        byte[] decoded;

        try {
            byte[] authorizationBytes = authorization.substring(6).getBytes(CharEncoding.UTF_8);
            decoded = Base64.decode(authorizationBytes);
        } catch (Exception e) {
            throw new AuthException("Failed to decode basic authentication token");
        }

        return new String(decoded, StandardCharsets.UTF_8);
    }

}
