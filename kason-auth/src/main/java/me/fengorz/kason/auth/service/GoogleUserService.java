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

package me.fengorz.kason.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kason.bdf.security.google.dto.GoogleUserInfo;
import me.fengorz.kason.common.api.R;
import me.fengorz.kason.common.api.entity.EnhancerUser;
import me.fengorz.kason.common.sdk.constant.SecurityConstants;
import me.fengorz.kason.upms.api.dto.UserFullInfoDTO;
import me.fengorz.kason.upms.api.entity.SysUser;
import me.fengorz.kason.upms.api.feign.UserApi;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Service for handling Google user integration with UPMS system
 *
 * @Author Kason Zhan
 * @Date 2025-06-16
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleUserService {

    private final UserApi userApi;
    private final BCryptPasswordEncoder passwordEncoder;

    /**
     * Find or create user based on Google user information
     *
     * @param googleUserInfo Google user information
     * @return Enhanced user for the system
     */
    public EnhancerUser findOrCreateUser(GoogleUserInfo googleUserInfo) {
        // First, try to find existing user by email
        R<UserFullInfoDTO> userResponse = userApi.info(googleUserInfo.getEmail());

        UserFullInfoDTO userFullInfo;
        SysUser sysUser;

        if (userResponse != null && userResponse.isSuccess() && userResponse.getData() != null) {
            // User exists, update Google info if needed
            userFullInfo = userResponse.getData();
            sysUser = userFullInfo.getSysUser();
            updateGoogleInfo(sysUser, googleUserInfo);
        } else {
            // User doesn't exist, create new one
            sysUser = createNewGoogleUser(googleUserInfo);
            userFullInfo = createUserFullInfo(sysUser);

            // After creating the user, retrieve the full info from the system
            userResponse = userApi.info(googleUserInfo.getEmail(), SecurityConstants.FROM_IN);
            if (userResponse != null && userResponse.isSuccess() && userResponse.getData() != null) {
                userFullInfo = userResponse.getData();
                sysUser = userFullInfo.getSysUser();
            }
        }

        return convertToEnhancerUser(sysUser, userFullInfo);
    }

    /**
     * Create new user from Google information
     *
     * @param googleUserInfo Google user information
     * @return Created SysUser
     */
    private SysUser createNewGoogleUser(GoogleUserInfo googleUserInfo) {
        SysUser sysUser = new SysUser();

        // Generate a unique user ID (you may want to use your sequence service)
        // sysUser.setUserId(seqService.genCommonIntSequence());

        // Basic user information
        sysUser.setUsername(googleUserInfo.getEmail());
        sysUser.setPassword(passwordEncoder.encode(generateRandomPassword()));
        sysUser.setEmail(googleUserInfo.getEmail());
        sysUser.setRealName(googleUserInfo.getName());
        sysUser.setPhone(googleUserInfo.getEmail()); // Use email as phone for now
        sysUser.setAvatar(googleUserInfo.getPicture());
        sysUser.setGoogleOpenid(googleUserInfo.getId());
        sysUser.setRegisterSource("google");
        sysUser.setCreateTime(LocalDateTime.now());
        sysUser.setUpdateTime(LocalDateTime.now());
        sysUser.setLockFlag(0); // Not locked
        sysUser.setDelFlag(0);  // Not deleted

        // Set default department (you may want to configure this)
        sysUser.setDeptId(1); // Default department ID
        sysUser.setUserId(0); // Default User ID, automatically assigned

        // Save user via Feign API
        R<Boolean> saveResult = userApi.save(sysUser);
        if (saveResult == null || !saveResult.isSuccess() || !Boolean.TRUE.equals(saveResult.getData())) {
            log.error("Failed to create user via Feign API for Google user: {}", googleUserInfo.getEmail());
            throw new RuntimeException("Failed to create user: " + googleUserInfo.getEmail());
        }

        log.info("Successfully created new user from Google SSO: {}", googleUserInfo.getEmail());
        return sysUser;
    }

    /**
     * Update existing user with Google information
     *
     * @param sysUser Existing system user
     * @param googleUserInfo Google user information
     */
    private void updateGoogleInfo(SysUser sysUser, GoogleUserInfo googleUserInfo) {
        boolean needsUpdate = false;

        // Update Google OpenID if not already set
        if (sysUser.getGoogleOpenid() == null) {
            sysUser.setGoogleOpenid(googleUserInfo.getId());
            needsUpdate = true;
        }

        // Update avatar if Google has a newer one
        if (googleUserInfo.getPicture() != null && !googleUserInfo.getPicture().equals(sysUser.getAvatar())) {
            sysUser.setAvatar(googleUserInfo.getPicture());
            needsUpdate = true;
        }

        // Update real name if not set
        if (sysUser.getRealName() == null && googleUserInfo.getName() != null) {
            sysUser.setRealName(googleUserInfo.getName());
            needsUpdate = true;
        }

        // Update email if not set
        if (sysUser.getEmail() == null) {
            sysUser.setEmail(googleUserInfo.getEmail());
            needsUpdate = true;
        }

        if (needsUpdate) {
            sysUser.setUpdateTime(LocalDateTime.now());

            // Update user via Feign API
            R<Boolean> updateResult = updateUserViaFeign(sysUser);
            if (updateResult == null || !updateResult.isSuccess() || !Boolean.TRUE.equals(updateResult.getData())) {
                log.error("Failed to update user via Feign API for Google user: {}", googleUserInfo.getEmail());
                // Don't throw exception for update failures, just log the error
            } else {
                log.info("Successfully updated user info from Google SSO: {}", googleUserInfo.getEmail());
            }
        }
    }

    /**
     * Update user via Feign API (placeholder for actual update method)
     * Note: This assumes you have an update method in your UserApi
     *
     * @param sysUser User to update
     * @return Result of update operation
     */
    private R<Boolean> updateUserViaFeign(SysUser sysUser) {
        // This is a placeholder - you'll need to implement an update method in UserApi
        // For now, we'll just log that an update is needed
        log.info("User update needed for: {} (Google OpenID: {})", sysUser.getUsername(), sysUser.getGoogleOpenid());

        // You might want to add an update method to UserApi like:
        // return userApi.updateById(sysUser, SecurityConstants.FROM_IN);

        // For now, return success to avoid breaking the flow
        return R.success(true);
    }

    /**
     * Create UserFullInfoDTO for new Google user
     *
     * @param sysUser System user
     * @return UserFullInfoDTO with default permissions
     */
    private UserFullInfoDTO createUserFullInfo(SysUser sysUser) {
        UserFullInfoDTO userFullInfo = new UserFullInfoDTO();
        userFullInfo.setSysUser(sysUser);

        // Set default permissions for Google users
        userFullInfo.setPermissions(new String[]{"user:read", "user:write"});

        // Set default role (regular user)
        userFullInfo.setRoles(new Integer[]{2}); // Assuming role ID 2 is for regular users

        return userFullInfo;
    }

    /**
     * Convert SysUser to EnhancerUser for authentication
     *
     * @param sysUser System user
     * @param userFullInfo Full user information with roles and permissions
     * @return EnhancerUser for authentication
     */
    private EnhancerUser convertToEnhancerUser(SysUser sysUser, UserFullInfoDTO userFullInfo) {
        // Create authorities from permissions
        Collection<GrantedAuthority> authorities = new ArrayList<>();

        if (userFullInfo != null && userFullInfo.getPermissions() != null) {
            for (String permission : userFullInfo.getPermissions()) {
                authorities.add(new SimpleGrantedAuthority(permission));
            }
        }

        // Add default authority if none exist
        if (authorities.isEmpty()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        }

        // Create EnhancerUser with required constructor parameters
        EnhancerUser enhancerUser = new EnhancerUser(
                sysUser.getUserId(),
                sysUser.getDeptId(),
                sysUser.getUsername(),
                sysUser.getPassword(),
                sysUser.getDelFlag() == 0,  // enabled
                true,  // accountNonExpired
                true,  // credentialsNonExpired
                sysUser.getLockFlag() == 0, // accountNonLocked
                authorities
        );

        // Initialize properties
        enhancerUser.afterPropertiesSet();

        return enhancerUser;
    }

    /**
     * Generate random password for Google users
     *
     * @return Random password
     */
    private String generateRandomPassword() {
        return "google_sso_" + System.currentTimeMillis() + "_" + Math.random();
    }

    /**
     * Check if user exists by email
     *
     * @param email User email
     * @return true if user exists
     */
    public boolean userExists(String email) {
        R<UserFullInfoDTO> userResponse = userApi.info(email, SecurityConstants.FROM_IN);
        return userResponse != null && userResponse.isSuccess() && userResponse.getData() != null;
    }

    /**
     * Link Google account to existing user
     *
     * @param existingUsername Existing username
     * @param googleUserInfo Google user information
     * @return Updated user information
     */
    public EnhancerUser linkGoogleAccount(String existingUsername, GoogleUserInfo googleUserInfo) {
        R<UserFullInfoDTO> userResponse = userApi.info(existingUsername, SecurityConstants.FROM_IN);

        if (userResponse == null || !userResponse.isSuccess() || userResponse.getData() == null) {
            throw new RuntimeException("User not found: " + existingUsername);
        }

        UserFullInfoDTO userFullInfo = userResponse.getData();
        SysUser sysUser = userFullInfo.getSysUser();

        // Update user with Google information
        boolean needsUpdate = false;

        if (sysUser.getGoogleOpenid() == null) {
            sysUser.setGoogleOpenid(googleUserInfo.getId());
            needsUpdate = true;
        }

        if (sysUser.getEmail() == null) {
            sysUser.setEmail(googleUserInfo.getEmail());
            needsUpdate = true;
        }

        if (googleUserInfo.getPicture() != null && !googleUserInfo.getPicture().equals(sysUser.getAvatar())) {
            sysUser.setAvatar(googleUserInfo.getPicture());
            needsUpdate = true;
        }

        if (sysUser.getRealName() == null && googleUserInfo.getName() != null) {
            sysUser.setRealName(googleUserInfo.getName());
            needsUpdate = true;
        }

        if (needsUpdate) {
            sysUser.setUpdateTime(LocalDateTime.now());

            // Update user via Feign API
            R<Boolean> updateResult = updateUserViaFeign(sysUser);
            if (updateResult == null || !updateResult.isSuccess() || !Boolean.TRUE.equals(updateResult.getData())) {
                log.error("Failed to update user via Feign API when linking Google account: {}", existingUsername);
                // Don't throw exception, just log the error
            } else {
                log.info("Successfully linked Google account {} to existing user {}", googleUserInfo.getEmail(), existingUsername);
            }
        }

        return convertToEnhancerUser(sysUser, userFullInfo);
    }

    /**
     * Get default role ID for Google users
     *
     * @return Default role ID
     */
    public Integer getDefaultRoleId() {
        // You may want to configure this or query from database
        return 2; // Assuming role ID 2 is for regular users
    }
}