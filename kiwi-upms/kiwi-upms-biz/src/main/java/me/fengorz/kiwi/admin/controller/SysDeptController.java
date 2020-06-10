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
package me.fengorz.kiwi.admin.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.api.R;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import lombok.RequiredArgsConstructor;
import me.fengorz.kiwi.admin.api.entity.SysDept;
import me.fengorz.kiwi.admin.service.SysDeptService;
import me.fengorz.kiwi.common.api.annotation.log.SysLog;
import me.fengorz.kiwi.common.sdk.controller.BaseController;

/**
 * 部门管理
 *
 * @author zhanshifeng
 * @date 2019-09-18 09:40:25
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/sys/dept")
public class SysDeptController extends BaseController {

    private final SysDeptService sysDeptService;

    /**
     * 分页查询
     *
     * @param page
     *            分页对象
     * @param sysDept
     *            部门管理
     * @return
     */
    @GetMapping("/page")
    public R getSysDeptPage(Page page, SysDept sysDept) {
        return R.ok(sysDeptService.page(page, Wrappers.query(sysDept)));
    }

    /**
     * 通过id查询部门管理
     *
     * @param deptId
     *            id
     * @return R
     */
    @GetMapping("/{deptId}")
    public R getById(@PathVariable("deptId") Integer deptId) {
        return R.ok(sysDeptService.getById(deptId));
    }

    /**
     * 新增部门管理
     *
     * @param sysDept
     *            部门管理
     * @return R
     */
    @SysLog("新增部门管理")
    @PostMapping
    @PreAuthorize("@pms.hasPermission('admin_sysdept_add')")
    public R save(@RequestBody SysDept sysDept) {
        return R.ok(sysDeptService.save(sysDept));
    }

    /**
     * 修改部门管理
     *
     * @param sysDept
     *            部门管理
     * @return R
     */
    @SysLog("修改部门管理")
    @PutMapping
    @PreAuthorize("@pms.hasPermission('admin_sysdept_edit')")
    public R updateById(@RequestBody SysDept sysDept) {
        return R.ok(sysDeptService.updateById(sysDept));
    }

    /**
     * 通过id删除部门管理
     *
     * @param deptId
     *            id
     * @return R
     */
    @SysLog("通过id删除部门管理")
    @DeleteMapping("/{deptId}")
    @PreAuthorize("@pms.hasPermission('admin_sysdept_del')")
    public R removeById(@PathVariable Integer deptId) {
        return R.ok(sysDeptService.removeById(deptId));
    }
}
