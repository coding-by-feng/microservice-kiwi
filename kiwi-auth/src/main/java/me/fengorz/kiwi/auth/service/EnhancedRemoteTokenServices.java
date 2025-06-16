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

package me.fengorz.kiwi.auth.service;

import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.auth.dto.GoogleTokenCacheInfo;
import me.fengorz.kiwi.common.api.entity.EnhancerUser;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.token.RemoteTokenServices;
import org.springframework.security.oauth2.provider.token.TokenStore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Enhanced RemoteTokenServices that supports both standard OAuth2 and Google SSO tokens
 *
 * @Author Kason Zhan
 * @Date 2025-06-16
 */
@Slf4j
public class EnhancedRemoteTokenServices extends RemoteTokenServices {

    private final TokenStore tokenStore;
    private final GoogleTokenCacheService googleTokenCacheService;

    public EnhancedRemoteTokenServices(TokenStore tokenStore, GoogleTokenCacheService googleTokenCacheService) {
        this.tokenStore = tokenStore;
        this.googleTokenCacheService = googleTokenCacheService;
    }

    @Override
    public OAuth2Authentication loadAuthentication(String accessToken) throws InvalidTokenException {
        log.debug("Loading authentication for token: {}", accessToken);

        try {
            // First, try to load from local token store (for Google SSO tokens)
            OAuth2Authentication localAuth = tokenStore.readAuthentication(accessToken);
            if (localAuth != null) {
                log.debug("Found token in local token store");
                
                // Check if it's a Google SSO token
                GoogleTokenCacheInfo googleTokenInfo = googleTokenCacheService.getGoogleTokenInfo(accessToken);
                if (googleTokenInfo != null) {
                    log.debug("Token is associated with Google SSO");
                    
                    // Check if Google token is expired
                    if (googleTokenInfo.ifExpired()) {
                        log.warn("Google token is expired for system token: {}", accessToken);
                        // Optionally try to refresh the token here
                        // For now, we'll let it proceed but log the warning
                    }
                    
                    // Enhance authentication with Google user info
                    return enhanceAuthenticationWithGoogleInfo(localAuth, googleTokenInfo);
                }
                
                log.debug("Token found in local store but not associated with Google SSO");
                return localAuth;
            }

            // If not found locally, try remote validation (for standard OAuth2 tokens)
            log.debug("Token not found in local store, trying remote validation");
            return super.loadAuthentication(accessToken);

        } catch (Exception e) {
            log.error("Error loading authentication for token: {}", accessToken, e);
            throw new InvalidTokenException("Invalid token: " + accessToken);
        }
    }

    @Override
    public OAuth2AccessToken readAccessToken(String accessToken) {
        log.debug("Reading access token: {}", accessToken);

        try {
            // First, try to read from local token store
            OAuth2AccessToken localToken = tokenStore.readAccessToken(accessToken);
            if (localToken != null) {
                log.debug("Found access token in local token store");
                return localToken;
            }

            // If not found locally, try remote reading
            log.debug("Access token not found in local store, trying remote reading");
            return super.readAccessToken(accessToken);

        } catch (Exception e) {
            log.error("Error reading access token: {}", accessToken, e);
            return null;
        }
    }

    /**
     * Enhance OAuth2Authentication with Google user information
     */
    private OAuth2Authentication enhanceAuthenticationWithGoogleInfo(OAuth2Authentication originalAuth, 
                                                                   GoogleTokenCacheInfo googleTokenInfo) {
        try {
            // Get the original user authentication
            Authentication userAuth = originalAuth.getUserAuthentication();
            if (userAuth instanceof UsernamePasswordAuthenticationToken) {
                UsernamePasswordAuthenticationToken tokenAuth = (UsernamePasswordAuthenticationToken) userAuth;
                
                if (tokenAuth.getPrincipal() instanceof EnhancerUser) {
                    EnhancerUser enhancerUser = (EnhancerUser) tokenAuth.getPrincipal();
                    
                    // Add Google-specific authorities
                    List<GrantedAuthority> authorities = new ArrayList<>(enhancerUser.getAuthorities());
                    authorities.add(new SimpleGrantedAuthority("GOOGLE_SSO_USER"));
                    
                    // Create enhanced user with Google info
                    EnhancerUser enhancedUser = new EnhancerUser(
                        enhancerUser.getId(),
                        enhancerUser.getDeptId(),
                        enhancerUser.getUsername(),
                        enhancerUser.getPassword(),
                        enhancerUser.isEnabled(),
                        enhancerUser.isAccountNonExpired(),
                        enhancerUser.isCredentialsNonExpired(),
                        enhancerUser.isAccountNonLocked(),
                        authorities
                    );
                    
                    // Create new user authentication with enhanced user
                    UsernamePasswordAuthenticationToken enhancedUserAuth = 
                        new UsernamePasswordAuthenticationToken(enhancedUser, null, authorities);
                    
                    // Create enhanced OAuth2 request with Google info
                    OAuth2Request originalRequest = originalAuth.getOAuth2Request();
                    Map<String, String> requestParams = new HashMap<>(originalRequest.getRequestParameters());
                    requestParams.put("google_user_id", googleTokenInfo.getGoogleUserInfo().getId());
                    requestParams.put("google_email", googleTokenInfo.getGoogleUserInfo().getEmail());
                    
                    OAuth2Request enhancedRequest = new OAuth2Request(
                        requestParams,
                        originalRequest.getClientId(),
                        authorities,
                        originalRequest.isApproved(),
                        originalRequest.getScope(),
                        originalRequest.getResourceIds(),
                        originalRequest.getRedirectUri(),
                        originalRequest.getResponseTypes(),
                        originalRequest.getExtensions()
                    );
                    
                    return new OAuth2Authentication(enhancedRequest, enhancedUserAuth);
                }
            }
            
            // If enhancement fails, return original authentication
            return originalAuth;
            
        } catch (Exception e) {
            log.error("Error enhancing authentication with Google info", e);
            return originalAuth;
        }
    }
}