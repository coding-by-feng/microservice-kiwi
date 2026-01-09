/*
 * Copyright [2019~2025] [codingByFeng]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.fengorz.kiwi.security.google;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.domain.upms.entity.SysUser;
import me.fengorz.kiwi.domain.upms.service.SysUserService;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;

/**
 * Google OAuth2 Service
 *
 * @author codingByFeng
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleOAuth2Service {

    private static final String GOOGLE_AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String GOOGLE_TOKEN_URL = "https://www.googleapis.com/oauth2/v4/token";
    private static final String GOOGLE_USER_INFO_URL = "https://www.googleapis.com/oauth2/v2/userinfo";

    private final GoogleOAuth2Properties googleOAuth2Properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final SysUserService sysUserService;

    public String getAuthorizationUrl(String state) throws Exception {
        log.info("Generating Google OAuth2 authorization URL");

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(GOOGLE_AUTH_URL)
                .queryParam("client_id", googleOAuth2Properties.getClientId())
                .queryParam("redirect_uri", googleOAuth2Properties.getRedirectUri())
                .queryParam("scope", String.join(" ", googleOAuth2Properties.getScopes()))
                .queryParam("response_type", "code")
                .queryParam("access_type", "offline")
                .queryParam("prompt", "consent");

        if (state != null && !state.trim().isEmpty()) {
            builder.queryParam("state", URLEncoder.encode(state, StandardCharsets.UTF_8.name()));
        }

        return builder.toUriString();
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> exchangeCodeForToken(String code) {
        log.info("Exchanging authorization code for token");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", googleOAuth2Properties.getClientId());
        body.add("client_secret", googleOAuth2Properties.getClientSecret());
        body.add("code", code);
        body.add("grant_type", "authorization_code");
        body.add("redirect_uri", googleOAuth2Properties.getRedirectUri());

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(GOOGLE_TOKEN_URL, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                return objectMapper.readValue(response.getBody(), Map.class);
            } else {
                log.error("Token exchange failed - Status: {}", response.getStatusCode());
                throw new RuntimeException("Failed to exchange authorization code for token");
            }
        } catch (IOException e) {
            log.error("Error parsing token response", e);
            throw new RuntimeException("Error parsing token response", e);
        }
    }

    public GoogleUserInfo getUserInfo(String accessToken) {
        log.info("Fetching user information from Google");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    GOOGLE_USER_INFO_URL, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                return objectMapper.readValue(response.getBody(), GoogleUserInfo.class);
            } else {
                log.error("Failed to get user info - Status: {}", response.getStatusCode());
                throw new RuntimeException("Failed to get user information from Google");
            }
        } catch (IOException e) {
            log.error("Error parsing user info response", e);
            throw new RuntimeException("Error parsing user info response", e);
        }
    }

    public SysUser findOrCreateUser(GoogleUserInfo googleUserInfo) {
        log.info("Finding or creating user for email: {}", googleUserInfo.getEmail());

        return sysUserService.findByUsername(googleUserInfo.getEmail())
                .orElseGet(() -> createNewGoogleUser(googleUserInfo));
    }

    private SysUser createNewGoogleUser(GoogleUserInfo googleUserInfo) {
        SysUser sysUser = new SysUser();
        sysUser.setUsername(googleUserInfo.getEmail());
        sysUser.setPassword("google_sso_" + System.currentTimeMillis());
        sysUser.setRealName(googleUserInfo.getName());
        sysUser.setEmail(googleUserInfo.getEmail());
        sysUser.setPhone(googleUserInfo.getEmail());
        sysUser.setAvatar(googleUserInfo.getPicture());
        sysUser.setGoogleOpenid(googleUserInfo.getId());
        sysUser.setCreateTime(LocalDateTime.now());
        sysUser.setUpdateTime(LocalDateTime.now());
        sysUser.setLockFlag(0);
        sysUser.setDelFlag(0);
        sysUser.setDeptId(1);

        return sysUserService.saveUser(sysUser);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> refreshAccessToken(String refreshToken) {
        log.info("Refreshing access token");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", googleOAuth2Properties.getClientId());
        body.add("client_secret", googleOAuth2Properties.getClientSecret());
        body.add("refresh_token", refreshToken);
        body.add("grant_type", "refresh_token");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(GOOGLE_TOKEN_URL, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                return objectMapper.readValue(response.getBody(), Map.class);
            } else {
                log.error("Token refresh failed - Status: {}", response.getStatusCode());
                throw new RuntimeException("Failed to refresh access token");
            }
        } catch (IOException e) {
            log.error("Error parsing refresh token response", e);
            throw new RuntimeException("Error parsing refresh token response", e);
        }
    }
}
