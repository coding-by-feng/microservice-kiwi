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

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.api.R;
import me.fengorz.kiwi.common.api.annotation.log.SysLog;
import me.fengorz.kiwi.common.api.constant.CommonConstants;
import me.fengorz.kiwi.common.sdk.controller.BaseController;
import me.fengorz.kiwi.word.api.dto.queue.RemovePronunciatioinMqDTO;
import me.fengorz.kiwi.word.api.dto.queue.fetch.FetchWordResultDTO;
import me.fengorz.kiwi.word.api.entity.WordFetchQueueDO;
import me.fengorz.kiwi.word.biz.service.base.IWordFetchQueueService;
import me.fengorz.kiwi.word.biz.service.operate.IWordCleanerService;
import me.fengorz.kiwi.word.biz.service.operate.IWordCrawlerService;
import me.fengorz.kiwi.word.biz.service.operate.IWordOperateService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.util.List;

/**
 * 单词待抓取列表
 *
 * @author zhanshifeng
 * @date 2019-10-30 14:45:45
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/word/fetch")
public class WordFetchController extends BaseController {

    private final IWordFetchQueueService wordFetchQueueService;
    private final IWordOperateService wordOperateService;
    private final IWordCrawlerService wordCrawlerService;
    private final IWordCleanerService wordCleanerService;

    @GetMapping(value = "/pageQueue/{status}/{current}/{size}")
    public R<List<WordFetchQueueDO>> pageQueue(@PathVariable Integer status, @PathVariable Integer current,
                                               @PathVariable Integer size) {
        return R.success(wordFetchQueueService.page2List(status, current, size, CommonConstants.FLAG_NO));
    }

    @GetMapping(value = "/pageQueueLockIn/{status}/{current}/{size}")
    public R<List<WordFetchQueueDO>> pageQueueLockIn(@PathVariable Integer status, @PathVariable Integer current,
                                                     @PathVariable Integer size) {
        return R.success(wordFetchQueueService.page2List(status, current, size, CommonConstants.FLAG_YES));
    }

    /**
     * 修改单词待抓取列表
     *
     * @param wordFetchQueue 单词待抓取列表
     * @return R
     */
    @SysLog("修改单词待抓取列表")
    @PostMapping("/updateById")
    public R<Boolean> updateById(@RequestBody WordFetchQueueDO wordFetchQueue) {
        return R.success(wordFetchQueueService.updateById(wordFetchQueue));
    }

    @SysLog("修改单词待抓取列表")
    @PostMapping("/updateByWordName")
    // @PreAuthorize("@pms.hasPermission('queue_wordfetchqueue_edit')")
    public R<Boolean> updateByWordName(@RequestBody WordFetchQueueDO wordFetchQueue) {
        return R.success(wordFetchQueueService.update(wordFetchQueue,
                new QueryWrapper<>(new WordFetchQueueDO().setWordName(wordFetchQueue.getWordName()))));
    }

    @SysLog("通过id删除单词待抓取列表")
    @PostMapping("/invalid")
    // @PreAuthorize("@pms.hasPermission('queue_wordfetchqueue_del')")
    public R<Boolean> invalid(@RequestParam @NotBlank String wordName) {
        return R.auto(wordFetchQueueService.invalid(wordName));
    }

    @PostMapping("/lock")
    // @PreAuthorize("@pms.hasPermission('queue_wordfetchqueue_del')")
    public R<Boolean> lock(@RequestParam @NotBlank String wordName) {
        return R.auto(wordFetchQueueService.lock(wordName));
    }

    @PostMapping("/storeResult")
    public R<Boolean> storeResult(@RequestBody @Valid FetchWordResultDTO dto) {
        return R.success(wordCrawlerService.storeFetchWordResult(dto));
    }

    @GetMapping("/fetchPronunciation/{wordId}")
    public R<Boolean> fetchPronunciation(@PathVariable Integer wordId) {
        return R.success(wordCrawlerService.fetchPronunciation(wordId));
    }

    @GetMapping("/removeWord/{wordName}/{queueId}")
    public R<List<RemovePronunciatioinMqDTO>> removeWord(@PathVariable String wordName, @PathVariable Integer queueId) {
        return R.success(wordCleanerService.removeWord(wordName, queueId));
    }
}
