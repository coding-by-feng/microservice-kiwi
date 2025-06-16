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
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.api.R;
import me.fengorz.kiwi.common.sdk.annotation.log.LogMarker;
import me.fengorz.kiwi.common.sdk.controller.BaseController;
import me.fengorz.kiwi.upms.api.dto.UserFullInfoDTO;
import me.fengorz.kiwi.upms.api.entity.SysUser;
import me.fengorz.kiwi.upms.service.SysUserService;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;
import java.util.Optional;

/**
 * 用户表
 *
 * @author zhanshifeng
 * @date 2019-09-26 09:37:54
 */
@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/sys/user")
public class SysUserController extends BaseController {

    private final SysUserService sysUserService;

    @GetMapping("/oneClickRegister")
    public R<SysUser> oneClickRegister() {
        log.info("oneClickRegister called");
        return R.success(sysUserService.oneClickRegister());
    }

    @GetMapping("/current/info")
    public R<UserFullInfoDTO> currentInfo() {
        log.info("currentInfo called");
        final SysUser[] user = {null};
        Optional.ofNullable(this.getCurrentUser()).ifPresent(inspectUser -> {
            String username = inspectUser.getUsername();
            log.debug("Fetching user details for: {}", username);
            user[0] = sysUserService.getOne(Wrappers.<SysUser>query().lambda().eq(SysUser::getUsername, username));
        });
        if (Objects.isNull(user[0])) {
            log.warn("Failed to get current user info");
            return R.failed("获取当前用户信息失败");
        }
        log.info("Successfully retrieved user info for: {}", user[0].getUsername());
        return R.success(sysUserService.getUserFullInfo(user[0]));
    }

    // TODO ZSF Add permission verification.
    @GetMapping("/info/{username}")
    public R<UserFullInfoDTO> info(@PathVariable String username) {
        log.info("Fetching info for username: {}", username);
        SysUser user = sysUserService.getOne(Wrappers.<SysUser>query().lambda().eq(SysUser::getUsername, username));
        if (Objects.isNull(user)) {
            log.warn("User not found with username: {}", username);
            return R.failed("用户信息查询不到 %s");
        }
        log.debug("Successfully retrieved user: {}", username);
        return R.success(sysUserService.getUserFullInfo(user));
    }

    /**
     * 分页查询
     *
     * @param page 分页对象
     * @param sysUser 用户表
     * @return
     */
    @GetMapping("/page")
    public R getSysUserPage(Page page, SysUser sysUser) {
        log.info("Fetching user page - Page: {}, User filter: {}", page.getCurrent(), sysUser);
        return R.success(sysUserService.page(page, Wrappers.query(sysUser)));
    }

    /**
     * 通过id查询用户表
     *
     * @param userId id
     * @return R
     */
    @GetMapping("/{userId}")
    public R getById(@PathVariable("userId") Integer userId) {
        log.info("Fetching user by ID: {}", userId);
        SysUser user = sysUserService.getById(userId);
        if (user == null) {
            log.warn("User not found with ID: {}", userId);
        }
        return R.success(user);
    }

    /**
     * 新增用户表
     *
     * @param sysUser 用户表
     * @return R
     */
    @LogMarker("新增用户表")
    @PostMapping
    public R save(@RequestBody SysUser sysUser) {
        log.info("Creating new user: {}", sysUser.getUsername());
        boolean result = sysUserService.save(sysUser);
        log.info("User creation {} for: {}", result ? "succeeded" : "failed", sysUser.getUsername());
        return R.success(result);
    }

    /**
     * 修改用户表
     *
     * @param sysUser 用户表
     * @return R
     */
    @LogMarker("修改用户表")
    @PutMapping
    public R updateById(@RequestBody SysUser sysUser) {
        log.info("Updating user with ID: {}", sysUser.getUserId());
        boolean result = sysUserService.updateById(sysUser);
        log.info("User update {} for ID: {}", result ? "succeeded" : "failed", sysUser.getUserId());
        return R.success(result);
    }

    /**
     * 通过id删除用户表
     *
     * @param userId id
     * @return R
     */
    @LogMarker("通过id删除用户表")
    @DeleteMapping("/{userId}")
    public R removeById(@PathVariable Integer userId) {
        log.info("Deleting user with ID: {}", userId);
        boolean result = sysUserService.removeById(userId);
        log.info("User deletion {} for ID: {}", result ? "succeeded" : "failed", userId);
        return R.success(result);
    }
}
