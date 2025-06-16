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

package me.fengorz.kiwi.auth.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.auth.service.EnhancedRemoteTokenServices;
import me.fengorz.kiwi.auth.service.GoogleTokenCacheService;
import me.fengorz.kiwi.bdf.security.component.KiwiUserAuthenticationConverter;
import me.fengorz.kiwi.common.sdk.config.FilterIgnorePropertiesConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.provider.token.DefaultAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.web.client.RestTemplate;

/**
 * Enhanced Resource Server Configuration for Google OAuth2 Integration
 *
 * @Author Kason Zhan
 * @Date 2025-06-16
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class GoogleTokenResourceServerConfig {

    private final TokenStore tokenStore;
    private final GoogleTokenCacheService googleTokenCacheService;
    private final FilterIgnorePropertiesConfig filterIgnorePropertiesConfig;
    private final RestTemplate lbRestTemplate;
    private final GoogleOAuth2Properties googleOAuth2Properties;

    /**
     * Enhanced RemoteTokenServices that supports both standard OAuth2 and Google tokens
     */
    @Bean
    @Primary
    public ResourceServerTokenServices resourceServerTokenServices() {
        log.info("Configuring enhanced ResourceServerTokenServices with Google token support");
        
        EnhancedRemoteTokenServices tokenServices = new EnhancedRemoteTokenServices(
            tokenStore, 
            googleTokenCacheService
        );
        
        // Configure for standard OAuth2 token validation
        tokenServices.setCheckTokenEndpointUrl(googleOAuth2Properties.getCheckTokenEndpointUrl());
        tokenServices.setClientId("kiwi-resource-server");
        tokenServices.setClientSecret("kiwi-resource-server-secret");
        tokenServices.setRestTemplate(lbRestTemplate);
        
        // Set up access token converter
        DefaultAccessTokenConverter accessTokenConverter = new DefaultAccessTokenConverter();
        KiwiUserAuthenticationConverter userTokenConverter = 
            new KiwiUserAuthenticationConverter(filterIgnorePropertiesConfig);
        accessTokenConverter.setUserTokenConverter(userTokenConverter);
        tokenServices.setAccessTokenConverter(accessTokenConverter);
        
        log.info("Enhanced ResourceServerTokenServices configured successfully");
        return tokenServices;
    }
}