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
import me.fengorz.kiwi.common.api.R;
import me.fengorz.kiwi.common.sdk.annotation.log.LogMarker;
import me.fengorz.kiwi.common.sdk.controller.BaseController;
import me.fengorz.kiwi.upms.api.entity.SysRoleMenuRel;
import me.fengorz.kiwi.upms.service.SysRoleMenuRelService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 角色菜单表
 *
 * @author zhanshifeng
 * @date 2019-09-26 16:03:15
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/sysrolemenurel")
public class SysRoleMenuRelController extends BaseController {

    private final SysRoleMenuRelService sysRoleMenuRelService;

    /**
     * 分页查询
     *
     * @param page 分页对象
     * @param sysRoleMenuRel 角色菜单表
     * @return
     */
    @GetMapping("/page")
    public R getSysRoleMenuRelPage(Page page, SysRoleMenuRel sysRoleMenuRel) {
        return R.success(sysRoleMenuRelService.page(page, Wrappers.query(sysRoleMenuRel)));
    }

    /**
     * 通过id查询角色菜单表
     *
     * @param roleId id
     * @return R
     */
    @GetMapping("/{roleId}")
    public R getById(@PathVariable("roleId") Integer roleId) {
        return R.success(sysRoleMenuRelService.getById(roleId));
    }

    /**
     * 新增角色菜单表
     *
     * @param sysRoleMenuRel 角色菜单表
     * @return R
     */
    @LogMarker("新增角色菜单表")
    @PostMapping
    @PreAuthorize("@pms.hasPermission('admin_sysrolemenurel_add')")
    public R save(@RequestBody SysRoleMenuRel sysRoleMenuRel) {
        return R.success(sysRoleMenuRelService.save(sysRoleMenuRel));
    }

    /**
     * 修改角色菜单表
     *
     * @param sysRoleMenuRel 角色菜单表
     * @return R
     */
    @LogMarker("修改角色菜单表")
    @PutMapping
    @PreAuthorize("@pms.hasPermission('admin_sysrolemenurel_edit')")
    public R updateById(@RequestBody SysRoleMenuRel sysRoleMenuRel) {
        return R.success(sysRoleMenuRelService.updateById(sysRoleMenuRel));
    }

    /**
     * 通过id删除角色菜单表
     *
     * @param roleId id
     * @return R
     */
    @LogMarker("通过id删除角色菜单表")
    @DeleteMapping("/{roleId}")
    @PreAuthorize("@pms.hasPermission('admin_sysrolemenurel_del')")
    public R removeById(@PathVariable Integer roleId) {
        return R.success(sysRoleMenuRelService.removeById(roleId));
    }
}
