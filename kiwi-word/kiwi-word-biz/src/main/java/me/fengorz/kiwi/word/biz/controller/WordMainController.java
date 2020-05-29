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
import io.swagger.models.auth.In;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.api.R;
import me.fengorz.kiwi.common.api.annotation.log.SysLog;
import me.fengorz.kiwi.common.api.exception.dfs.DfsOperateDeleteException;
import me.fengorz.kiwi.common.sdk.controller.BaseController;
import me.fengorz.kiwi.word.api.entity.WordMainDO;
import me.fengorz.kiwi.word.biz.service.IWordMainService;
import me.fengorz.kiwi.word.biz.service.operate.IWordOperateService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;


/**
 * 单词主表
 *
 * @author codingByFeng
 * @date 2019-10-31 20:32:07
 */
@RestController
@AllArgsConstructor
@RequestMapping("/word/main")
@Validated
@Slf4j
public class WordMainController extends BaseController {

    private final IWordMainService wordMainService;
    private final IWordOperateService wordOperateService;

    /**
     * 分页查询
     *
     * @param page       分页对象
     * @param wordMainDO 单词主表
     * @return
     */
    @GetMapping("/page")
    public R getWordMainPage(Page page, WordMainDO wordMainDO) {
        return R.success(wordMainService.page(page, Wrappers.query(wordMainDO)));
    }

    /**
     * 根据条件查询单个实体
     *
     * @param condition
     * @return
     */
    @PostMapping("/getOne")
    public R getOne(WordMainDO condition) {
        return R.success(wordMainService.getOne(new QueryWrapper<>(condition)));
    }

    /**
     * 通过id查询单词主表
     *
     * @param wordId id
     * @return R
     */
    @GetMapping("/getById/{wordId}")
    public R getById(@PathVariable("wordId") Integer wordId) {
        return R.success(wordMainService.getById(wordId));
    }

    /**
     * 新增单词主表
     *
     * @param wordMainDO 单词主表
     * @return R
     */
    @SysLog("新增单词主表")
    @PostMapping
    @PreAuthorize("@pms.hasPermission('biz_wordmain_add')")
    public R save(@RequestBody WordMainDO wordMainDO) {
        return R.success(wordMainService.save(wordMainDO));
    }

    /**
     * 修改单词主表
     *
     * @param wordMainDO 单词主表
     * @return R
     */
    @SysLog("修改单词主表")
    @PutMapping
    @PreAuthorize("@pms.hasPermission('biz_wordmain_edit')")
    public R updateById(@RequestBody WordMainDO wordMainDO) {
        return R.success(wordMainService.updateById(wordMainDO));
    }

    @SysLog("通过id删除单词主表")
    @GetMapping("/removeByWordName/{wordName}")
    // @PreAuthorize("@pms.hasPermission('biz_wordmain_del')")
    public R removeByWordName(@PathVariable String wordName) throws DfsOperateDeleteException {
        return R.auto(wordOperateService.removeWord(wordName));
    }

    @GetMapping("/query/{wordName}")
    public R queryWord(@PathVariable("wordName") String wordName) {
        return R.success(wordOperateService.queryWord(wordName));
    }

    @GetMapping("/queryById/{wordId}")
    public R queryWord(@PathVariable Integer wordId) {
        // TODO ZSF 改成去缓存
        WordMainDO word = wordMainService.getById(wordId);
        if (word == null) {
            return R.success();
        }
        return R.success(wordOperateService.queryWord(word.getWordName()));
    }

    @SysLog("模糊查询单词列表")
    @PostMapping("/fuzzyQueryList")
    public R<List<Map>> fuzzyQueryList(@NotBlank String wordName, Page<WordMainDO> page) {
        return R.success(wordMainService.fuzzyQueryList(page, wordName));
    }

}
