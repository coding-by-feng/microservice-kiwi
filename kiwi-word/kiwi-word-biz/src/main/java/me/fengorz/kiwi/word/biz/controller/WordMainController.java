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

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.api.R;
import me.fengorz.kiwi.common.api.annotation.log.SysLog;
import me.fengorz.kiwi.common.sdk.controller.BaseController;
import me.fengorz.kiwi.common.sdk.util.lang.string.KiwiStringUtils;
import me.fengorz.kiwi.common.sdk.util.time.KiwiDateUtils;
import me.fengorz.kiwi.word.api.dto.mapper.out.FuzzyQueryResultDTO;
import me.fengorz.kiwi.word.api.entity.WordMainDO;
import me.fengorz.kiwi.word.api.vo.detail.WordQueryVO;
import me.fengorz.kiwi.word.biz.service.base.IWordFetchQueueService;
import me.fengorz.kiwi.word.biz.service.base.IWordMainService;
import me.fengorz.kiwi.word.biz.service.operate.IOperateService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotBlank;
import java.util.List;

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

    private final IWordMainService mainService;
    private final IOperateService operateService;
    private final IWordFetchQueueService queueService;

    @GetMapping("/removeByWordName/{wordName}")
    // @PreAuthorize("@pms.hasPermission('biz_wordmain_del')")
    public R<Boolean> removeByWordName(@PathVariable String wordName) {
        queueService.startFetchOnAsync(wordName);
        return R.success();
    }

    @GetMapping("/query/gate/{keyword}")
    public R<WordQueryVO> queryGate(@PathVariable("keyword") String keyword) {
        log.info(KiwiStringUtils.format("========>queryGate[{}],[time={}]", keyword, KiwiDateUtils.now()));
        if (KiwiStringUtils.isContainChinese(keyword)) {
            return R.success(operateService.queryWordByCH(keyword));
        } else {
            return this.queryWord(keyword);
        }
    }


    @GetMapping("/query/{wordName}")
    public R<WordQueryVO> queryWord(@PathVariable("wordName") String wordName) {
        WordQueryVO wordQueryVO = operateService.queryWord(wordName);
        return R.success(wordQueryVO);
    }

    @GetMapping("/queryById/{wordId}")
    public R<WordQueryVO> queryWord(@PathVariable Integer wordId) {
        String wordName = mainService.getWordName(wordId);
        if (KiwiStringUtils.isBlank(wordName)) {
            return R.failed();
        }
        return R.success(operateService.queryWord(wordName));
    }

    @SysLog("模糊查询单词列表")
    @PostMapping("/fuzzyQueryList")
    public R<List<FuzzyQueryResultDTO>> fuzzyQueryList(@NotBlank String wordName, Page<WordMainDO> page) {
        return R.success(mainService.fuzzyQueryList(page, wordName));
    }

    @GetMapping("/listOverlapInUnLock")
    public R<List<String>> listOverlapInUnLock() {
        return R.success(mainService.listOverlapInUnLock());
    }

}
