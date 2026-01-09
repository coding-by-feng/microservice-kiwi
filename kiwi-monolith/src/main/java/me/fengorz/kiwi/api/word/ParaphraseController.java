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
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.R;
import me.fengorz.kiwi.domain.word.dto.ParaphraseRequest;
import me.fengorz.kiwi.domain.word.entity.ParaphraseStarList;
import me.fengorz.kiwi.domain.word.entity.ParaphraseStarRel;
import me.fengorz.kiwi.domain.word.service.ParaphraseService;
import me.fengorz.kiwi.domain.word.service.ParaphraseStarListService;
import me.fengorz.kiwi.domain.word.service.ParaphraseStarRelService;
import me.fengorz.kiwi.domain.word.service.WordQueryService;
import me.fengorz.kiwi.domain.word.vo.ParaphraseStarListVO;
import me.fengorz.kiwi.domain.word.vo.ParaphraseVO;
import me.fengorz.kiwi.security.KiwiUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Paraphrase Controller
 * Aligned with original microservice ParaphraseController
 *
 * @author codingByFeng
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/word/paraphrase")
@Tag(name = "Paraphrase", description = "Word paraphrase management operations")
public class ParaphraseController {

    private final ParaphraseService paraphraseService;
    private final WordQueryService wordQueryService;
    private final ParaphraseStarListService paraphraseStarListService;
    private final ParaphraseStarRelService paraphraseStarRelService;

    /**
     * Modify paraphrase Chinese meaning
     */
    @PutMapping("/modifyMeaningChinese")
    @Operation(summary = "Modify paraphrase Chinese meaning")
    public R<Boolean> modifyMeaningChinese(@Valid ParaphraseRequest request) {
        boolean success = wordQueryService.updateMeaningChinese(
                request.getParaphraseId(),
                request.getMeaningChinese()
        );
        return R.ok(success);
    }

    /**
     * Get paraphrase detail by ID
     */
    @GetMapping("/star/list/getItemDetail/{paraphraseId}")
    @Operation(summary = "Get paraphrase detail")
    public R<ParaphraseVO> getItemDetail(@PathVariable Integer paraphraseId) {
        log.info("Querying paraphraseId={}", paraphraseId);
        return wordQueryService.findParaphraseVO(paraphraseId)
                .map(R::ok)
                .orElse(R.failed("Paraphrase not found"));
    }

    /**
     * Get paraphrase by ID (simple version)
     */
    @GetMapping("/{paraphraseId}")
    @Operation(summary = "Get paraphrase by ID")
    public R<ParaphraseVO> getById(@PathVariable Integer paraphraseId) {
        log.info("Querying paraphraseId={}", paraphraseId);
        return wordQueryService.findParaphraseVO(paraphraseId)
                .map(R::ok)
                .orElse(R.failed("Paraphrase not found"));
    }

    // ==================== Paraphrase Star List Operations ====================

    /**
     * Save paraphrase star list
     */
    @PostMapping("/star/list/save")
    @Operation(summary = "Create new paraphrase star list")
    public R<Boolean> saveStarList(
            @RequestParam String listName,
            @RequestParam(required = false) String remark,
            @AuthenticationPrincipal KiwiUser user) {
        log.info("Creating paraphrase star list: {} for user: {}", listName, user.getUserId());
        ParaphraseStarList list = paraphraseStarListService.create(listName, remark, user.getUserId());
        return R.ok(list != null);
    }

    /**
     * Update paraphrase star list
     */
    @PutMapping("/star/list/updateById")
    @Operation(summary = "Update paraphrase star list")
    public R<Boolean> updateStarList(
            @RequestParam Integer id,
            @RequestParam(required = false) String listName,
            @RequestParam(required = false) String remark) {
        log.info("Updating paraphrase star list: {}", id);
        boolean success = paraphraseStarListService.updateList(id, listName, remark);
        return R.ok(success);
    }

    /**
     * Delete paraphrase star list by ID
     */
    @DeleteMapping("/star/list/delById/{id}")
    @Operation(summary = "Delete paraphrase star list")
    public R<Boolean> deleteStarList(@PathVariable Integer id) {
        log.info("Deleting paraphrase star list: {}", id);
        boolean success = paraphraseStarListService.deleteById(id);
        return R.ok(success);
    }

    /**
     * Get current user's paraphrase star lists
     */
    @GetMapping("/star/list/getCurrentUserList")
    @Operation(summary = "Get current user's paraphrase star lists")
    public R<List<ParaphraseStarListVO>> getCurrentUserList(@AuthenticationPrincipal KiwiUser user) {
        List<ParaphraseStarList> lists = paraphraseStarListService.findByOwner(user.getUserId());
        List<ParaphraseStarListVO> voList = lists.stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        return R.ok(voList);
    }

    /**
     * Put paraphrase into star list
     */
    @PutMapping("/star/list/putIntoStarList")
    @Operation(summary = "Add paraphrase to star list")
    public R<Boolean> putIntoStarList(
            @NotNull @RequestParam Integer paraphraseId,
            @NotNull @RequestParam Integer listId) {
        log.info("Adding paraphrase {} to list {}", paraphraseId, listId);
        boolean success = paraphraseStarRelService.putIntoStarList(paraphraseId, listId);
        return R.ok(success);
    }

    /**
     * Get star list items with pagination
     */
    @GetMapping("/star/list/getListItems/{size}/{current}/{listId}")
    @Operation(summary = "Get paraphrase star list items")
    public R<Page<ParaphraseVO>> getListItems(
            @PathVariable @Min(0) Integer current,
            @PathVariable @Min(1) @Max(100) Integer size,
            @PathVariable Integer listId) {
        List<ParaphraseStarRel> relations = paraphraseStarRelService.findByListId(listId);
        return R.ok(toParaphraseVOPage(relations, current, size));
    }

    /**
     * Get review list items (not remembered yet)
     */
    @GetMapping("/star/list/getReviewListItems/{size}/{current}/{listId}")
    @Operation(summary = "Get review list items")
    public R<Page<ParaphraseVO>> getReviewListItems(
            @PathVariable @Min(0) Integer current,
            @PathVariable @Min(1) @Max(100) Integer size,
            @PathVariable Integer listId) {
        List<ParaphraseStarRel> relations = paraphraseStarRelService.findReviewItems(listId);
        return R.ok(toParaphraseVOPage(relations, current, size));
    }

    /**
     * Get remember list items (remembered but not kept in mind)
     */
    @GetMapping("/star/list/getRememberListItems/{size}/{current}/{listId}")
    @Operation(summary = "Get remember list items")
    public R<Page<ParaphraseVO>> getRememberListItems(
            @PathVariable @Min(0) Integer current,
            @PathVariable @Min(1) @Max(100) Integer size,
            @PathVariable Integer listId) {
        List<ParaphraseStarRel> relations = paraphraseStarRelService.findRememberItems(listId);
        return R.ok(toParaphraseVOPage(relations, current, size));
    }

    /**
     * Remember one paraphrase
     */
    @PutMapping("/star/list/rememberOne")
    @Operation(summary = "Mark paraphrase as remembered")
    public R<Void> rememberOne(
            @NotNull @RequestParam Integer paraphraseId,
            @NotNull @RequestParam Integer listId,
            @AuthenticationPrincipal KiwiUser user) {
        log.info("User {} remembering paraphrase {} in list {}", user.getUserId(), paraphraseId, listId);
        paraphraseStarRelService.rememberOne(paraphraseId, listId);
        return R.ok();
    }

    /**
     * Keep in mind one paraphrase
     */
    @PutMapping("/star/list/keepInMind")
    @Operation(summary = "Keep paraphrase in mind")
    public R<Void> keepInMind(
            @NotNull @RequestParam Integer paraphraseId,
            @NotNull @RequestParam Integer listId,
            @AuthenticationPrincipal KiwiUser user) {
        log.info("User {} keeping in mind paraphrase {} in list {}", user.getUserId(), paraphraseId, listId);
        paraphraseStarRelService.keepInMind(paraphraseId, listId);
        return R.ok();
    }

    /**
     * Forget one paraphrase
     */
    @PutMapping("/star/list/forgetOne")
    @Operation(summary = "Mark paraphrase as forgotten")
    public R<Void> forgetOne(
            @NotNull @RequestParam Integer paraphraseId,
            @NotNull @RequestParam Integer listId) {
        log.info("Forgetting paraphrase {} in list {}", paraphraseId, listId);
        paraphraseStarRelService.forgetOne(paraphraseId, listId);
        return R.ok();
    }

    /**
     * Remove paraphrase from star list
     */
    @DeleteMapping("/star/list/removeParaphraseStar")
    @Operation(summary = "Remove paraphrase from star list")
    public R<Boolean> removeParaphraseStar(
            @NotNull @RequestParam Integer paraphraseId,
            @NotNull @RequestParam Integer listId) {
        log.info("Removing paraphrase {} from list {}", paraphraseId, listId);
        boolean success = paraphraseStarRelService.removeFromStarList(paraphraseId, listId);
        return R.ok(success);
    }

    // ==================== Helper Methods ====================

    private ParaphraseStarListVO toVO(ParaphraseStarList entity) {
        return new ParaphraseStarListVO()
                .setId(entity.getId())
                .setListName(entity.getListName())
                .setRemark(entity.getRemark())
                .setOwner(entity.getOwner())
                .setSort(entity.getSort())
                .setCreateTime(entity.getCreateTime());
    }

    private Page<ParaphraseVO> toParaphraseVOPage(List<ParaphraseStarRel> relations, int current, int size) {
        // Manual pagination
        int start = current * size;
        int end = Math.min(start + size, relations.size());

        List<ParaphraseVO> content;
        if (start >= relations.size()) {
            content = List.of();
        } else {
            content = relations.subList(start, end).stream()
                    .map(rel -> wordQueryService.findParaphraseVO(rel.getParaphraseId()).orElse(null))
                    .filter(vo -> vo != null)
                    .collect(Collectors.toList());
        }

        return new PageImpl<>(content, PageRequest.of(current, size), relations.size());
    }
}
