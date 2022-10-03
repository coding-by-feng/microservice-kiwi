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
package me.fengorz.kiwi.word.biz.controller;

import java.util.List;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.Range;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.bdf.core.service.SeqService;
import me.fengorz.kiwi.common.api.R;
import me.fengorz.kiwi.common.sdk.annotation.log.LogMarker;
import me.fengorz.kiwi.common.sdk.controller.BaseController;
import me.fengorz.kiwi.common.sdk.web.security.SecurityUtils;
import me.fengorz.kiwi.word.api.entity.ParaphraseStarListDO;
import me.fengorz.kiwi.word.api.vo.ParaphraseStarListVO;
import me.fengorz.kiwi.word.api.vo.detail.ParaphraseVO;
import me.fengorz.kiwi.word.api.vo.star.ParaphraseStarItemVO;
import me.fengorz.kiwi.word.biz.service.base.ParaphraseService;
import me.fengorz.kiwi.word.biz.service.base.ParaphraseStarListService;
import me.fengorz.kiwi.word.biz.service.operate.OperateService;
import me.fengorz.kiwi.word.biz.service.operate.ReviewService;

/**
 * 单词本
 *
 * @author zhanshifeng
 * @date 2019-12-08 23:27:41
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/word/paraphrase/star/list")
@Validated
@Slf4j
public class ParaphraseStarListController extends BaseController {

    private final ParaphraseStarListService starListService;
    private final OperateService operateService;
    private final ParaphraseService paraphraseService;
    private final ReviewService reviewService;
    private final SeqService seqService;

    /**
     * 新增单词本
     *
     * @param vo 单词本
     * @return R
     */
    @PostMapping("/save")
    // @PreAuthorize("@pms.hasPermission('api_wordparaphrasestarlist_add')")
    public R<Boolean> save(ParaphraseStarListVO vo) {
        vo.setOwner(SecurityUtils.getCurrentUserId());
        vo.setId(seqService.genCommonIntSequence());
        return R.success(starListService.save(vo));
    }

    /**
     * 修改单词本
     *
     * @param paraphraseStarListDO 单词本
     * @return R
     */
    @PostMapping("/updateById")
    // @PreAuthorize("@pms.hasPermission('api_wordparaphrasestarlist_edit')")
    public R<Boolean> updateById(ParaphraseStarListDO paraphraseStarListDO) {
        return R.success(starListService.updateById(paraphraseStarListDO));
    }

    /**
     * 通过id删除单词本
     *
     * @param id id
     * @return R
     */
    @LogMarker("通过id删除单词本")
    @PostMapping("/delById/{id}")
    // @PreAuthorize("@pms.hasPermission('api_wordparaphrasestarlist_del')")
    public R<Boolean> delById(@PathVariable Integer id) {
        return R.success(starListService.removeById(id));
    }

    @GetMapping("/getCurrentUserList")
    public R<List<ParaphraseStarListVO>> getCurrentUserList() {
        return R.success(starListService.getCurrentUserList(SecurityUtils.getCurrentUserId()));
    }

    @PostMapping("/putIntoStarList")
    public R<Boolean> putIntoStarList(@NotNull Integer paraphraseId, @NotNull Integer listId) {
        starListService.putIntoStarList(paraphraseId, listId);
        return R.success();
    }

    @PostMapping("/getListItems/{size}/{current}")
    public R<IPage<ParaphraseStarItemVO>> getListItems(@NotNull Integer listId, @PathVariable @Min(0) Integer current,
        @PathVariable @Range(min = 1, max = 100) Integer size) {
        return R.success(starListService.selectListItems(new Page<>(current, size), listId));
    }

    @PostMapping("/getReviewListItems/{size}/{current}")
    public R<IPage<ParaphraseStarItemVO>> getReviewListItems(@NotNull Integer listId,
        @PathVariable @Min(0) Integer current, @PathVariable @Range(min = 1, max = 100) Integer size) {
        return R.success(starListService.selectReviewListItems(new Page<>(current, size), listId));
    }

    @PostMapping("/getRememberListItems/{size}/{current}")
    public R<IPage<ParaphraseStarItemVO>> getRememberListItems(@NotNull Integer listId,
        @PathVariable @Min(0) Integer current, @PathVariable @Range(min = 1, max = 100) Integer size) {
        return R.success(starListService.selectRememberListItems(new Page<>(current, size), listId));
    }

    @GetMapping("/getItemDetail/{paraphraseId}")
    public R<ParaphraseVO> getItemDetail(@PathVariable Integer paraphraseId) {
        // reviewService.increase(ReviseDailyCounterTypeEnum.REVIEW_COUNTER.getType(), SecurityUtils.getCurrentUserId());
        log.info("Querying paraphraseId={}", paraphraseId);
        return R.success(operateService.findParaphraseVO(paraphraseId));
    }

    @PostMapping("/rememberOne")
    public R<Void> rememberOne(@NotNull Integer paraphraseId, @NotNull Integer listId) {
        starListService.rememberOne(paraphraseId, listId);
        return R.success();
    }

    @PostMapping("/keepInMind")
    public R<Void> keepInMind(@NotNull Integer paraphraseId, @NotNull Integer listId) {
        starListService.keepInMind(paraphraseId, listId);
        return R.success();
    }

    @PostMapping("/forgetOne")
    public R<Void> forgetOne(@NotNull Integer paraphraseId, @NotNull Integer listId) {
        starListService.forgetOne(paraphraseId, listId);
        return R.success();
    }

    @PostMapping("/removeParaphraseStar")
    public R<Boolean> removeParaphraseStar(@NotNull Integer paraphraseId, @NotNull Integer listId) {
        starListService.removeParaphraseStar(paraphraseId, listId);
        return R.success();
    }
}
