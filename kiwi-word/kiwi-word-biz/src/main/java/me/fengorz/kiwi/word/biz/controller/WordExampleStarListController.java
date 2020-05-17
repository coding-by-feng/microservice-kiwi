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
import me.fengorz.kiwi.word.api.entity.WordExampleStarListDO;
import me.fengorz.kiwi.word.biz.service.IWordExampleStarListService;
import me.fengorz.kiwi.word.biz.service.operate.IWordOperateService;
import org.hibernate.validator.constraints.Range;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;


/**
 * @author codingByFeng
 * @date 2019-12-08 23:27:12
 */
@RestController
@AllArgsConstructor
@RequestMapping("/word/example/star/list")
@Validated
@Slf4j
public class WordExampleStarListController extends BaseController {

    private final IWordExampleStarListService wordExampleStarListService;
    private final IWordOperateService wordOperateService;

    /**
     * 分页查询
     *
     * @param page                  分页对象
     * @param wordExampleStarListDO
     * @return
     */
    @GetMapping("/page")
    public R getWordParaphraseExampleListPage(Page page, WordExampleStarListDO wordExampleStarListDO) {
        return R.ok(wordExampleStarListService.page(page, Wrappers.query(wordExampleStarListDO)));
    }

    /**
     * 根据条件查询单个实体
     *
     * @param condition
     * @return
     */
    @PostMapping("/getOne")
    public R getOne(@RequestBody WordExampleStarListDO condition) {
        return R.ok(wordExampleStarListService.getOne(new QueryWrapper<>(condition)));
    }


    /**
     * 通过id查询
     *
     * @param id id
     * @return R
     */
    @GetMapping("/getById/{id}")
    public R getById(@PathVariable("id") Integer id) {
        return R.ok(wordExampleStarListService.getById(id));
    }

    /**
     * 新增
     *
     * @param wordExampleStarListDO
     * @return R
     */
    @SysLog("新增")
    @PostMapping("/save")
    // @PreAuthorize("@pms.hasPermission('api_wordparaphraseexamplelist_add')")
    public R save(WordExampleStarListDO wordExampleStarListDO) {
        wordExampleStarListDO.setOwner(1);
        return R.ok(wordExampleStarListService.save(wordExampleStarListDO));
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
    public R updateById(WordExampleStarListDO wordExampleStarListDO) {
        return R.ok(wordExampleStarListService.updateById(wordExampleStarListDO));
    }

    /**
     * 通过id删除
     *
     * @param id id
     * @return R
     */
    @SysLog("通过id删除")
    @PostMapping("/delById/{id}")
    // @PreAuthorize("@pms.hasPermission('api_wordparaphraseexamplelist_del')")
    public R delById(@PathVariable Integer id) {
        return R.ok(wordExampleStarListService.removeById(id));
    }

    @GetMapping("/getCurrentUserList")
    public R getCurrentUserList(Page page) {
        return wordExampleStarListService.getCurrentUserList(page, 1);
    }

    @PostMapping("/putIntoStarList")
    public R putIntoStarList(@NotNull Integer exampleId, @NotNull Integer listId) {
        try {
            return R.ok(this.wordOperateService.putExampleIntoStarList(exampleId, listId));
        } catch (ServiceException e) {
            log.error(e.getMessage());
            return R.failed(e.getMessage());
        }
    }

    @PostMapping("/removeExampleStar")
    public R removeExampleStar(@NotNull Integer exampleId, @NotNull Integer listId) {
        return R.ok(this.wordOperateService.removeExampleStar(exampleId, listId));
    }

    @PostMapping("/getListItems/{size}/{current}")
    public R getListItems(@NotNull Integer listId,
                          @PathVariable @Min(1) Integer current,
                          @PathVariable @Range(min = 1, max = 100) Integer size) {
        return R.ok(wordExampleStarListService.getListItems(new Page(current, size), listId));
    }

}
