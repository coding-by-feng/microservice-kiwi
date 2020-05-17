/*
 *
 *   Copyright [2019~2025] [codingByFeng]
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

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.api.R;
import me.fengorz.kiwi.common.api.annotation.log.SysLog;
import me.fengorz.kiwi.common.api.exception.ServiceException;
import me.fengorz.kiwi.common.sdk.controller.BaseController;
import me.fengorz.kiwi.word.api.entity.WordParaphraseStarListDO;
import me.fengorz.kiwi.word.biz.service.IWordParaphraseService;
import me.fengorz.kiwi.word.biz.service.IWordParaphraseStarListService;
import me.fengorz.kiwi.word.biz.service.operate.IWordOperateService;
import org.hibernate.validator.constraints.Range;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;


/**
 * 单词本
 *
 * @author codingByFeng
 * @date 2019-12-08 23:27:41
 */
@RestController
@AllArgsConstructor
@RequestMapping("/word/paraphrase/star/list")
@Validated
@Slf4j
public class WordParaphraseStarListController extends BaseController {

    private final IWordParaphraseStarListService wordParaphraseStarListService;
    private final IWordOperateService wordOperateService;
    private final IWordParaphraseService wordParaphraseService;

    /**
     * 分页查询
     *
     * @param page                     分页对象
     * @param wordParaphraseStarListDO 单词本
     * @return
     */
    @GetMapping("/page")
    public R getWordParaphraseStarListPage(Page page, WordParaphraseStarListDO wordParaphraseStarListDO) {
        return R.ok(wordParaphraseStarListService.page(page, Wrappers.query(wordParaphraseStarListDO)));
    }

    /**
     * 根据条件查询单个实体
     *
     * @param condition
     * @return
     */
    @PostMapping("/getOne")
    public R getOne(@RequestBody WordParaphraseStarListDO condition) {
        return R.ok(wordParaphraseStarListService.getOne(new QueryWrapper<>(condition)));
    }


    /**
     * 通过id查询单词本
     *
     * @param id id
     * @return R
     */
    @GetMapping("/getById/{id}")
    public R getById(@PathVariable("id") Integer id) {
        return R.ok(wordParaphraseStarListService.getById(id));
    }

    /**
     * 新增单词本
     *
     * @param wordParaphraseStarListDO 单词本
     * @return R
     */
    @SysLog("新增单词本")
    @PostMapping("/save")
    // @PreAuthorize("@pms.hasPermission('api_wordparaphrasestarlist_add')")
    public R save(WordParaphraseStarListDO wordParaphraseStarListDO) {
        wordParaphraseStarListDO.setOwner(1);
        return R.ok(wordParaphraseStarListService.save(wordParaphraseStarListDO));
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
    public R updateById(WordParaphraseStarListDO wordParaphraseStarListDO) {
        return R.ok(wordParaphraseStarListService.updateById(wordParaphraseStarListDO));
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
    public R delById(@PathVariable Integer id) {
        return R.ok(wordParaphraseStarListService.removeById(id));
    }

    @GetMapping("/getCurrentUserList")
    public R getCurrentUserList() {
        return wordParaphraseStarListService.getCurrentUserList(1);
    }

    @PostMapping("/putIntoStarList")
    public R putIntoStarList(@NotNull Integer paraphraseId, @NotNull Integer listId) {
        try {
            return R.ok(this.wordOperateService.putParaphraseIntoStarList(paraphraseId, listId));
        } catch (ServiceException e) {
            log.error(e.getMessage());
            return R.failed(e.getMessage());
        }
    }

    @PostMapping("/getListItems/{size}/{current}")
    public R getListItems(@NotNull Integer listId,
                          @PathVariable @Min(1) Integer current,
                          @PathVariable @Range(min = 1, max = 100) Integer size) {
        return R.ok(wordParaphraseStarListService.getListItems(new Page(current, size), listId));
    }

    @GetMapping("/getItemDetail/{paraphraseId}")
    public R getItemDetail(@PathVariable Integer paraphraseId) {
        return R.ok(wordParaphraseService.findWordParaphraseVO(paraphraseId));
    }

    @PostMapping("/removeParaphraseStar")
    public R removeParaphraseStar(@NotNull Integer paraphraseId, @NotNull Integer listId) throws ServiceException {
        return R.ok(this.wordParaphraseStarListService.removeParaphraseStar(paraphraseId, listId));
    }

}
