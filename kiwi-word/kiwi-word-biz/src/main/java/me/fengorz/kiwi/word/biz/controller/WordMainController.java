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

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.api.R;
import me.fengorz.kiwi.common.api.annotation.log.SysLog;
import me.fengorz.kiwi.common.api.exception.dfs.DfsOperateDeleteException;
import me.fengorz.kiwi.common.sdk.controller.BaseController;
import me.fengorz.kiwi.common.sdk.util.lang.string.KiwiStringUtils;
import me.fengorz.kiwi.word.api.entity.WordMainDO;
import me.fengorz.kiwi.word.api.vo.detail.WordQueryVO;
import me.fengorz.kiwi.word.biz.service.IWordMainService;
import me.fengorz.kiwi.word.biz.service.operate.IWordOperateService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;


/**
 * 单词主表
 *
 * @author zhanshifeng
 * @date 2019-10-31 20:32:07
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/word/main")
@Validated
@Slf4j
public class WordMainController extends BaseController {

    private final IWordMainService wordMainService;
    private final IWordOperateService wordOperateService;

    @GetMapping("/removeByWordName/{wordName}")
    // @PreAuthorize("@pms.hasPermission('biz_wordmain_del')")
    public R<Boolean> removeByWordName(@PathVariable String wordName) throws DfsOperateDeleteException {
        return R.auto(wordOperateService.removeWord(wordName));
    }

    @GetMapping("/query/{wordName}")
    public R<WordQueryVO> queryWord(@PathVariable("wordName") String wordName) {
        return R.success(wordOperateService.queryWord(wordName));
    }

    @GetMapping("/queryById/{wordId}")
    public R<WordQueryVO> queryWord(@PathVariable Integer wordId) {
        String wordName = wordMainService.getWordName(wordId);
        if (KiwiStringUtils.isBlank(wordName)) {
            return R.failed();
        }
        return R.success(wordOperateService.queryWord(wordName));
    }

    @SysLog("模糊查询单词列表")
    @PostMapping("/fuzzyQueryList")
    public R<List<Map>> fuzzyQueryList(@NotBlank String wordName, Page<WordMainDO> page) {
        return R.success(wordMainService.fuzzyQueryList(page, wordName));
    }

}
