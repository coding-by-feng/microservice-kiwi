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
import me.fengorz.kiwi.bdf.security.google.GoogleRemoteTokenServices;
import me.fengorz.kiwi.bdf.security.google.GoogleTokenCacheService;
import me.fengorz.kiwi.common.sdk.config.FilterIgnorePropertiesConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.ExpressionUrlAuthorizationConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.token.DefaultAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.client.RestTemplate;

@Slf4j
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
public class KiwiResourceServerConfigurerAdapter extends ResourceServerConfigurerAdapter {

    @Autowired
    protected ResourceAuthExceptionEntryPoint resourceAuthExceptionEntryPoint;

    @Autowired
    private FilterIgnorePropertiesConfig ignorePropertiesConfig;

    @Autowired
    private AccessDeniedHandler accessDeniedHandler;

    @Autowired
    private RestTemplate lbRestTemplate;

    @Autowired
    private FilterIgnorePropertiesConfig filterIgnorePropertiesConfig;

    // Add these dependencies for enhanced token services
    @Autowired
    private TokenStore tokenStore;

    @Autowired
    private GoogleTokenCacheService googleTokenCacheService;

    @Override
    public void configure(HttpSecurity httpSecurity) throws Exception {
        httpSecurity.headers().frameOptions().disable();
        ExpressionUrlAuthorizationConfigurer<HttpSecurity>.ExpressionInterceptUrlRegistry registry =
                httpSecurity.authorizeRequests();
        registry.antMatchers(ignorePropertiesConfig.getUrls().toArray(new String[0])).permitAll().anyRequest()
                .authenticated().and().csrf().disable();
    }

    @Override
    public void configure(ResourceServerSecurityConfigurer resources) {
        // Create Enhanced Remote Token Services instead of default RemoteTokenServices
        GoogleRemoteTokenServices enhancedTokenServices = createEnhancedTokenServices();

        resources.authenticationEntryPoint(resourceAuthExceptionEntryPoint)
                .accessDeniedHandler(accessDeniedHandler)
                .tokenServices(enhancedTokenServices); // Use enhanced services
    }

    /**
     * Create and configure EnhancedRemoteTokenServices
     */
    private GoogleRemoteTokenServices createEnhancedTokenServices() {
        log.info("Creating EnhancedRemoteTokenServices for Resource Server");

        GoogleRemoteTokenServices enhancedServices =
                new GoogleRemoteTokenServices(tokenStore, googleTokenCacheService);

        // Configure for remote token validation
        enhancedServices.setCheckTokenEndpointUrl("http://kiwi-auth:3001/oauth/check_token");
        enhancedServices.setClientId("kiwi-resource-server");
        enhancedServices.setClientSecret("kiwi-resource-server-secret");
        enhancedServices.setRestTemplate(lbRestTemplate);

        // Configure token converter
        DefaultAccessTokenConverter accessTokenConverter = new DefaultAccessTokenConverter();
        KiwiUserAuthenticationConverter userTokenConverter =
                new KiwiUserAuthenticationConverter(filterIgnorePropertiesConfig);
        accessTokenConverter.setUserTokenConverter(userTokenConverter);
        enhancedServices.setAccessTokenConverter(accessTokenConverter);

        log.info("EnhancedRemoteTokenServices configured successfully");
        return enhancedServices;
    }
}