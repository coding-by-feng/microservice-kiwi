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
import me.fengorz.kiwi.bdf.core.service.ISeqService;
import me.fengorz.kiwi.common.api.R;
import me.fengorz.kiwi.common.api.annotation.log.SysLog;
import me.fengorz.kiwi.common.api.constant.MapperConstant;
import me.fengorz.kiwi.common.api.exception.ServiceException;
import me.fengorz.kiwi.common.sdk.controller.BaseController;
import me.fengorz.kiwi.common.sdk.web.security.SecurityUtils;
import me.fengorz.kiwi.word.api.entity.WordExampleStarListDO;
import me.fengorz.kiwi.word.api.vo.WordExampleStarListVO;
import me.fengorz.kiwi.word.api.vo.star.ExampleStarItemVO;
import me.fengorz.kiwi.word.biz.service.base.IWordExampleStarListService;
import me.fengorz.kiwi.word.biz.service.operate.IWordOperateService;

/**
 * @author zhanshifeng
 * @date 2019-12-08 23:27:12
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/word/example/star/list")
@Validated
@Slf4j
public class WordExampleStarListController extends BaseController {

    private final IWordExampleStarListService wordExampleStarListService;
    private final IWordOperateService wordOperateService;
    private final ISeqService seqService;

    /**
     * 新增
     *
     * @param vo
     * @return R
     */
    @SysLog("新增")
    @PostMapping("/save")
    // @PreAuthorize("@pms.hasPermission('api_wordparaphraseexamplelist_add')")
    public R<Boolean> save(WordExampleStarListVO vo) {
        vo.setOwner(SecurityUtils.getCurrentUserId());
        vo.setId(seqService.genIntSequence(MapperConstant.T_INS_SEQUENCE));
        return R.success(wordExampleStarListService.save(vo));
    }

    /**
     * 修改
     *
     * @param wordExampleStarListDO
     * @return R
     */
    @SysLog("修改")
    @PostMapping("/updateById")
    // @PreAuthorize("@pms.hasPermission('api_wordparaphraseexamplelist_edit')")
    public R<Boolean> updateById(WordExampleStarListDO wordExampleStarListDO) {
        return R.success(wordExampleStarListService.updateById(wordExampleStarListDO));
    }

    /**
     * 通过id删除
     *
     * @param id
     *            id
     * @return R
     */
    @SysLog("通过id删除")
    @PostMapping("/delById/{id}")
    // @PreAuthorize("@pms.hasPermission('api_wordparaphraseexamplelist_del')")
    public R<Boolean> delById(@PathVariable Integer id) {
        return R.success(wordExampleStarListService.removeById(id));
    }

    @GetMapping("/getCurrentUserList")
    public R<List<WordExampleStarListVO>> getCurrentUserList() {
        return R.success(wordExampleStarListService.getCurrentUserList(SecurityUtils.getCurrentUserId()));
    }

    @PostMapping("/putIntoStarList")
    public R<Boolean> putIntoStarList(@NotNull Integer exampleId, @NotNull Integer listId) {
        try {
            return R.success(wordOperateService.putExampleIntoStarList(exampleId, listId));
        } catch (ServiceException e) {
            log.error(e.getMessage());
            return R.failed(e.getMessage());
        }
    }

    @PostMapping("/removeExampleStar")
    public R<Boolean> removeExampleStar(@NotNull Integer exampleId, @NotNull Integer listId) {
        return R.success(wordOperateService.removeExampleStar(exampleId, listId));
    }

    @PostMapping("/getListItems/{size}/{current}")
    public R<IPage<ExampleStarItemVO>> getListItems(@NotNull Integer listId, @PathVariable @Min(1) Integer current,
        @PathVariable @Range(min = 1, max = 100) Integer size) {
        return R.success(wordExampleStarListService.getListItems(new Page(current, size), listId));
    }

}
