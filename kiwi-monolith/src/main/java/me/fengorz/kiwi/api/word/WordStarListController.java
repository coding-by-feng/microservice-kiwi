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

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.R;
import me.fengorz.kiwi.domain.word.dto.WordStarListRequest;
import me.fengorz.kiwi.domain.word.entity.WordStarList;
import me.fengorz.kiwi.domain.word.entity.WordStarRel;
import me.fengorz.kiwi.domain.word.mapper.WordVOMapper;
import me.fengorz.kiwi.domain.word.service.WordQueryService;
import me.fengorz.kiwi.domain.word.service.WordStarListService;
import me.fengorz.kiwi.domain.word.service.WordStarRelService;
import me.fengorz.kiwi.domain.word.vo.WordQueryVO;
import me.fengorz.kiwi.domain.word.vo.WordStarListVO;
import me.fengorz.kiwi.security.KiwiUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Word Star List Controller
 * Aligned with original microservice WordStarListController
 *
 * @author codingByFeng
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/word/star/list")
@Tag(name = "Word Star List", description = "Word star list management operations")
public class WordStarListController {

    private final WordStarListService wordStarListService;
    private final WordStarRelService wordStarRelService;
    private final WordQueryService wordQueryService;
    private final WordVOMapper wordVOMapper;

    /**
     * Save/Create new word star list
     */
    @PostMapping("/save")
    @Operation(summary = "Create new word star list")
    public R<Boolean> save(WordStarListVO vo, @AuthenticationPrincipal KiwiUser user) {
        WordStarList list = wordStarListService.create(
                vo.getListName(),
                vo.getRemark(),
                user.getUserId()
        );
        return R.ok(list != null);
    }

    /**
     * Update word star list by ID
     */
    @PutMapping("/updateById")
    @Operation(summary = "Update word star list")
    public R<Boolean> updateById(WordStarListRequest request) {
        return wordStarListService.findById(request.getId())
                .map(existing -> {
                    if (request.getListName() != null) {
                        existing.setListName(request.getListName());
                    }
                    if (request.getRemark() != null) {
                        existing.setRemark(request.getRemark());
                    }
                    if (request.getSort() != null) {
                        existing.setSort(request.getSort());
                    }
                    wordStarListService.save(existing);
                    return R.ok(true);
                })
                .orElse(R.failed("Star list not found"));
    }

    /**
     * Delete word star list by ID
     */
    @DeleteMapping("/del/{id}")
    @Operation(summary = "Delete word star list")
    public R<Boolean> del(@PathVariable Integer id) {
        wordStarListService.deleteById(id);
        return R.ok(true);
    }

    /**
     * Get current user's word star lists
     */
    @GetMapping("/getCurrentUserList")
    @Operation(summary = "Get current user's word star lists")
    public R<List<WordStarListVO>> getCurrentUserList(@AuthenticationPrincipal KiwiUser user) {
        List<WordStarList> lists = wordStarListService.findByOwner(user.getUserId());
        return R.ok(wordVOMapper.toWordStarListVOList(lists));
    }

    /**
     * Get word star list items with pagination
     */
    @GetMapping("/getListItems/{size}/{current}/{listId}")
    @Operation(summary = "Get word star list items")
    public R<Page<WordQueryVO>> getListItems(
            @PathVariable @Min(0) Integer current,
            @PathVariable @Min(1) @Max(100) Integer size,
            @PathVariable Integer listId) {

        Page<WordStarRel> page = new Page<>(current, size);
        Page<WordStarRel> relPage = wordStarRelService.findByListId(listId, page);

        List<WordQueryVO> voList = relPage.getRecords().stream()
                .map(rel -> wordQueryService.queryWordById(rel.getWordId()))
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .collect(Collectors.toList());

        Page<WordQueryVO> resultPage = new Page<>(current, size, relPage.getTotal());
        resultPage.setRecords(voList);
        return R.ok(resultPage);
    }

    /**
     * Put word into star list
     */
    @PutMapping("/putWordStarList")
    @Operation(summary = "Add word to star list")
    public R<Boolean> putWordStarList(
            @NotNull @RequestParam Integer wordId,
            @NotNull @RequestParam Integer listId) {
        wordStarRelService.addWordToList(listId, wordId);
        return R.ok(true);
    }

    /**
     * Remove word from star list
     */
    @DeleteMapping("/removeWordStarList")
    @Operation(summary = "Remove word from star list")
    public R<Boolean> removeWordStarList(
            @NotNull @RequestParam Integer wordId,
            @NotNull @RequestParam Integer listId) {
        wordStarRelService.removeWordFromList(listId, wordId);
        return R.ok(true);
    }

    /**
     * Find all word IDs in a list
     */
    @GetMapping("/findAllWordId/{listId}")
    @Operation(summary = "Find all word IDs in list")
    public R<List<Integer>> findAllWordId(@PathVariable Integer listId) {
        List<Integer> wordIds = wordStarRelService.findByListId(listId).stream()
                .map(WordStarRel::getWordId)
                .collect(Collectors.toList());
        return R.ok(wordIds);
    }

    // ==================== Additional endpoints for compatibility ====================

    /**
     * Get star list by ID
     */
    @GetMapping("/{listId}")
    @Operation(summary = "Get star list by ID")
    public R<WordStarListVO> getById(@PathVariable Integer listId) {
        return wordStarListService.findById(listId)
                .map(entity -> R.ok(wordVOMapper.toWordStarListVO(entity)))
                .orElse(R.failed("Star list not found"));
    }
}
