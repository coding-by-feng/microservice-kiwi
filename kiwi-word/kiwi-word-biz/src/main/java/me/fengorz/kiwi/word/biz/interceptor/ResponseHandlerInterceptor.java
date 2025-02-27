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

package me.fengorz.kiwi.word.biz.interceptor;

import cn.hutool.http.Header;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.word.api.common.WordConstants;
import me.fengorz.kiwi.word.biz.property.CacheControlApiProperties;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.CacheControl;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.TimeUnit;

/**
 * @Description TODO
 * @Author Kason Zhan
 * @Date 2022/9/25 09:03
 */
@Slf4j
@Component(WordConstants.BEAN_NAMES.RESPONSE_HANDLER_INTERCEPTOR)
@RequiredArgsConstructor
public class ResponseHandlerInterceptor implements HandlerInterceptor {

    private final CacheControlApiProperties cacheControlApiProperties;

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
        ModelAndView modelAndView) throws Exception {
        if (StringUtils.startsWithAny(request.getRequestURI(),
            cacheControlApiProperties.getNeedCacheApi().toArray(new String[0]))) {
            response.setHeader(Header.CACHE_CONTROL.toString(),
                CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic().toString());
            response.setHeader("test", "test");
        }
        response.getHeaderNames().forEach(headerName -> {
            log.info("header name is {}, value is {}", headerName, response.getHeaders(headerName));
        });
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        HandlerInterceptor.super.afterCompletion(request, response, handler, ex);
    }
}
