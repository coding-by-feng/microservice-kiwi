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
import me.fengorz.kiwi.bdf.core.service.ISeqService;
import me.fengorz.kiwi.common.api.R;
import me.fengorz.kiwi.common.api.annotation.log.SysLog;
import me.fengorz.kiwi.common.api.constant.MapperConstant;
import me.fengorz.kiwi.common.api.exception.ServiceException;
import me.fengorz.kiwi.common.sdk.controller.BaseController;
import me.fengorz.kiwi.common.sdk.web.security.SecurityUtils;
import me.fengorz.kiwi.word.api.entity.WordStarListDO;
import me.fengorz.kiwi.word.api.vo.WordStarListVO;
import me.fengorz.kiwi.word.api.vo.star.WordStarItemVO;
import me.fengorz.kiwi.word.biz.service.base.IWordStarListService;
import me.fengorz.kiwi.word.biz.service.base.IWordStarRelService;
import me.fengorz.kiwi.word.biz.service.operate.IOperateService;
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
 * @date 2019-12-08 23:26:57
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/word/star/list")
public class WordStarListController extends BaseController {

    private final IWordStarListService starListService;
    private final IOperateService operateService;
    private final IWordStarRelService relService;
    private final ISeqService seqService;

    /**
     * 新增单词本
     *
     * @param vo 单词本
     * @return R
     */
    @SysLog("新增单词本")
    @PostMapping("/save")
    // @PreAuthorize("@pms.hasPermission('api_wordstarlist_add')")
    public R<Boolean> save(WordStarListVO vo) {
        vo.setOwner(SecurityUtils.getCurrentUserId());
        vo.setId(seqService.genIntSequence(MapperConstant.T_INS_SEQUENCE));
        return R.success(starListService.save(vo));
    }

    /**
     * 修改单词本
     *
     * @param wordStarListDO 单词本
     * @return R
     */
    @SysLog("修改单词本")
    @PostMapping("/updateById")
    // @PreAuthorize("@pms.hasPermission('api_wordstarlist_edit')")
    public R<Boolean> updateById(WordStarListDO wordStarListDO) {
        return R.success(starListService.updateById(wordStarListDO));
    }

    /**
     * 通过id删除单词本
     *
     * @param id id
     * @return R
     */
    @SysLog("通过id删除单词本")
    @PostMapping("/del/{id}")
    // @PreAuthorize("@pms.hasPermission('api_wordstarlist_del')")
    public R<Boolean> del(@PathVariable Integer id) {
        return R.success(starListService.removeById(id));
    }

    @GetMapping("/getCurrentUserList")
    public R<List<WordStarListVO>> getCurrentUserList() {
        return R.success(starListService.getCurrentUserList(SecurityUtils.getCurrentUserId()));
    }

    @PostMapping("/getListItems/{size}/{current}")
    public R<IPage<WordStarItemVO>> getListItems(@NotNull Integer listId, @PathVariable @Min(0) Integer current,
                                                 @PathVariable @Range(min = 1, max = 100) Integer size) {
        return R.success(starListService.getListItems(new Page(current, size), listId));
    }

    @PostMapping("/putWordStarList")
    public R<Boolean> putWordStarList(@NotNull Integer wordId, @NotNull Integer listId) throws ServiceException {
        starListService.putIntoStarList(wordId, listId);
        return R.success();
    }

    @PostMapping("/removeWordStarList")
    public R<Boolean> removeWordStarList(@NotNull Integer wordId, @NotNull Integer listId) throws ServiceException {
        starListService.removeStarList(wordId, listId);
        return R.success();
    }

    @GetMapping("/findAllWordId/{listId}")
    public R<List<Integer>> findAllWordName(@PathVariable Integer listId) {
        return R.success(relService.findAllWordId(listId));
    }
}
