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
import me.fengorz.kiwi.admin.api.entity.SysMenu;
import me.fengorz.kiwi.admin.service.SysMenuService;
import me.fengorz.kiwi.common.api.R;
import me.fengorz.kiwi.common.sdk.controller.BaseController;
import me.fengorz.kiwi.common.log.annotation.SysLog;
import lombok.AllArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


/**
 * 菜单权限表
 *
 * @author codingByFeng
 * @date 2019-09-26 15:59:10
 */
@RestController
@AllArgsConstructor
@RequestMapping("/sysmenu")
public class SysMenuController extends BaseController {

    private final SysMenuService sysMenuService;

    /**
     * 分页查询
     *
     * @param page    分页对象
     * @param sysMenu 菜单权限表
     * @return
     */
    @GetMapping("/page")
    public R getSysMenuPage(Page page, SysMenu sysMenu) {
        return R.ok(sysMenuService.page(page, Wrappers.query(sysMenu)));
    }


    /**
     * 通过id查询菜单权限表
     *
     * @param menuId id
     * @return R
     */
    @GetMapping("/{menuId}")
    public R getById(@PathVariable("menuId") Integer menuId) {
        return R.ok(sysMenuService.getById(menuId));
    }

    /**
     * 新增菜单权限表
     *
     * @param sysMenu 菜单权限表
     * @return R
     */
    @SysLog("新增菜单权限表")
    @PostMapping
    @PreAuthorize("@pms.hasPermission('admin_sysmenu_add')")
    public R save(@RequestBody SysMenu sysMenu) {
        return R.ok(sysMenuService.save(sysMenu));
    }

    /**
     * 修改菜单权限表
     *
     * @param sysMenu 菜单权限表
     * @return R
     */
    @SysLog("修改菜单权限表")
    @PutMapping
    @PreAuthorize("@pms.hasPermission('admin_sysmenu_edit')")
    public R updateById(@RequestBody SysMenu sysMenu) {
        return R.ok(sysMenuService.updateById(sysMenu));
    }

    /**
     * 通过id删除菜单权限表
     *
     * @param menuId id
     * @return R
     */
    @SysLog("通过id删除菜单权限表")
    @DeleteMapping("/{menuId}")
    @PreAuthorize("@pms.hasPermission('admin_sysmenu_del')")
    public R removeById(@PathVariable Integer menuId) {
        return R.ok(sysMenuService.removeById(menuId));
    }
}