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

package me.fengorz.kason.word.biz.config;

import cn.hutool.http.Header;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kason.common.sdk.constant.GlobalConstants;
import me.fengorz.kason.common.sdk.util.validate.KasonAssertUtils;
import me.fengorz.kason.word.biz.property.CacheControlApiProperties;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.CacheControl;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
@EnableWebSecurity
// @EnableGlobalMethodSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter implements Ordered {

    private final CacheControlApiProperties cacheControlApiProperties;

    public WebSecurityConfig(CacheControlApiProperties cacheControlApiProperties) {
        // default enable: false
        super(false);
        KasonAssertUtils.assertNotEmpty(cacheControlApiProperties, "cacheControlApiProperties must not be empty.");
        this.cacheControlApiProperties = cacheControlApiProperties;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        log.info("Disable default header.");
        http.requestMatcher(request -> StringUtils
                        .startsWithAny(request.getRequestURI(), cacheControlApiProperties.getNeedCacheApi().toArray(new String[0])))
                .headers().cacheControl().disable()
                .addHeaderWriter(new StaticHeadersWriter(Header.CACHE_CONTROL.toString(),
                        CacheControl.maxAge(365, TimeUnit.DAYS).noTransform().cachePublic().getHeaderValue()))
                .addHeaderWriter(new StaticHeadersWriter(GlobalConstants.HEADERS.HEADER_EXPIRES_UPPER_CASE,
                        ZonedDateTime.of(LocalDateTime.now().plusYears(1), ZoneId.of("GMT"))
                                .format(GlobalConstants.HEADERS.HEADER_EXPIRES_TIME_FORMATTER)))
                .and().csrf().disable();
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
