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
package me.fengorz.kiwi.domain.upms.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import me.fengorz.kiwi.domain.upms.entity.SysRole;
import me.fengorz.kiwi.domain.upms.entity.SysUser;
import me.fengorz.kiwi.domain.upms.mapper.SysUserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * SysUser Service
 *
 * @author codingByFeng
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SysUserService extends ServiceImpl<SysUserMapper, SysUser> {

    private final SysRoleService roleService;
    private final SysMenuService menuService;
    private final PasswordEncoder passwordEncoder;
    private final SysUserRoleRelService userRoleRelService;

    /**
     * Find user by ID
     */
    public Optional<SysUser> findById(Integer userId) {
        return Optional.ofNullable(getById(userId));
    }

    /**
     * Find user by username
     */
    public Optional<SysUser> findByUsername(String username) {
        return Optional.ofNullable(getOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username)
                .ne(SysUser::getDelFlag, 1)));
    }

    /**
     * Find user by email
     */
    public Optional<SysUser> findByEmail(String email) {
        return Optional.ofNullable(getOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getEmail, email)
                .ne(SysUser::getDelFlag, 1)));
    }

    /**
     * Find user by Google OpenID
     */
    public Optional<SysUser> findByGoogleOpenid(String googleOpenid) {
        return Optional.ofNullable(getOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getGoogleOpenid, googleOpenid)
                .ne(SysUser::getDelFlag, 1)));
    }

    /**
     * Find user with roles
     */
    public Optional<SysUser> findByUsernameWithRoles(String username) {
        return findByUsername(username)
                .map(this::loadUserRoles);
    }

    /**
     * Find user with roles and menus
     */
    public Optional<SysUser> findByIdWithRolesAndMenus(Integer userId) {
        return findById(userId)
                .map(this::loadUserRoles);
    }

    /**
     * Load and set roles for user
     */
    private SysUser loadUserRoles(SysUser user) {
        List<Integer> roleIds = userRoleRelService.findRoleIdsByUserId(user.getUserId());
        if (!roleIds.isEmpty()) {
            List<SysRole> roles = roleService.listByIds(roleIds);
            user.setRoles(roles);
        }
        return user;
    }

    /**
     * Get all users with pagination
     */
    public Page<SysUser> findAllUsers(Page<SysUser> page) {
        return page(page, new LambdaQueryWrapper<SysUser>()
                .ne(SysUser::getDelFlag, 1));
    }

    /**
     * Check if username exists
     */
    public boolean existsByUsername(String username) {
        return count(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username)
                .ne(SysUser::getDelFlag, 1)) > 0;
    }

    /**
     * Check if email exists
     */
    public boolean existsByEmail(String email) {
        return count(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getEmail, email)
                .ne(SysUser::getDelFlag, 1)) > 0;
    }

    /**
     * Get user permissions
     */
    public Set<String> getUserPermissions(Integer userId) {
        // Note: This may need adjustment based on actual permission query implementation
        return Set.of();
    }

    /**
     * Get user roles
     */
    public List<SysRole> getUserRoles(Integer userId) {
        List<Integer> roleIds = userRoleRelService.findRoleIdsByUserId(userId);
        if (roleIds.isEmpty()) {
            return List.of();
        }
        return roleService.listByIds(roleIds);
    }

    /**
     * Save user (create or update)
     */
    @Transactional
    public SysUser saveUser(SysUser user) {
        if (user.getUserId() == null && user.getPassword() != null) {
            // Encode password for new users
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        saveOrUpdate(user);
        return user;
    }

    /**
     * Create new user with encoded password
     */
    @Transactional
    public SysUser createUser(SysUser user, String rawPassword) {
        user.setPassword(passwordEncoder.encode(rawPassword));
        save(user);
        return user;
    }

    /**
     * Update user password
     */
    @Transactional
    public void updatePassword(Integer userId, String newPassword) {
        findById(userId).ifPresent(user -> {
            user.setPassword(passwordEncoder.encode(newPassword));
            updateById(user);
        });
    }

    /**
     * Verify password
     */
    public boolean verifyPassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    /**
     * Lock user account
     */
    @Transactional
    public void lockUser(Integer userId) {
        findById(userId).ifPresent(user -> {
            user.lock();
            updateById(user);
        });
    }

    /**
     * Unlock user account
     */
    @Transactional
    public void unlockUser(Integer userId) {
        findById(userId).ifPresent(user -> {
            user.unlock();
            updateById(user);
        });
    }

    /**
     * Soft delete user
     */
    @Transactional
    public void deleteUserById(Integer userId) {
        findById(userId).ifPresent(user -> {
            user.setDelFlag(1);
            updateById(user);
        });
    }

    /**
     * Assign roles to user
     */
    @Transactional
    public void assignRoles(Integer userId, List<Integer> roleIds) {
        userRoleRelService.assignRolesToUser(userId, roleIds);
    }
}
