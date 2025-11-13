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

package me.fengorz.kason.auth.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kason.auth.filter.DebugTokenFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Configuration for debug filter registration
 *
 * @Author Kason Zhan
 * @Date 2025-06-16
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class DebugFilterConfig {

    private final DebugTokenFilter debugTokenFilter;

    /**
     * Register debug token filter with high priority
     * This filter will be active only in debug/development mode
     */
    @Bean
    public FilterRegistrationBean<DebugTokenFilter> debugTokenFilterRegistration() {
        log.info("Registering DebugTokenFilter for token analysis");
        
        FilterRegistrationBean<DebugTokenFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(debugTokenFilter);
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        registration.setName("debugTokenFilter");
        
        // Only enable in debug mode - you can control this via application properties
        registration.setEnabled(true); // Set to false in production
        
        log.info("DebugTokenFilter registered successfully");
        return registration;
    }
}