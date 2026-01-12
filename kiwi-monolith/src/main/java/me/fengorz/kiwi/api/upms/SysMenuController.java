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
import me.fengorz.kiwi.domain.upms.entity.SysMenu;
import me.fengorz.kiwi.domain.upms.service.SysMenuService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * System Menu Controller
 *
 * @author codingByFeng
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sys/menu")
@Tag(name = "System Menu", description = "System menu management operations")
public class SysMenuController {

    private final SysMenuService sysMenuService;

    @GetMapping("/page")
    @Operation(summary = "Get menus with pagination")
    public R<Page<SysMenu>> getPage(
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer size) {
        Page<SysMenu> pageRequest = new Page<>(page, size);
        return R.ok(sysMenuService.page(pageRequest));
    }

    @GetMapping("/{menuId}")
    @Operation(summary = "Get menu by ID")
    public R<SysMenu> getById(@PathVariable Integer menuId) {
        return sysMenuService.findById(menuId)
                .map(R::ok)
                .orElse(R.failed("Menu not found"));
    }

    @GetMapping("/parent/{parentId}")
    @Operation(summary = "Get menus by parent ID")
    public R<List<SysMenu>> getByParentId(@PathVariable Integer parentId) {
        return R.ok(sysMenuService.findByParentId(parentId));
    }

    @GetMapping("/type/{type}")
    @Operation(summary = "Get menus by type")
    public R<List<SysMenu>> getByType(@PathVariable String type) {
        return R.ok(sysMenuService.findByType(type));
    }

    @PostMapping
    @Operation(summary = "Create new menu")
    @PreAuthorize("hasRole('ADMIN')")
    public R<SysMenu> save(@RequestBody SysMenu sysMenu) {
        return R.ok(sysMenuService.saveMenu(sysMenu));
    }

    @PutMapping("/{menuId}")
    @Operation(summary = "Update menu")
    @PreAuthorize("hasRole('ADMIN')")
    public R<SysMenu> update(@PathVariable Integer menuId, @RequestBody SysMenu sysMenu) {
        return sysMenuService.findById(menuId)
                .map(existing -> {
                    sysMenu.setMenuId(menuId);
                    return R.ok(sysMenuService.saveMenu(sysMenu));
                })
                .orElse(R.failed("Menu not found"));
    }

    @DeleteMapping("/{menuId}")
    @Operation(summary = "Delete menu")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> delete(@PathVariable Integer menuId) {
        sysMenuService.deleteById(menuId);
        return R.ok();
    }
}
