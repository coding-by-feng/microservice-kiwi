/*
 *
 *   Copyright [2019~2025] [zhanshifeng]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 *
 */
package me.fengorz.kiwi.word.biz.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.bdf.core.service.ISeqService;
import me.fengorz.kiwi.common.api.R;
import me.fengorz.kiwi.common.api.annotation.log.SysLog;
import me.fengorz.kiwi.common.api.constant.MapperConstant;
import me.fengorz.kiwi.common.api.exception.ServiceException;
import me.fengorz.kiwi.common.sdk.controller.BaseController;
import me.fengorz.kiwi.word.api.entity.WordParaphraseStarListDO;
import me.fengorz.kiwi.word.api.vo.WordParaphraseStarListVO;
import me.fengorz.kiwi.word.api.vo.detail.WordParaphraseVO;
import me.fengorz.kiwi.word.api.vo.star.ParaphraseStarItemVO;
import me.fengorz.kiwi.word.biz.service.IWordParaphraseService;
import me.fengorz.kiwi.word.biz.service.IWordParaphraseStarListService;
import me.fengorz.kiwi.word.biz.service.operate.IWordOperateService;
import org.hibernate.validator.constraints.Range;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.List;


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
public class WordParaphraseStarListController extends BaseController {

    private final IWordParaphraseStarListService wordParaphraseStarListService;
    private final IWordOperateService wordOperateService;
    private final IWordParaphraseService wordParaphraseService;
    private final ISeqService seqService;

    /**
     * 新增单词本
     *
     * @param vo 单词本
     * @return R
     */
    @SysLog("新增单词本")
    @PostMapping("/save")
    // @PreAuthorize("@pms.hasPermission('api_wordparaphrasestarlist_add')")
    public R<Boolean> save(WordParaphraseStarListVO vo) {
        vo.setOwner(1);
        vo.setId(seqService.genIntSequence(MapperConstant.T_INS_SEQUENCE));
        return R.success(wordParaphraseStarListService.save(vo));
    }

    /**
     * 修改单词本
     *
     * @param wordParaphraseStarListDO 单词本
     * @return R
     */
    @SysLog("修改单词本")
    @PostMapping("/updateById")
    // @PreAuthorize("@pms.hasPermission('api_wordparaphrasestarlist_edit')")
    public R<Boolean> updateById(WordParaphraseStarListDO wordParaphraseStarListDO) {
        return R.success(wordParaphraseStarListService.updateById(wordParaphraseStarListDO));
    }

    /**
     * 通过id删除单词本
     *
     * @param id id
     * @return R
     */
    @SysLog("通过id删除单词本")
    @PostMapping("/delById/{id}")
    // @PreAuthorize("@pms.hasPermission('api_wordparaphrasestarlist_del')")
    public R<Boolean> delById(@PathVariable Integer id) {
        return R.success(wordParaphraseStarListService.removeById(id));
    }

    @GetMapping("/getCurrentUserList")
    public R<List<WordParaphraseStarListVO>> getCurrentUserList() {
        return R.success(wordParaphraseStarListService.getCurrentUserList(1));
    }

    @PostMapping("/putIntoStarList")
    public R<Boolean> putIntoStarList(@NotNull Integer paraphraseId, @NotNull Integer listId) {
        try {
            return R.success(this.wordOperateService.putParaphraseIntoStarList(paraphraseId, listId));
        } catch (ServiceException e) {
            log.error(e.getMessage());
            return R.failed(e.getMessage());
        }
    }

    @PostMapping("/getListItems/{size}/{current}")
    public R<IPage<ParaphraseStarItemVO>> getListItems(@NotNull Integer listId,
                                                       @PathVariable @Min(1) Integer current,
                                                       @PathVariable @Range(min = 1, max = 100) Integer size) {
        return R.success(wordParaphraseStarListService.getListItems(new Page(current, size), listId));
    }

    @GetMapping("/getItemDetail/{paraphraseId}")
    public R<WordParaphraseVO> getItemDetail(@PathVariable Integer paraphraseId) {
        return R.success(wordOperateService.findWordParaphraseVO(paraphraseId));
    }

    @PostMapping("/removeParaphraseStar")
    public R<Integer> removeParaphraseStar(@NotNull Integer paraphraseId, @NotNull Integer listId) throws ServiceException {
        return R.success(this.wordParaphraseStarListService.removeParaphraseStar(paraphraseId, listId));
    }

}
