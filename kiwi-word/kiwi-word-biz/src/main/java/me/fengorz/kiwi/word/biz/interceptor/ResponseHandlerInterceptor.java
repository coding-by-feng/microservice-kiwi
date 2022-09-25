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

import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.CacheControl;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import cn.hutool.http.Header;
import lombok.extern.slf4j.Slf4j;

/**
 * @Description TODO
 * @Author zhanshifeng
 * @Date 2022/9/25 09:03
 */
@Slf4j
// @Component(WordConstants.BEAN_NAMES.RESPONSE_HANDLER_INTERCEPTOR)
public class ResponseHandlerInterceptor implements HandlerInterceptor {

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
        ModelAndView modelAndView) throws Exception {
        response.addHeader(Header.CACHE_CONTROL.toString(),
            CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic().toString());
        response.getHeaderNames().forEach(headerName -> {
            log.info("header name is {}, value is {}", headerName, response.getHeaders(headerName));
        });
    }

}
