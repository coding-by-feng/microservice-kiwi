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
package me.fengorz.kiwi.api.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.PasswordValidator;
import me.fengorz.kiwi.common.R;
import me.fengorz.kiwi.domain.upms.entity.SysUser;
import me.fengorz.kiwi.domain.upms.service.SysUserService;
import me.fengorz.kiwi.security.KiwiUser;
import me.fengorz.kiwi.security.token.SystemToken;
import me.fengorz.kiwi.security.token.SystemTokenService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Authentication Controller
 * Handles login, registration, logout, and token operations
 * Uses SystemTokenService for unified token management (shared with Google OAuth)
 *
 * @author codingByFeng
 */
@Slf4j
@RestController
@RequestMapping("/auth/oauth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication and registration operations")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final SysUserService sysUserService;
    private final SystemTokenService systemTokenService;

    /**
     * Login with username and password
     * Returns a SystemToken that is compatible with Google OAuth tokens
     */
    @PostMapping(value = "/login", consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE})
    @Operation(summary = "Login with username and password")
    public R<Map<String, Object>> login(
            @RequestBody(required = false) LoginRequest body,
            @RequestParam(value = "username", required = false) String usernameParam,
            @RequestParam(value = "password", required = false) String passwordParam) {

        String username = body != null ? body.getUsername() : usernameParam;
        String password = body != null ? body.getPassword() : passwordParam;

        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            return R.failed("Username and password are required");
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password));

            Object principal = authentication.getPrincipal();
            if (!(principal instanceof KiwiUser)) {
                log.warn("Unsupported principal type: {}", principal != null ? principal.getClass() : null);
                return R.failed("Authentication principal type unsupported");
            }

            KiwiUser user = (KiwiUser) principal;
            SystemToken token = systemTokenService.generateToken(user);

            Map<String, Object> response = systemTokenService.buildTokenResponse(token);
            // Add user info to response
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("userId", user.getUserId());
            userInfo.put("username", user.getUsername());
            userInfo.put("email", user.getEmail());
            userInfo.put("realName", user.getRealName());
            userInfo.put("avatar", user.getAvatar());
            response.put("userInfo", userInfo);

            return R.ok(response);

        } catch (BadCredentialsException e) {
            log.warn("Bad credentials for user: {}", username);
            return R.failed("Invalid username or password");
        } catch (Exception e) {
            log.error("Login error for user {}", username, e);
            return R.failed("Login error: " + e.getMessage());
        }
    }

    /**
     * Register a new user
     */
    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    public R<Map<String, Object>> register(@RequestBody RegisterRequest request) {
        // Validate required fields
        if (!StringUtils.hasText(request.getUsername())) {
            return R.failed("Username is required");
        }
        if (!StringUtils.hasText(request.getPassword())) {
            return R.failed("Password is required");
        }
        if (!StringUtils.hasText(request.getEmail())) {
            return R.failed("Email is required");
        }

        // Validate password strength
        String passwordError = PasswordValidator.validate(request.getPassword());
        if (passwordError != null) {
            return R.failed(passwordError);
        }

        // Check if username already exists
        if (sysUserService.existsByUsername(request.getUsername())) {
            return R.failed("Username already exists");
        }

        // Check if email already exists
        if (sysUserService.existsByEmail(request.getEmail())) {
            return R.failed("Email already exists");
        }

        try {
            // Create new user
            SysUser newUser = new SysUser();
            newUser.setUsername(request.getUsername());
            newUser.setEmail(request.getEmail());
            newUser.setRealName(request.getRealName());
            newUser.setPhone(request.getPhone());
            newUser.setAvatar(request.getAvatar());
            newUser.setDeptId(request.getDeptId() != null ? request.getDeptId() : 1); // Default department
            newUser.setLockFlag(0);
            newUser.setDelFlag(0);
            newUser.setCreateTime(LocalDateTime.now());
            newUser.setUpdateTime(LocalDateTime.now());

            // Save user with encoded password
            SysUser savedUser = sysUserService.createUser(newUser, request.getPassword());

            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("userId", savedUser.getUserId());
            response.put("username", savedUser.getUsername());
            response.put("email", savedUser.getEmail());
            response.put("message", "Registration successful. Please login.");

            log.info("New user registered: {}", savedUser.getUsername());
            return R.ok(response);

        } catch (Exception e) {
            log.error("Registration error for user {}", request.getUsername(), e);
            return R.failed("Registration error: " + e.getMessage());
        }
    }

    /**
     * Logout - invalidate the access token
     * Supports both POST and DELETE for compatibility
     */
    @RequestMapping(value = "/logout", method = {RequestMethod.POST, RequestMethod.DELETE})
    @Operation(summary = "Logout current user")
    public R<Boolean> logout(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader) {
        if (!StringUtils.hasText(authHeader)) {
            return R.ok(true); // Already logged out
        }

        String tokenValue = authHeader.replace("Bearer ", "").trim();

        try {
            systemTokenService.removeAccessToken(tokenValue);
            log.info("User logged out, token invalidated");
            return R.ok(true);
        } catch (Exception e) {
            log.error("Error during logout", e);
            return R.failed("Logout failed: " + e.getMessage());
        }
    }

    /**
     * Get current user info from token
     */
    @GetMapping("/me")
    @Operation(summary = "Get current user info")
    public R<Map<String, Object>> getCurrentUser(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader) {
        if (!StringUtils.hasText(authHeader)) {
            return R.failed("Authorization header is required");
        }

        String tokenValue = authHeader.replace("Bearer ", "").trim();
        SystemToken token = systemTokenService.readAccessToken(tokenValue);

        if (token == null) {
            return R.failed("Invalid token");
        }

        if (token.isExpired()) {
            return R.failed("Token expired");
        }

        Map<String, Object> response = new HashMap<>();
        response.put("userId", token.getUserId());
        response.put("username", token.getUsername());
        response.put("email", token.getEmail());
        response.put("realName", token.getRealName());
        response.put("avatar", token.getAvatar());
        response.put("deptId", token.getDeptId());
        response.put("isAdmin", token.getIsAdmin());
        response.put("authMethod", token.getAuthMethod());

        return R.ok(response);
    }

    /**
     * Refresh access token
     */
    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token")
    public R<Map<String, Object>> refreshToken(@RequestBody RefreshTokenRequest request) {
        if (!StringUtils.hasText(request.getRefreshToken())) {
            return R.failed("Refresh token is required");
        }

        try {
            SystemToken newToken = systemTokenService.refreshToken(request.getRefreshToken());
            if (newToken == null) {
                return R.failed("Invalid or expired refresh token");
            }

            Map<String, Object> response = systemTokenService.buildTokenResponse(newToken);
            return R.ok(response);

        } catch (Exception e) {
            log.error("Token refresh error", e);
            return R.failed("Token refresh error: " + e.getMessage());
        }
    }

    /**
     * Check if username is available
     */
    @GetMapping("/check-username")
    @Operation(summary = "Check if username is available")
    public R<Boolean> checkUsername(@RequestParam String username) {
        if (!StringUtils.hasText(username)) {
            return R.failed("Username is required");
        }
        boolean available = !sysUserService.existsByUsername(username);
        // Random delay to prevent timing-based enumeration attacks
        try {
            Thread.sleep(ThreadLocalRandom.current().nextLong(50, 150));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return R.ok(available);
    }

    /**
     * Check if email is available
     */
    @GetMapping("/check-email")
    @Operation(summary = "Check if email is available")
    public R<Boolean> checkEmail(@RequestParam String email) {
        if (!StringUtils.hasText(email)) {
            return R.failed("Email is required");
        }
        boolean available = !sysUserService.existsByEmail(email);
        // Random delay to prevent timing-based enumeration attacks
        try {
            Thread.sleep(ThreadLocalRandom.current().nextLong(50, 150));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return R.ok(available);
    }

    @Data
    public static class LoginRequest {
        private String username;
        private String password;
    }

    @Data
    public static class RegisterRequest {
        private String username;
        private String password;
        private String email;
        private String realName;
        private String phone;
        private String avatar;
        private Integer deptId;
    }

    @Data
    public static class RefreshTokenRequest {
        private String refreshToken;
    }
}
