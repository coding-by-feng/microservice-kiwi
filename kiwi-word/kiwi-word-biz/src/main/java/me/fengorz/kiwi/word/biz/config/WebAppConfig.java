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

package me.fengorz.kiwi.word.biz.config;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.word.api.common.WordConstants;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;
import java.util.List;

/**
 * @Description TODO
 * @Author Kason Zhan
 * @Date 2022/9/25 09:05
 */
@Slf4j
// @Configuration
public class WebAppConfig implements WebMvcConfigurer {

    @Resource(name = WordConstants.BEAN_NAMES.RESPONSE_HANDLER_INTERCEPTOR)
    private HandlerInterceptor responseHandlerInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        log.info("InterceptorRegistry is adding interceptor.");
        registry.addInterceptor(responseHandlerInterceptor).addPathPatterns(PATH_PATTERNS);
    }

    private static final List<String> PATH_PATTERNS =
        Lists.newArrayList("/word/main/query/**", "/word/main/queryById/**", "/word/main/fuzzyQueryList/**",
            "/word/main/variant/**", "/word/paraphrase/star/list/getItemDetail/**",
            "/word/pronunciation/downloadVoice/**", "/word/review/downloadReviewAudio/**");

}
