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
import me.fengorz.kiwi.admin.api.entity.SysRole;
import me.fengorz.kiwi.admin.service.SysRoleService;
import me.fengorz.kiwi.common.api.R;
import me.fengorz.kiwi.common.sdk.controller.BaseController;
import me.fengorz.kiwi.common.log.annotation.SysLog;
import lombok.AllArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


/**
 * 系统角色表
 *
 * @author codingByFeng
 * @date 2019-09-26 14:21:47
 */
@RestController
@AllArgsConstructor
@RequestMapping("/sys/role")
public class SysRoleController extends BaseController {

    private final SysRoleService sysRoleService;

    /**
     * 分页查询
     *
     * @param page    分页对象
     * @param sysRole 系统角色表
     * @return
     */
    @GetMapping("/page")
    public R getSysRolePage(Page page, SysRole sysRole) {
        return R.ok(sysRoleService.page(page, Wrappers.query(sysRole)));
    }


    /**
     * 通过id查询系统角色表
     *
     * @param roleId id
     * @return R
     */
    @GetMapping("/{roleId}")
    public R getById(@PathVariable("roleId") Integer roleId) {
        return R.ok(sysRoleService.getById(roleId));
    }

    /**
     * 新增系统角色表
     *
     * @param sysRole 系统角色表
     * @return R
     */
    @SysLog("新增系统角色表")
    @PostMapping
    @PreAuthorize("@pms.hasPermission('admin_sysrole_add')")
    public R save(@RequestBody SysRole sysRole) {
        return R.ok(sysRoleService.save(sysRole));
    }

    /**
     * 修改系统角色表
     *
     * @param sysRole 系统角色表
     * @return R
     */
    @SysLog("修改系统角色表")
    @PutMapping
    @PreAuthorize("@pms.hasPermission('admin_sysrole_edit')")
    public R updateById(@RequestBody SysRole sysRole) {
        return R.ok(sysRoleService.updateById(sysRole));
    }

    /**
     * 通过id删除系统角色表
     *
     * @param roleId id
     * @return R
     */
    @SysLog("通过id删除系统角色表")
    @DeleteMapping("/{roleId}")
    @PreAuthorize("@pms.hasPermission('admin_sysrole_del')")
    public R removeById(@PathVariable Integer roleId) {
        return R.ok(sysRoleService.removeById(roleId));
    }
}