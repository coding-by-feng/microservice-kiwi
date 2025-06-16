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

package me.fengorz.kiwi.upms.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.api.R;
import me.fengorz.kiwi.common.sdk.annotation.log.LogMarker;
import me.fengorz.kiwi.common.sdk.controller.BaseController;
import me.fengorz.kiwi.upms.api.dto.UserFullInfoDTO;
import me.fengorz.kiwi.upms.api.entity.SysUser;
import me.fengorz.kiwi.upms.api.entity.SysUserRoleRel;
import me.fengorz.kiwi.upms.service.SysUserRoleRelService;
import me.fengorz.kiwi.upms.service.SysUserService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Google user management controller for UPMS integration
 *
 * @author zhanshifeng
 * @date 2025-06-16
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/sys/user/google")
public class GoogleUserController extends BaseController {

    private final SysUserService sysUserService;
    private final SysUserRoleRelService sysUserRoleRelService;
    private final BCryptPasswordEncoder passwordEncoder;

    /**
     * Create or update user from Google OAuth information
     *
     * @param request Google user information
     * @return User full information
     */
    @LogMarker("Create or update Google user")
    @PostMapping("/createOrUpdate")
    public R<UserFullInfoDTO> createOrUpdateGoogleUser(@RequestBody Map<String, Object> request) {
        try {
            String googleId = (String) request.get("googleId");
            String email = (String) request.get("email");
            String name = (String) request.get("name");
            String picture = (String) request.get("picture");
            String givenName = (String) request.get("givenName");
            String familyName = (String) request.get("familyName");

            if (email == null || email.trim().isEmpty()) {
                return R.failed("Email is required");
            }

            // Check if user exists by email or Google ID
            SysUser existingUser = sysUserService.getOne(
                    Wrappers.<SysUser>lambdaQuery()
                            .eq(SysUser::getEmail, email)
                            .or()
                            .eq(SysUser::getGoogleOpenid, googleId)
                            .eq(SysUser::getDelFlag, 0)
            );

            SysUser sysUser;
            boolean isNewUser = false;

            if (existingUser != null) {
                // Update existing user
                sysUser = existingUser;
                updateGoogleUserInfo(sysUser, googleId, email, name, picture, givenName, familyName);
                sysUserService.updateById(sysUser);
                log.info("Updated existing user from Google SSO: {}", email);
            } else {
                // Create new user
                sysUser = createNewGoogleUser(googleId, email, name, picture, givenName, familyName);
                sysUserService.save(sysUser);
                isNewUser = true;
                log.info("Created new user from Google SSO: {}", email);

                // Assign default role to new user
                assignDefaultRole(sysUser.getUserId());
            }

            // Get full user information with roles and permissions
            UserFullInfoDTO userFullInfo = sysUserService.getUserFullInfo(sysUser);

            return R.success(userFullInfo, isNewUser ? "User created successfully" : "User updated successfully");

        } catch (Exception e) {
            log.error("Error creating/updating Google user", e);
            return R.failed("Failed to create/update user: " + e.getMessage());
        }
    }

    /**
     * Link Google account to existing user
     *
     * @param request Link request containing username and Google info
     * @return Updated user information
     */
    @LogMarker("Link Google account to existing user")
    @PostMapping("/link")
    public R<UserFullInfoDTO> linkGoogleAccount(@RequestBody Map<String, Object> request) {
        try {
            String username = (String) request.get("username");
            String password = (String) request.get("password");
            String googleId = (String) request.get("googleId");
            String email = (String) request.get("email");
            String name = (String) request.get("name");
            String picture = (String) request.get("picture");

            if (username == null || password == null) {
                return R.failed("Username and password are required");
            }

            // Verify existing user credentials
            SysUser existingUser = sysUserService.getOne(
                    Wrappers.<SysUser>lambdaQuery()
                            .eq(SysUser::getUsername, username)
                            .eq(SysUser::getDelFlag, 0)
            );

            if (existingUser == null) {
                return R.failed("User not found");
            }

            if (!passwordEncoder.matches(password, existingUser.getPassword())) {
                return R.failed("Invalid credentials");
            }

            // Check if Google account is already linked to another user
            SysUser googleLinkedUser = sysUserService.getOne(
                    Wrappers.<SysUser>lambdaQuery()
                            .eq(SysUser::getGoogleOpenid, googleId)
                            .ne(SysUser::getUserId, existingUser.getUserId())
                            .eq(SysUser::getDelFlag, 0)
            );

            if (googleLinkedUser != null) {
                return R.failed("Google account is already linked to another user");
            }

            // Link Google account
            existingUser.setGoogleOpenid(googleId);
            existingUser.setEmail(email);
            if (picture != null && !picture.trim().isEmpty()) {
                existingUser.setAvatar(picture);
            }
            if (name != null && !name.trim().isEmpty()) {
                existingUser.setRealName(name);
            }
            existingUser.setUpdateTime(LocalDateTime.now());

            sysUserService.updateById(existingUser);

            UserFullInfoDTO userFullInfo = sysUserService.getUserFullInfo(existingUser);
            return R.success(userFullInfo, "Google account linked successfully");

        } catch (Exception e) {
            log.error("Error linking Google account", e);
            return R.failed("Failed to link Google account: " + e.getMessage());
        }
    }

    /**
     * Unlink Google account from user
     *
     * @param userId User ID
     * @return Success response
     */
    @LogMarker("Unlink Google account")
    @PostMapping("/unlink/{userId}")
    public R<Boolean> unlinkGoogleAccount(@PathVariable Integer userId) {
        try {
            SysUser user = sysUserService.getById(userId);
            if (user == null) {
                return R.failed("User not found");
            }

            user.setGoogleOpenid(null);
            user.setUpdateTime(LocalDateTime.now());
            sysUserService.updateById(user);

            return R.success(true, "Google account unlinked successfully");

        } catch (Exception e) {
            log.error("Error unlinking Google account", e);
            return R.failed("Failed to unlink Google account: " + e.getMessage());
        }
    }

    /**
     * Create new user from Google information
     */
    private SysUser createNewGoogleUser(String googleId, String email, String name, String picture, String givenName, String familyName) {
        SysUser sysUser = new SysUser();

        sysUser.setUsername(email); // Use email as username
        sysUser.setPassword(passwordEncoder.encode("google_sso_" + System.currentTimeMillis()));
        sysUser.setEmail(email);
        sysUser.setRealName(name);
        sysUser.setAvatar(picture);
        sysUser.setGoogleOpenid(googleId);
        sysUser.setRegisterSource("google");
        sysUser.setPhone(email); // Temporary, can be updated later
        sysUser.setDeptId(1); // Default department
        sysUser.setCreateTime(LocalDateTime.now());
        sysUser.setUpdateTime(LocalDateTime.now());
        sysUser.setLockFlag(0);
        sysUser.setDelFlag(0);

        return sysUser;
    }

    /**
     * Update existing user with Google information
     */
    private void updateGoogleUserInfo(SysUser sysUser, String googleId, String email, String name, String picture, String givenName, String familyName) {
        sysUser.setGoogleOpenid(googleId);
        sysUser.setEmail(email);

        if (name != null && !name.trim().isEmpty()) {
            sysUser.setRealName(name);
        }

        if (picture != null && !picture.trim().isEmpty()) {
            sysUser.setAvatar(picture);
        }

        sysUser.setUpdateTime(LocalDateTime.now());
    }

    /**
     * Assign default role to new user
     */
    private void assignDefaultRole(Integer userId) {
        SysUserRoleRel userRoleRel = new SysUserRoleRel();
        userRoleRel.setUserId(userId);
        userRoleRel.setRoleId(2); // Default role ID for regular users
        sysUserRoleRelService.save(userRoleRel);
    }
}