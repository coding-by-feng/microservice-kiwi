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

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.token.TokenEnhancer;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.redis.RedisTokenStore;

import lombok.RequiredArgsConstructor;
import me.fengorz.kiwi.bdf.security.component.KiwiWebResponseExceptionTranslator;
import me.fengorz.kiwi.bdf.security.service.KiwiClientDetailsService;
import me.fengorz.kiwi.common.api.constant.SecurityConstants;
import me.fengorz.kiwi.common.api.entity.EnhancerUser;

/**
 * @Author zhanshifeng
 * @Date 2019-09-23 21:07
 */
@Configuration
@RequiredArgsConstructor
@EnableAuthorizationServer
public class AuthorizationServerConfig extends AuthorizationServerConfigurerAdapter {

    private final DataSource dataSource;
    private final UserDetailsService userDetailsService;
    private final AuthenticationManager authenticationManager;
    private final RedisConnectionFactory redisConnectionFactory;

    @Override
    public void configure(AuthorizationServerSecurityConfigurer configurer) throws Exception {
        configurer.allowFormAuthenticationForClients().checkTokenAccess("permitAll()");
    }

    @Override
    public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
        KiwiClientDetailsService clientDetailsService = new KiwiClientDetailsService(dataSource);
        clientDetailsService.setSelectClientDetailsSql(SecurityConstants.DEFAULT_SELECT_STATEMENT);
        clientDetailsService.setFindClientDetailsSql(SecurityConstants.DEFAULT_FIND_STATEMENT);
        clients.withClientDetails(clientDetailsService);
    }

    @Override
    public void configure(AuthorizationServerEndpointsConfigurer endpoints) throws Exception {
        endpoints.allowedTokenEndpointRequestMethods(HttpMethod.GET, HttpMethod.POST).tokenStore(tokenStore())
            .tokenEnhancer(tokenEnhancer()).userDetailsService(userDetailsService)
            .authenticationManager(authenticationManager).reuseRefreshTokens(false)
            // TODO zhanshifeng 这个API作用什么？
            .exceptionTranslator(new KiwiWebResponseExceptionTranslator());
    }

    @Bean
    public TokenStore tokenStore() {
        RedisTokenStore tokenStore = new RedisTokenStore(redisConnectionFactory);
        tokenStore.setPrefix(SecurityConstants.PROJECT_PREFIX + SecurityConstants.OAUTH_PREFIX);
        return tokenStore;
    }

    @Bean
    public TokenEnhancer tokenEnhancer() {
        return (accessToken, authentication) -> {
            final Map<String, Object> additionalInfo = new HashMap<>(1);
            EnhancerUser enhancerUser = (EnhancerUser)authentication.getPrincipal();
            additionalInfo.put(SecurityConstants.DETAILS_LICENSE, SecurityConstants.PROJECT_LICENSE);
            additionalInfo.put(SecurityConstants.DETAILS_USER_ID, enhancerUser.getId());
            additionalInfo.put(SecurityConstants.DETAILS_USERNAME, enhancerUser.getUsername());
            additionalInfo.put(SecurityConstants.DETAILS_DEPT_ID, enhancerUser.getDeptId());
            ((DefaultOAuth2AccessToken)accessToken).setAdditionalInformation(additionalInfo);
            return accessToken;
        };
    }
}
