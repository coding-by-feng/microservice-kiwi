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
package me.fengorz.kiwi.api.notes;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.R;
import me.fengorz.kiwi.domain.notes.dto.NotesCategoryRequest;
import me.fengorz.kiwi.domain.notes.service.NotesCategoryService;
import me.fengorz.kiwi.domain.notes.service.NotesLockService;
import me.fengorz.kiwi.domain.notes.vo.NotesCategoryVO;
import me.fengorz.kiwi.security.KiwiUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Notes Category Controller
 *
 * @author codingByFeng
 */
@Slf4j
@RestController
@RequestMapping("/api/notes/category")
@RequiredArgsConstructor
@Tag(name = "Notes Category", description = "Private notes category management")
public class NotesCategoryController {

    private final NotesCategoryService categoryService;
    private final NotesLockService lockService;

    @GetMapping("/list")
    @Operation(summary = "List user's note categories")
    public R<List<NotesCategoryVO>> listCategories(@AuthenticationPrincipal KiwiUser user) {
        lockService.verifyUnlocked(user.getUserId());
        return R.ok(categoryService.listByUser(user.getUserId()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get category by ID")
    public R<NotesCategoryVO> getCategory(
            @AuthenticationPrincipal KiwiUser user,
            @PathVariable Long id) {
        lockService.verifyUnlocked(user.getUserId());
        NotesCategoryVO category = categoryService.getByIdAndUser(id, user.getUserId());
        if (category == null) {
            return R.failed("Category not found");
        }
        return R.ok(category);
    }

    @PostMapping
    @Operation(summary = "Create new category")
    public R<NotesCategoryVO> createCategory(
            @AuthenticationPrincipal KiwiUser user,
            @RequestBody @Validated NotesCategoryRequest request) {
        lockService.verifyUnlocked(user.getUserId());
        return R.ok(categoryService.create(request, user.getUserId()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update category")
    public R<NotesCategoryVO> updateCategory(
            @AuthenticationPrincipal KiwiUser user,
            @PathVariable Long id,
            @RequestBody @Validated NotesCategoryRequest request) {
        lockService.verifyUnlocked(user.getUserId());
        return R.ok(categoryService.update(id, request, user.getUserId()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete category (physical delete)")
    public R<Void> deleteCategory(
            @AuthenticationPrincipal KiwiUser user,
            @PathVariable Long id) {
        lockService.verifyUnlocked(user.getUserId());
        categoryService.delete(id, user.getUserId());
        return R.ok();
    }
}
