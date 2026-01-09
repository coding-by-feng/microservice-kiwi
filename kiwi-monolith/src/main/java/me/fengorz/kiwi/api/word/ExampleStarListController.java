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
package me.fengorz.kiwi.api.word;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.R;
import me.fengorz.kiwi.domain.word.entity.ExampleStarList;
import me.fengorz.kiwi.domain.word.service.ExampleStarListService;
import me.fengorz.kiwi.domain.word.service.ExampleStarRelService;
import me.fengorz.kiwi.security.KiwiUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Example Star List Controller
 *
 * @author codingByFeng
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/word/example/star/list")
@Tag(name = "Example Star List", description = "Example star list management operations")
public class ExampleStarListController {

    private final ExampleStarListService exampleStarListService;
    private final ExampleStarRelService exampleStarRelService;

    @GetMapping
    @Operation(summary = "Get current user's example star lists")
    public R<List<ExampleStarList>> getCurrentUserList(@AuthenticationPrincipal KiwiUser user) {
        return R.ok(exampleStarListService.findByOwner(user.getUserId()));
    }

    @GetMapping("/{listId}")
    @Operation(summary = "Get example star list by ID")
    public R<ExampleStarList> getById(@PathVariable Integer listId) {
        return exampleStarListService.findById(listId)
                .map(R::ok)
                .orElse(R.failed("Example star list not found"));
    }

    @PostMapping
    @Operation(summary = "Create new example star list")
    public R<ExampleStarList> save(@RequestBody ExampleStarList exampleStarList,
                                    @AuthenticationPrincipal KiwiUser user) {
        exampleStarList.setOwner(user.getUserId());
        return R.ok(exampleStarListService.saveList(exampleStarList));
    }

    @PutMapping("/{listId}")
    @Operation(summary = "Update example star list")
    public R<ExampleStarList> update(@PathVariable Integer listId,
                                      @RequestBody ExampleStarList exampleStarList) {
        return exampleStarListService.findById(listId)
                .map(existing -> {
                    exampleStarList.setId(listId);
                    exampleStarList.setOwner(existing.getOwner());
                    return R.ok(exampleStarListService.saveList(exampleStarList));
                })
                .orElse(R.failed("Example star list not found"));
    }

    @DeleteMapping("/{listId}")
    @Operation(summary = "Delete example star list")
    public R<Void> delete(@PathVariable Integer listId) {
        exampleStarListService.deleteById(listId);
        return R.ok();
    }

    @PostMapping("/{listId}/example/{exampleId}")
    @Operation(summary = "Add example to star list")
    public R<Void> addExampleToList(@PathVariable Integer listId,
                                     @PathVariable Integer exampleId) {
        exampleStarRelService.addExampleToList(listId, exampleId);
        return R.ok();
    }

    @DeleteMapping("/{listId}/example/{exampleId}")
    @Operation(summary = "Remove example from star list")
    public R<Void> removeExampleFromList(@PathVariable Integer listId,
                                          @PathVariable Integer exampleId) {
        exampleStarRelService.removeExampleFromList(listId, exampleId);
        return R.ok();
    }
}
