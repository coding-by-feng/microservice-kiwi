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

package me.fengorz.kiwi.bdf.security.component;

import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.sdk.config.FilterIgnorePropertiesConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.ExpressionUrlAuthorizationConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.token.DefaultAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.RemoteTokenServices;
import org.springframework.security.oauth2.provider.token.UserAuthenticationConverter;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.client.RestTemplate;

/**
 * 1. 支持remoteTokenServices 负载均衡 2. 支持 获取用户全部信息 @Author Kason Zhan
 */
@Slf4j
public class KiwiResourceServerConfigurerAdapter extends ResourceServerConfigurerAdapter {
    @Autowired
    protected ResourceAuthExceptionEntryPoint resourceAuthExceptionEntryPoint;
    @Autowired
    protected RemoteTokenServices remoteTokenServices;
    @Autowired
    private FilterIgnorePropertiesConfig ignorePropertiesConfig;
    @Autowired
    private AccessDeniedHandler pigAccessDeniedHandler;
    @Autowired
    private RestTemplate lbRestTemplate;
    @Autowired
    private FilterIgnorePropertiesConfig filterIgnorePropertiesConfig;

    /**
     * 默认的配置，对外暴露
     *
     * @param httpSecurity
     */
    @Override
    public void configure(HttpSecurity httpSecurity) throws Exception {
        // 允许使用iframe 嵌套，避免swagger-ui 不被加载的问题
        httpSecurity.headers().frameOptions().disable();
        ExpressionUrlAuthorizationConfigurer<HttpSecurity>.ExpressionInterceptUrlRegistry registry =
            httpSecurity.authorizeRequests();
        registry.antMatchers(ignorePropertiesConfig.getUrls().toArray(new String[0])).permitAll().anyRequest()
            .authenticated().and().csrf().disable();
    }

    @Override
    public void configure(ResourceServerSecurityConfigurer resources) {
        DefaultAccessTokenConverter accessTokenConverter = new DefaultAccessTokenConverter();
        UserAuthenticationConverter userTokenConverter =
            new KiwiUserAuthenticationConverter(this.filterIgnorePropertiesConfig);
        accessTokenConverter.setUserTokenConverter(userTokenConverter);

        remoteTokenServices.setRestTemplate(lbRestTemplate);
        remoteTokenServices.setAccessTokenConverter(accessTokenConverter);
        resources.authenticationEntryPoint(resourceAuthExceptionEntryPoint).accessDeniedHandler(pigAccessDeniedHandler)
            .tokenServices(remoteTokenServices);
    }
}
