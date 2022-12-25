/*
 *
 * Copyright [2019~2025] [codingByFeng]
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
import me.fengorz.kiwi.common.api.R;
import me.fengorz.kiwi.common.sdk.annotation.log.LogMarker;
import me.fengorz.kiwi.common.sdk.controller.BaseController;
import me.fengorz.kiwi.common.sdk.util.lang.string.KiwiStringUtils;
import me.fengorz.kiwi.common.sdk.util.time.KiwiDateUtils;
import me.fengorz.kiwi.common.sdk.web.WebTools;
import me.fengorz.kiwi.word.api.dto.mapper.out.FuzzyQueryResultDTO;
import me.fengorz.kiwi.word.api.entity.WordMainDO;
import me.fengorz.kiwi.word.api.vo.detail.WordQueryVO;
import me.fengorz.kiwi.word.biz.service.base.WordFetchQueueService;
import me.fengorz.kiwi.word.biz.service.base.WordMainService;
import me.fengorz.kiwi.word.biz.service.operate.OperateService;
import me.fengorz.kiwi.word.biz.service.operate.ReviewService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.elasticsearch.core.DocumentOperations;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;

import static me.fengorz.kiwi.word.api.util.WordApiUtils.decode;

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

    private final WordMainService mainService;
    private final OperateService operateService;
    private final WordFetchQueueService queueService;
    private final ReviewService reviewService;
    private final DocumentOperations documentOperations;

    @DeleteMapping("/removeByWordName/{wordName}")
    // @PreAuthorize("@pms.hasPermission('biz_wordmain_del')")
    public R<Boolean> removeByWordName(@PathVariable String wordName) {
        queueService.startFetchOnAsync(decode(wordName));
        return R.success();
    }

    @PostMapping("/query/gate/{keyword}")
    public R<IPage<WordQueryVO>> queryGate(@PathVariable(name = "keyword") String keyword, Integer current,
        Integer size) {
        keyword = decode(StringUtils.lowerCase(keyword));
        log.info(KiwiStringUtils.format("========>queryGate[{}],[time={}]", keyword, KiwiDateUtils.now()));
        if (KiwiStringUtils.isContainChinese(keyword)) {
            return R.success(operateService.queryWordByCh(keyword, WebTools.deductCurrent(current), size));
        } else {
            return this.queryWord(keyword);
        }
    }

    @LogMarker(isPrintParameter = true)
    @GetMapping("/query/{wordName}")
    public R<IPage<WordQueryVO>> queryWord(@PathVariable(value = "wordName") String wordName) {
        wordName = decode(StringUtils.lowerCase(wordName));
        IPage<WordQueryVO> page;
        page = new Page<>();
        if (KiwiStringUtils.isNotBlank(wordName)) {
            List<WordQueryVO> list = new ArrayList<>(1);
            list.add(operateService.queryWord(wordName));
            page.setRecords(list);
        }
        return R.success(page);
    }

    @GetMapping("/queryById/{wordId}")
    public R<WordQueryVO> queryWord(@PathVariable Integer wordId) {
        String wordName = mainService.getWordName(wordId);
        if (KiwiStringUtils.isBlank(wordName)) {
            return R.failed();
        }
        return R.success(operateService.queryWord(wordName));
    }

    @LogMarker("模糊查询单词列表")
    @PostMapping("/fuzzyQueryList")
    public R<List<FuzzyQueryResultDTO>> fuzzyQueryList(@NotBlank String wordName, Page<WordMainDO> page) {
        return R.success(mainService.fuzzyQueryList(page, decode(wordName)));
    }

    @GetMapping("/listOverlapAnyway")
    public R<List<String>> listOverlapAnyway() {
        return R.success(mainService.listOverlapAnyway());
    }

    @PostMapping("/variant/insertVariant/{inputWordName}/{fetchWordName}")
    public R<Void> insertVariant(@PathVariable String inputWordName, @PathVariable String fetchWordName) {
        return R.auto(operateService.insertVariant(inputWordName, fetchWordName));
    }

}
