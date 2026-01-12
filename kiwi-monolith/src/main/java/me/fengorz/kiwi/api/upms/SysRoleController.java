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
import me.fengorz.kiwi.domain.upms.service.SysRoleService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * System Role Controller
 *
 * @author codingByFeng
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sys/role")
@Tag(name = "System Role", description = "System role management operations")
public class SysRoleController {

    private final SysRoleService sysRoleService;

    @GetMapping("/page")
    @Operation(summary = "Get roles with pagination")
    public R<Page<SysRole>> getPage(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size) {
        Page<SysRole> page = new Page<>(current, size);
        return R.ok(sysRoleService.findAllRoles(page));
    }

    @GetMapping("/{roleId}")
    @Operation(summary = "Get role by ID")
    public R<SysRole> getById(@PathVariable Integer roleId) {
        return sysRoleService.findById(roleId)
                .map(R::ok)
                .orElse(R.failed("Role not found"));
    }

    @GetMapping("/code/{roleCode}")
    @Operation(summary = "Get role by code")
    public R<SysRole> getByCode(@PathVariable String roleCode) {
        return sysRoleService.findByRoleCode(roleCode)
                .map(R::ok)
                .orElse(R.failed("Role not found"));
    }

    @PostMapping
    @Operation(summary = "Create new role")
    @PreAuthorize("hasRole('ADMIN')")
    public R<SysRole> save(@RequestBody SysRole sysRole) {
        return R.ok(sysRoleService.saveRole(sysRole));
    }

    @PutMapping("/{roleId}")
    @Operation(summary = "Update role")
    @PreAuthorize("hasRole('ADMIN')")
    public R<SysRole> update(@PathVariable Integer roleId, @RequestBody SysRole sysRole) {
        return sysRoleService.findById(roleId)
                .map(existing -> {
                    sysRole.setRoleId(roleId);
                    return R.ok(sysRoleService.saveRole(sysRole));
                })
                .orElse(R.failed("Role not found"));
    }

    @DeleteMapping("/{roleId}")
    @Operation(summary = "Delete role")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> delete(@PathVariable Integer roleId) {
        sysRoleService.deleteById(roleId);
        return R.ok();
    }
}
