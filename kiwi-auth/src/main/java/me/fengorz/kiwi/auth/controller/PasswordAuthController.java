package me.fengorz.kiwi.auth.controller;

import cn.hutool.core.util.StrUtil;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.api.R;
import me.fengorz.kiwi.common.api.entity.EnhancerUser;
import me.fengorz.kiwi.common.sdk.constant.SecurityConstants;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.common.DefaultExpiringOAuth2RefreshToken;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2RefreshToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/oauth/username-password")
public class PasswordAuthController {

    private final AuthenticationManager authenticationManager;
    private final TokenStore tokenStore;

    @PostMapping(value = "/login", consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE})
    public R<Map<String, Object>> login(@RequestBody(required = false) LoginRequest body,
                                        @RequestParam(value = "username", required = false) String usernameParam,
                                        @RequestParam(value = "password", required = false) String passwordParam) {
        String username = body != null ? body.getUsername() : usernameParam;
        String password = body != null ? body.getPassword() : passwordParam;

        if (!StrUtil.isAllNotBlank(username, password)) {
            return R.failed("username and password are required");
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password));

            Object principal = authentication.getPrincipal();
            if (!(principal instanceof EnhancerUser)) {
                log.warn("Unsupported principal type: {}", principal != null ? principal.getClass() : null);
                return R.failed("authentication principal type unsupported");
            }

            EnhancerUser user = (EnhancerUser) principal;
            OAuth2AccessToken accessToken = generateSystemToken(user);

            Map<String, Object> resp = new HashMap<>();
            resp.put("access_token", accessToken.getValue());
            resp.put("token_type", accessToken.getTokenType());
            resp.put("expires_in", accessToken.getExpiresIn());
            resp.put("scope", accessToken.getScope());
            resp.put("refresh_token", accessToken.getRefreshToken() != null ? accessToken.getRefreshToken().getValue() : null);
            return R.success(resp, "login success");
        } catch (BadCredentialsException e) {
            log.warn("Bad credentials for user: {}", username);
            return R.failed("invalid username or password");
        } catch (Exception e) {
            log.error("Login error for user {}", username, e);
            return R.failed("login error: " + e.getMessage());
        }
    }

    @NotNull
    private OAuth2AccessToken generateSystemToken(EnhancerUser user) {
        // Build OAuth2 request
        Map<String, String> requestParameters = new HashMap<>();
        requestParameters.put("grant_type", "password");
        requestParameters.put("client_id", "password-client");

        Set<String> scopes = new HashSet<>();
        scopes.add("read");
        scopes.add("write");
        scopes.add("profile");

        OAuth2Request oAuth2Request = new OAuth2Request(
                requestParameters,
                "password-client",
                user.getAuthorities(),
                true,
                scopes,
                Collections.emptySet(),
                null,
                Collections.emptySet(),
                Collections.emptyMap()
        );

        UsernamePasswordAuthenticationToken userAuth = new UsernamePasswordAuthenticationToken(
                user,
                null,
                user.getAuthorities()
        );
        OAuth2Authentication oAuth2Authentication = new OAuth2Authentication(oAuth2Request, userAuth);

        // Create token
        DefaultOAuth2AccessToken accessToken = new DefaultOAuth2AccessToken(UUID.randomUUID().toString());
        accessToken.setExpiration(new Date(System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000));
        accessToken.setScope(scopes);
        accessToken.setTokenType("Bearer");

        Date refreshTokenExpiration = new Date(System.currentTimeMillis() + 90L * 24 * 60 * 60 * 1000);
        OAuth2RefreshToken refreshToken = new DefaultExpiringOAuth2RefreshToken(UUID.randomUUID().toString(), refreshTokenExpiration);
        accessToken.setRefreshToken(refreshToken);

        Map<String, Object> additionalInfo = new HashMap<>();
        additionalInfo.put(SecurityConstants.DETAILS_LICENSE, SecurityConstants.PROJECT_LICENSE);
        additionalInfo.put(SecurityConstants.DETAILS_USER_ID, user.getId());
        additionalInfo.put(SecurityConstants.DETAILS_USERNAME, user.getUsername());
        additionalInfo.put(SecurityConstants.DETAILS_DEPT_ID, user.getDeptId());
        additionalInfo.put("auth_method", "standard");
        additionalInfo.put("is_admin", user.getIsAdmin());
        accessToken.setAdditionalInformation(additionalInfo);

        tokenStore.storeAccessToken(accessToken, oAuth2Authentication);
        return accessToken;
    }

    @Data
    public static class LoginRequest {
        private String username;
        private String password;
    }
}

