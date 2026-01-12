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
package me.fengorz.kiwi.api.upms;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import me.fengorz.kiwi.common.R;
import me.fengorz.kiwi.domain.upms.entity.SysRole;
import me.fengorz.kiwi.domain.upms.entity.SysUser;
import me.fengorz.kiwi.domain.upms.service.SysUserService;
import me.fengorz.kiwi.security.KiwiUser;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * SysUser REST Controller
 *
 * @author codingByFeng
 */
@RestController
@RequestMapping("/api/upms/user")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "User CRUD operations")
public class SysUserController {

    private final SysUserService userService;

    @GetMapping("/current")
    @Operation(summary = "Get current user info")
    public R<SysUser> getCurrentUser(@AuthenticationPrincipal KiwiUser principal) {
        return userService.findById(principal.getUserId())
                .map(R::ok)
                .orElse(R.failed("User not found"));
    }

    @GetMapping("/current/permissions")
    @Operation(summary = "Get current user permissions")
    public R<Set<String>> getCurrentUserPermissions(@AuthenticationPrincipal KiwiUser principal) {
        Set<String> permissions = userService.getUserPermissions(principal.getUserId());
        return R.ok(permissions);
    }

    @GetMapping("/current/roles")
    @Operation(summary = "Get current user roles")
    public R<List<SysRole>> getCurrentUserRoles(@AuthenticationPrincipal KiwiUser principal) {
        List<SysRole> roles = userService.getUserRoles(principal.getUserId());
        return R.ok(roles);
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get user by ID")
    @PreAuthorize("hasRole('ADMIN')")
    public R<SysUser> getById(@PathVariable Integer userId) {
        return userService.findById(userId)
                .map(R::ok)
                .orElse(R.failed("User not found"));
    }

    @GetMapping("/username/{username}")
    @Operation(summary = "Get user by username")
    @PreAuthorize("hasRole('ADMIN')")
    public R<SysUser> getByUsername(@PathVariable String username) {
        return userService.findByUsername(username)
                .map(R::ok)
                .orElse(R.failed("User not found"));
    }

    @GetMapping
    @Operation(summary = "Get all users with pagination")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Page<SysUser>> list(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size) {
        Page<SysUser> page = new Page<>(current, size);
        return R.ok(userService.findAllUsers(page));
    }

    @PostMapping
    @Operation(summary = "Create new user")
    @PreAuthorize("hasRole('ADMIN')")
    public R<SysUser> create(@RequestBody SysUser user) {
        if (userService.existsByUsername(user.getUsername())) {
            return R.failed("Username already exists");
        }
        if (user.getEmail() != null && userService.existsByEmail(user.getEmail())) {
            return R.failed("Email already exists");
        }
        return R.ok(userService.saveUser(user));
    }

    @PutMapping("/{userId}")
    @Operation(summary = "Update user")
    @PreAuthorize("hasRole('ADMIN')")
    public R<SysUser> update(@PathVariable Integer userId, @RequestBody SysUser user) {
        if (userService.findById(userId).isEmpty()) {
            return R.failed("User not found");
        }
        user.setUserId(userId);
        return R.ok(userService.saveUser(user));
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "Delete user")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> delete(@PathVariable Integer userId) {
        userService.deleteUserById(userId);
        return R.ok();
    }

    @PostMapping("/{userId}/lock")
    @Operation(summary = "Lock user account")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> lock(@PathVariable Integer userId) {
        userService.lockUser(userId);
        return R.ok();
    }

    @PostMapping("/{userId}/unlock")
    @Operation(summary = "Unlock user account")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> unlock(@PathVariable Integer userId) {
        userService.unlockUser(userId);
        return R.ok();
    }

    @PostMapping("/{userId}/roles")
    @Operation(summary = "Assign roles to user")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> assignRoles(@PathVariable Integer userId, @RequestBody List<Integer> roleIds) {
        userService.assignRoles(userId, roleIds);
        return R.ok();
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change current user password")
    public R<Void> changePassword(@AuthenticationPrincipal KiwiUser principal,
                                   @RequestParam String oldPassword,
                                   @RequestParam String newPassword) {
        SysUser user = userService.findById(principal.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!userService.verifyPassword(oldPassword, user.getPassword())) {
            return R.failed("Invalid old password");
        }

        userService.updatePassword(principal.getUserId(), newPassword);
        return R.ok();
    }

    @PostMapping("/{userId}/reset-password")
    @Operation(summary = "Reset user password (Admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> resetPassword(@PathVariable Integer userId,
                                  @RequestParam String newPassword) {
        if (userService.findById(userId).isEmpty()) {
            return R.failed("User not found");
        }
        userService.updatePassword(userId, newPassword);
        return R.ok();
    }

    @PostMapping("/reset-password-by-username")
    @Operation(summary = "Reset user password by username (Admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> resetPasswordByUsername(@RequestParam String username,
                                            @RequestParam String newPassword) {
        SysUser user = userService.findByUsername(username)
                .orElse(null);
        if (user == null) {
            return R.failed("User not found");
        }
        userService.updatePassword(user.getUserId(), newPassword);
        return R.ok();
    }
}
