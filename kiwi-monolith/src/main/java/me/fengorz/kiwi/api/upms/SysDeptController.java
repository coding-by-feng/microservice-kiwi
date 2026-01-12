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
import me.fengorz.kiwi.domain.upms.entity.SysDept;
import me.fengorz.kiwi.domain.upms.service.SysDeptService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * System Department Controller
 *
 * @author codingByFeng
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sys/dept")
@Tag(name = "System Department", description = "System department management operations")
public class SysDeptController {

    private final SysDeptService sysDeptService;

    @GetMapping("/page")
    @Operation(summary = "Get departments with pagination")
    public R<Page<SysDept>> getPage(
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer size) {
        Page<SysDept> pageRequest = new Page<>(page, size);
        return R.ok(sysDeptService.page(pageRequest));
    }

    @GetMapping("/{deptId}")
    @Operation(summary = "Get department by ID")
    public R<SysDept> getById(@PathVariable Integer deptId) {
        return sysDeptService.findById(deptId)
                .map(R::ok)
                .orElse(R.failed("Department not found"));
    }

    @GetMapping("/parent/{parentId}")
    @Operation(summary = "Get departments by parent ID")
    public R<List<SysDept>> getByParentId(@PathVariable Integer parentId) {
        return R.ok(sysDeptService.findByParentId(parentId));
    }

    @GetMapping("/tree")
    @Operation(summary = "Get department tree")
    public R<List<SysDept>> getTree() {
        return R.ok(sysDeptService.findRootDepartments());
    }

    @PostMapping
    @Operation(summary = "Create new department")
    @PreAuthorize("hasRole('ADMIN')")
    public R<SysDept> save(@RequestBody SysDept sysDept) {
        return R.ok(sysDeptService.saveDept(sysDept));
    }

    @PutMapping("/{deptId}")
    @Operation(summary = "Update department")
    @PreAuthorize("hasRole('ADMIN')")
    public R<SysDept> update(@PathVariable Integer deptId, @RequestBody SysDept sysDept) {
        return sysDeptService.findById(deptId)
                .map(existing -> {
                    sysDept.setDeptId(deptId);
                    return R.ok(sysDeptService.saveDept(sysDept));
                })
                .orElse(R.failed("Department not found"));
    }

    @DeleteMapping("/{deptId}")
    @Operation(summary = "Delete department")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> delete(@PathVariable Integer deptId) {
        sysDeptService.deleteDeptById(deptId);
        return R.ok();
    }
}
