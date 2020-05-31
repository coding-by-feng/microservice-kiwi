/*
 *
 *   Copyright [2019~2025] [zhanshifeng]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 *
 */
package me.fengorz.kiwi.admin.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import me.fengorz.kiwi.admin.api.entity.SysUser;
import me.fengorz.kiwi.admin.service.SysUserService;
import me.fengorz.kiwi.common.api.R;
import me.fengorz.kiwi.common.api.annotation.log.SysLog;
import me.fengorz.kiwi.common.sdk.controller.BaseController;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequiredArgsConstructor
@RequestMapping("/sys/user")
public class SysUserController extends BaseController {

    private final SysUserService sysUserService;

    public SysUserService getSysUserService() {
        return sysUserService;
    }

    @GetMapping("/current/info")
    public R currentInfo() {
        final SysUser[] user = {null};
        Optional.ofNullable(this.getCurrentUser()).ifPresent(inspectUser -> {
            String username = inspectUser.getUsername();
            user[0] = sysUserService.getOne(Wrappers.<SysUser>query().lambda().eq(SysUser::getUsername, username));
        });
        if (Objects.isNull(user[0])) {
            return R.failed("获取当前用户信息失败");
        }
        return R.success(sysUserService.getUserFullInfo(user[0]));

    }

    @GetMapping("/info/{username}")
    public R info(@PathVariable String username) {
        SysUser user = sysUserService.getOne(Wrappers.<SysUser>query().lambda().eq(SysUser::getUsername, username));
        if (Objects.isNull(user)) {
            return R.failed("用户信息查询不到 %s" , username);
        }
        return R.success(sysUserService.getUserFullInfo(user));
    }

    /**
     * 分页查询
     *
     * @param page    分页对象
     * @param sysUser 用户表
     * @return
     */
    @GetMapping("/page")
    public R getSysUserPage(Page page, SysUser sysUser) {
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
        return R.success(sysUserService.getById(userId));
    }

    /**
     * 新增用户表
     *
     * @param sysUser 用户表
     * @return R
     */
    @SysLog("新增用户表")
    @PostMapping
    @PreAuthorize("@pms.hasPermission('admin_sysuser_add')")
    public R save(@RequestBody SysUser sysUser) {
        return R.success(sysUserService.save(sysUser));
    }

    /**
     * 修改用户表
     *
     * @param sysUser 用户表
     * @return R
     */
    @SysLog("修改用户表")
    @PutMapping
    @PreAuthorize("@pms.hasPermission('admin_sysuser_edit')")
    public R updateById(@RequestBody SysUser sysUser) {
        return R.success(sysUserService.updateById(sysUser));
    }

    /**
     * 通过id删除用户表
     *
     * @param userId id
     * @return R
     */
    @SysLog("通过id删除用户表")
    @DeleteMapping("/{userId}")
    @PreAuthorize("@pms.hasPermission('admin_sysuser_del')")
    public R removeById(@PathVariable Integer userId) {
        return R.success(sysUserService.removeById(userId));
    }
}
