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
package me.fengorz.kiwi.security.token;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.security.KiwiUser;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Filter to authenticate requests using Bearer tokens stored in Redis.
 * Extracts the token from the Authorization header, validates it via SystemTokenService,
 * and sets the KiwiUser in the SecurityContext.
 *
 * @author codingByFeng
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BearerTokenAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final SystemTokenService systemTokenService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = extractToken(request);
            if (StringUtils.hasText(token)) {
                SystemToken systemToken = systemTokenService.readAccessToken(token);
                if (systemToken != null && !systemToken.isExpired()) {
                    KiwiUser kiwiUser = buildKiwiUser(systemToken);
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(kiwiUser, null, kiwiUser.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.debug("Authenticated user: {} for URI: {}", systemToken.getUsername(), request.getRequestURI());
                } else if (systemToken != null) {
                    log.debug("Token expired for user: {}", systemToken.getUsername());
                }
            }
        } catch (Exception e) {
            log.error("Failed to authenticate token", e);
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        // Try Authorization header first
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }

        // SECURITY NOTE: Query parameter tokens are a known limitation for WebSocket connections.
        // Tokens in URLs may appear in server/proxy logs. Consider migrating to Sec-WebSocket-Protocol auth.
        String accessToken = request.getParameter("access_token");
        if (StringUtils.hasText(accessToken)) {
            log.warn("Token received via query parameter for URI: {} - consider migrating to subprotocol auth",
                    request.getRequestURI());
            return accessToken;
        }

        return null;
    }

    private KiwiUser buildKiwiUser(SystemToken token) {
        List<SimpleGrantedAuthority> authorities;
        if (Boolean.TRUE.equals(token.getIsAdmin())) {
            authorities = List.of(
                    new SimpleGrantedAuthority("ROLE_ADMIN"),
                    new SimpleGrantedAuthority("ROLE_USER")
            );
        } else {
            authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
        }

        return KiwiUser.kiwiBuilder()
                .userId(token.getUserId())
                .deptId(token.getDeptId())
                .username(token.getUsername())
                .password("")
                .email(token.getEmail())
                .realName(token.getRealName())
                .avatar(token.getAvatar())
                .registerSource(token.getAuthMethod())
                .enabled(true)
                .accountNonExpired(true)
                .credentialsNonExpired(true)
                .accountNonLocked(true)
                .authorities(authorities)
                .build();
    }
}
