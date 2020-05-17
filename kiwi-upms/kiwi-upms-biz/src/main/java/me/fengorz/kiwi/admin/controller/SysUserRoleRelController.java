/*
 *
 *   Copyright [2019~2025] [codingByFeng]
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
import lombok.AllArgsConstructor;
import me.fengorz.kiwi.admin.api.entity.SysUserRoleRel;
import me.fengorz.kiwi.admin.service.SysUserRoleRelService;
import me.fengorz.kiwi.common.api.R;
import me.fengorz.kiwi.common.api.annotation.log.SysLog;
import me.fengorz.kiwi.common.sdk.controller.BaseController;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


/**
 * 用户角色表
 *
 * @author codingByFeng
 * @date 2019-09-26 14:39:35
 */
@RestController
@AllArgsConstructor
@RequestMapping("/sys/user/role/rel")
public class SysUserRoleRelController extends BaseController {

    private final SysUserRoleRelService sysUserRoleRelService;

    /**
     * 分页查询
     *
     * @param page           分页对象
     * @param sysUserRoleRel 用户角色表
     * @return
     */
    @GetMapping("/page")
    public R getSysUserRoleRelPage(Page page, SysUserRoleRel sysUserRoleRel) {
        return R.ok(sysUserRoleRelService.page(page, Wrappers.query(sysUserRoleRel)));
    }


    /**
     * 通过id查询用户角色表
     *
     * @param userId id
     * @return R
     */
    @GetMapping("/{userId}")
    public R getById(@PathVariable("userId") Integer userId) {
        return R.ok(sysUserRoleRelService.getById(userId));
    }

    /**
     * 新增用户角色表
     *
     * @param sysUserRoleRel 用户角色表
     * @return R
     */
    @SysLog("新增用户角色表")
    @PostMapping
    @PreAuthorize("@pms.hasPermission('admin_sysuserrolerel_add')")
    public R save(@RequestBody SysUserRoleRel sysUserRoleRel) {
        return R.ok(sysUserRoleRelService.save(sysUserRoleRel));
    }

    /**
     * 修改用户角色表
     *
     * @param sysUserRoleRel 用户角色表
     * @return R
     */
    @SysLog("修改用户角色表")
    @PutMapping
    @PreAuthorize("@pms.hasPermission('admin_sysuserrolerel_edit')")
    public R updateById(@RequestBody SysUserRoleRel sysUserRoleRel) {
        return R.ok(sysUserRoleRelService.updateById(sysUserRoleRel));
    }

    /**
     * 通过id删除用户角色表
     *
     * @param userId id
     * @return R
     */
    @SysLog("通过id删除用户角色表")
    @DeleteMapping("/{userId}")
    @PreAuthorize("@pms.hasPermission('admin_sysuserrolerel_del')")
    public R removeById(@PathVariable Integer userId) {
        return R.ok(sysUserRoleRelService.removeById(userId));
    }
}
