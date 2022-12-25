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

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.bdf.core.service.SeqService;
import me.fengorz.kiwi.common.api.R;
import me.fengorz.kiwi.common.sdk.annotation.log.LogMarker;
import me.fengorz.kiwi.common.sdk.controller.BaseController;
import me.fengorz.kiwi.common.sdk.web.security.SecurityUtils;
import me.fengorz.kiwi.word.api.common.enumeration.ReviseDailyCounterTypeEnum;
import me.fengorz.kiwi.word.api.entity.ParaphraseStarListDO;
import me.fengorz.kiwi.word.api.request.ParaphraseRequest;
import me.fengorz.kiwi.word.api.vo.ParaphraseStarListVO;
import me.fengorz.kiwi.word.api.vo.detail.ParaphraseVO;
import me.fengorz.kiwi.word.api.vo.star.ParaphraseStarItemVO;
import me.fengorz.kiwi.word.biz.service.base.ParaphraseService;
import me.fengorz.kiwi.word.biz.service.base.ParaphraseStarListService;
import me.fengorz.kiwi.word.biz.service.operate.OperateService;
import me.fengorz.kiwi.word.biz.service.operate.ReviewService;
import org.hibernate.validator.constraints.Range;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 单词释义表
 *
 * @author zhanshifeng
 * @date 2019-10-31 20:39:48
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/word/paraphrase")
public class ParaphraseController extends BaseController {

    private final OperateService operateService;
    private final ParaphraseStarListService starListService;
    private final ParaphraseService paraphraseService;
    private final ReviewService reviewService;
    private final SeqService seqService;

    @LogMarker("修改单词释义")
    @PutMapping("/modifyMeaningChinese")
    public R<Boolean> modifyMeaningChinese(@Valid ParaphraseRequest request) {
        return R.success(operateService.modifyMeaningChinese(request));
    }

    /**
     * 新增单词本
     *
     * @param vo 单词本
     * @return R
     */
    @PostMapping("/star/list/save")
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
    @PutMapping("/star/list/updateById")
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
    @DeleteMapping("/star/list/delById/{id}")
    // @PreAuthorize("@pms.hasPermission('api_wordparaphrasestarlist_del')")
    public R<Boolean> delById(@PathVariable Integer id) {
        return R.success(starListService.removeById(id));
    }

    @GetMapping("/star/list/getCurrentUserList")
    public R<List<ParaphraseStarListVO>> getCurrentUserList() {
        return R.success(starListService.getCurrentUserList(SecurityUtils.getCurrentUserId()));
    }

    @PutMapping("/star/list/putIntoStarList")
    public R<Boolean> putIntoStarList(@NotNull Integer paraphraseId, @NotNull Integer listId) {
        starListService.putIntoStarList(paraphraseId, listId);
        return R.success();
    }

    @GetMapping("/star/list/getListItems/{size}/{current}/{listId}")
    public R<IPage<ParaphraseStarItemVO>> getListItems(@PathVariable @Min(0) Integer current,
                                                       @PathVariable @Range(min = 1, max = 100) Integer size,
                                                       @PathVariable Integer listId) {
        return R.success(starListService.selectListItems(new Page<>(current, size), listId));
    }

    @GetMapping("/star/list/getReviewListItems/{size}/{current}/{listId}")
    public R<IPage<ParaphraseStarItemVO>> getReviewListItems(@PathVariable @Min(0) Integer current,
                                                             @PathVariable @Range(min = 1, max = 100) Integer size,
                                                             @PathVariable Integer listId) {
        return R.success(starListService.selectReviewListItems(new Page<>(current, size), listId));
    }

    @GetMapping("/star/list/getRememberListItems/{size}/{current}/{listId}")
    public R<IPage<ParaphraseStarItemVO>> getRememberListItems(@PathVariable @Min(0) Integer current,
                                                               @PathVariable @Range(min = 1, max = 100) Integer size,
                                                               @PathVariable Integer listId) {
        return R.success(starListService.selectRememberListItems(new Page<>(current, size), listId));
    }

    @GetMapping("/star/list/getItemDetail/{paraphraseId}")
    public R<ParaphraseVO> getItemDetail(@PathVariable Integer paraphraseId) {
        log.info("Querying paraphraseId={}", paraphraseId);
        return R.success(operateService.findParaphraseVO(paraphraseId));
    }

    @PutMapping("/star/list/rememberOne")
    public R<Void> rememberOne(@NotNull Integer paraphraseId, @NotNull Integer listId) {
        reviewService.increase(ReviseDailyCounterTypeEnum.REMEMBER.getType(), SecurityUtils.getCurrentUserId());
        starListService.rememberOne(paraphraseId, listId);
        return R.success();
    }

    @PutMapping("/star/list/keepInMind")
    public R<Void> keepInMind(@NotNull Integer paraphraseId, @NotNull Integer listId) {
        reviewService.increase(ReviseDailyCounterTypeEnum.KEEP_IN_MIND.getType(), SecurityUtils.getCurrentUserId());
        starListService.keepInMind(paraphraseId, listId);
        return R.success();
    }

    @PutMapping("/star/list/forgetOne")
    public R<Void> forgetOne(@NotNull Integer paraphraseId, @NotNull Integer listId) {
        starListService.forgetOne(paraphraseId, listId);
        return R.success();
    }

    @DeleteMapping("/star/list/removeParaphraseStar")
    public R<Boolean> removeParaphraseStar(@NotNull Integer paraphraseId, @NotNull Integer listId) {
        starListService.removeParaphraseStar(paraphraseId, listId);
        return R.success();
    }

}
