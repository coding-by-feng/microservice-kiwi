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

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import me.fengorz.kiwi.common.api.R;
import me.fengorz.kiwi.common.sdk.controller.BaseController;
import me.fengorz.kiwi.common.log.annotation.SysLog;
import me.fengorz.kiwi.word.api.entity.WordParaphraseExampleDO;
import me.fengorz.kiwi.word.biz.service.IWordParaphraseExampleService;
import lombok.AllArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


/**
 * 单词例句表
 *
 * @author codingByFeng
 * @date 2019-10-31 20:40:38
 */
@RestController
@AllArgsConstructor
@RequestMapping("/word/paraphrase/example")
public class WordParaphraseExampleController extends BaseController {

    private final IWordParaphraseExampleService wordParaphraseExampleService;

    /**
     * 分页查询
     *
     * @param page                    分页对象
     * @param wordParaphraseExampleDO 单词例句表
     * @return
     */
    @GetMapping("/page")
    public R getWordParaphraseExamplePage(Page page, WordParaphraseExampleDO wordParaphraseExampleDO) {
        return R.ok(wordParaphraseExampleService.page(page, Wrappers.query(wordParaphraseExampleDO)));
    }

    /**
     * 通过id查询单词例句表
     *
     * @param exampleId id
     * @return R
     */
    @GetMapping("/{exampleId}")
    public R getById(@PathVariable("exampleId") Integer exampleId) {
        return R.ok(wordParaphraseExampleService.getById(exampleId));
    }

    /**
     * 新增单词例句表
     *
     * @param wordParaphraseExampleDO 单词例句表
     * @return R
     */
    @SysLog("新增单词例句表")
    @PostMapping
    @PreAuthorize("@pms.hasPermission('biz_wordparaphraseexample_add')")
    public R save(@RequestBody WordParaphraseExampleDO wordParaphraseExampleDO) {
        return R.ok(wordParaphraseExampleService.save(wordParaphraseExampleDO));
    }

    /**
     * 修改单词例句表
     *
     * @param wordParaphraseExampleDO 单词例句表
     * @return R
     */
    @SysLog("修改单词例句表")
    @PutMapping
    @PreAuthorize("@pms.hasPermission('biz_wordparaphraseexample_edit')")
    public R updateById(@RequestBody WordParaphraseExampleDO wordParaphraseExampleDO) {
        return R.ok(wordParaphraseExampleService.updateById(wordParaphraseExampleDO));
    }

    /**
     * 通过id删除单词例句表
     *
     * @param exampleId id
     * @return R
     */
    @SysLog("通过id删除单词例句表")
    @DeleteMapping("/{exampleId}")
    @PreAuthorize("@pms.hasPermission('biz_wordparaphraseexample_del')")
    public R removeById(@PathVariable Integer exampleId) {
        return R.ok(wordParaphraseExampleService.removeById(exampleId));
    }

}
