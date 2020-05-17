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
import lombok.AllArgsConstructor;
import me.fengorz.kiwi.common.api.R;
import me.fengorz.kiwi.common.api.annotation.log.SysLog;
import me.fengorz.kiwi.common.api.exception.ServiceException;
import me.fengorz.kiwi.common.sdk.controller.BaseController;
import me.fengorz.kiwi.word.api.entity.WordCharacterDO;
import me.fengorz.kiwi.word.biz.service.IWordCharacterService;
import me.fengorz.kiwi.word.biz.service.operate.IWordOperateService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


/**
 * 单词词性表
 *
 * @author codingByFeng
 * @date 2019-10-31 20:38:37
 */
@RestController
@AllArgsConstructor
@RequestMapping("/word/character")
public class WordCharacterController extends BaseController {

    private final IWordCharacterService wordCharacterService;
    private final IWordOperateService wordOperateService;

    /**
     * 分页查询
     *
     * @param page          分页对象
     * @param wordCharacter 单词词性表
     * @return
     */
    @GetMapping("/page")
    public R getWordCharacterPage(Page page, WordCharacterDO wordCharacter) {
        return R.ok(wordCharacterService.page(page, Wrappers.query(wordCharacter)));
    }


    /**
     * 通过id查询单词词性表
     *
     * @param characterId id
     * @return R
     */
    @GetMapping("/{characterId}")
    public R getById(@PathVariable("characterId") Integer characterId) {
        return R.ok(wordCharacterService.getById(characterId));
    }

    /**
     * 新增单词词性表
     *
     * @param wordCharacter 单词词性表
     * @return R
     */
    @SysLog("新增单词词性表")
    @PostMapping
    @PreAuthorize("@pms.hasPermission('biz_wordcharacter_add')")
    public R save(@RequestBody WordCharacterDO wordCharacter) {
        return R.ok(wordCharacterService.save(wordCharacter));
    }

    /**
     * 修改单词词性表
     *
     * @param wordCharacter 单词词性表
     * @return R
     */
    @SysLog("修改单词词性表")
    @PutMapping
    @PreAuthorize("@pms.hasPermission('biz_wordcharacter_edit')")
    public R updateById(@RequestBody WordCharacterDO wordCharacter) {
        return R.ok(wordCharacterService.updateById(wordCharacter));
    }

    /**
     * 通过id删除单词词性表
     *
     * @param characterId id
     * @return R
     */
    @SysLog("通过id删除单词词性表")
    @DeleteMapping("/{characterId}")
    @PreAuthorize("@pms.hasPermission('biz_wordcharacter_del')")
    public R removeById(@PathVariable Integer characterId) {
        return R.ok(wordCharacterService.removeById(characterId));
    }

    @GetMapping("/getByParaphraseId/{paraphraseId}")
    public R getByParaphraseId(@PathVariable Integer paraphraseId) throws ServiceException {
        return R.ok(wordOperateService.getByParaphraseId(paraphraseId));
    }
}
