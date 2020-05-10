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
import me.fengorz.kiwi.word.api.entity.WordParaphraseDO;
import me.fengorz.kiwi.word.biz.service.IWordParaphraseService;
import lombok.AllArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


/**
 * 单词释义表
 *
 * @author codingByFeng
 * @date 2019-10-31 20:39:48
 */
@RestController
@AllArgsConstructor
@RequestMapping("/word/paraphrase")
public class WordParaphraseController extends BaseController {

    private final IWordParaphraseService wordParaphraseService;

    /**
     * 分页查询
     *
     * @param page             分页对象
     * @param wordParaphraseDO 单词释义表
     * @return
     */
    @GetMapping("/page")
    public R getWordParaphrasePage(Page page, WordParaphraseDO wordParaphraseDO) {
        return R.ok(wordParaphraseService.page(page, Wrappers.query(wordParaphraseDO)));
    }


    /**
     * 通过id查询单词释义表
     *
     * @param paraphraseId id
     * @return R
     */
    @GetMapping("/{paraphraseId}")
    public R getById(@PathVariable("paraphraseId") Integer paraphraseId) {
        return R.ok(wordParaphraseService.getById(paraphraseId));
    }

    /**
     * 新增单词释义表
     *
     * @param wordParaphraseDO 单词释义表
     * @return R
     */
    @SysLog("新增单词释义表")
    @PostMapping
    @PreAuthorize("@pms.hasPermission('biz_wordparaphrase_add')")
    public R save(@RequestBody WordParaphraseDO wordParaphraseDO) {
        return R.ok(wordParaphraseService.save(wordParaphraseDO));
    }

    /**
     * 修改单词释义表
     *
     * @param wordParaphraseDO 单词释义表
     * @return R
     */
    @SysLog("修改单词释义表")
    @PutMapping
    @PreAuthorize("@pms.hasPermission('biz_wordparaphrase_edit')")
    public R updateById(@RequestBody WordParaphraseDO wordParaphraseDO) {
        return R.ok(wordParaphraseService.updateById(wordParaphraseDO));
    }

    /**
     * 通过id删除单词释义表
     *
     * @param paraphraseId id
     * @return R
     */
    @SysLog("通过id删除单词释义表")
    @DeleteMapping("/{paraphraseId}")
    @PreAuthorize("@pms.hasPermission('biz_wordparaphrase_del')")
    public R removeById(@PathVariable Integer paraphraseId) {
        return R.ok(wordParaphraseService.removeById(paraphraseId));
    }

}
