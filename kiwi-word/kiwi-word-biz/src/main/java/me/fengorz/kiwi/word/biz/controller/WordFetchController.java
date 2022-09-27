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

import java.time.LocalDateTime;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import cn.hutool.core.util.EnumUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.api.R;
import me.fengorz.kiwi.common.sdk.constant.GlobalConstants;
import me.fengorz.kiwi.common.sdk.controller.BaseController;
import me.fengorz.kiwi.word.api.common.enumeration.ReviewAudioGenerationEnum;
import me.fengorz.kiwi.word.api.dto.queue.RemovePronunciatioinMqDTO;
import me.fengorz.kiwi.word.api.dto.queue.result.FetchPhraseResultDTO;
import me.fengorz.kiwi.word.api.dto.queue.result.FetchPhraseRunUpResultDTO;
import me.fengorz.kiwi.word.api.dto.queue.result.FetchWordResultDTO;
import me.fengorz.kiwi.word.api.entity.FetchQueueDO;
import me.fengorz.kiwi.word.api.util.WordApiUtils;
import me.fengorz.kiwi.word.biz.service.base.WordFetchQueueService;
import me.fengorz.kiwi.word.biz.service.operate.CleanerService;
import me.fengorz.kiwi.word.biz.service.operate.CrawlerService;
import me.fengorz.kiwi.word.biz.service.operate.OperateService;

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

    private final WordFetchQueueService queueService;
    private final OperateService operateService;
    private final CrawlerService crawlerService;
    private final CleanerService cleanerService;

    @GetMapping("/getOne/{queueId}")
    public R<FetchQueueDO> getOne(@PathVariable Integer queueId) {
        return R.success(queueService.getOneInUnLock(queueId));
    }

    @GetMapping("/getOneByWordName")
    public R<FetchQueueDO> getOneByWordName(@RequestParam String wordName) {
        return R.success(queueService.getOneInUnLock(WordApiUtils.decode(wordName)));
    }

    @GetMapping("/getAnyOne")
    public R<FetchQueueDO> getAnyOne(@RequestParam String wordName) {
        return R.success(queueService.getAnyOne(WordApiUtils.decode(wordName)));
    }

    @GetMapping(value = "/pageQueue/{status}/{current}/{size}/{infoType}")
    public R<List<FetchQueueDO>> pageQueue(@PathVariable Integer status, @PathVariable Integer current,
        @PathVariable Integer size, @PathVariable Integer infoType) {
        return R.success(queueService.page2List(status, current, size, GlobalConstants.FLAG_NO, infoType));
    }

    @GetMapping(value = "/pageQueueLockIn/{status}/{current}/{size}/{infoType}")
    public R<List<FetchQueueDO>> pageQueueLockIn(@PathVariable Integer status, @PathVariable Integer current,
        @PathVariable Integer size, @PathVariable Integer infoType) {
        return R.success(queueService.page2List(status, current, size, GlobalConstants.FLAG_YES, infoType));
    }

    @GetMapping(value = "/listNotIntoCache")
    public R<List<FetchQueueDO>> listNotIntoCache() {
        return R.success(queueService.listNotIntoCache());
    }

    /**
     * 修改单词待抓取列表
     *
     * @param queue 单词待抓取列表
     * @return R
     */
    @PostMapping("/updateById")
    public R<Boolean> updateById(@RequestBody FetchQueueDO queue) {
        queue.setOperateTime(LocalDateTime.now());
        return R.success(queueService.updateById(queue));
    }

    @PostMapping("/updateByWordName")
    // @PreAuthorize("@pms.hasPermission('queue_wordfetchqueue_edit')")
    public R<Boolean> updateByWordName(@RequestBody FetchQueueDO wordFetchQueue) {
        return R.success(queueService.update(wordFetchQueue,
            new QueryWrapper<>(new FetchQueueDO().setWordName(wordFetchQueue.getWordName()))));
    }

    @Deprecated
    @PostMapping("/invalid")
    // @PreAuthorize("@pms.hasPermission('queue_wordfetchqueue_del')")
    public R<Boolean> invalid(@RequestParam @NotBlank String wordName) {
        return R.auto(queueService.invalid(wordName));
    }

    @Deprecated
    @PostMapping("/lock")
    // @PreAuthorize("@pms.hasPermission('queue_wordfetchqueue_del')")
    public R<Boolean> lock(@RequestParam @NotBlank String wordName) {
        return R.auto(queueService.lock(wordName));
    }

    @PostMapping("/storeResult")
    public R<Boolean> storeResult(@RequestBody @Valid FetchWordResultDTO dto) {
        return R.success(crawlerService.storeFetchWordResult(dto));
    }

    @PostMapping("/handlePhrasesFetchResult")
    public R<Boolean> handlePhrasesFetchResult(@RequestBody @Valid FetchPhraseRunUpResultDTO dto) {
        return R.success(crawlerService.handlePhrasesFetchResult(dto));
    }

    @PostMapping("/storePhrasesFetchResult")
    public R<Boolean> storePhrasesFetchResult(@RequestBody FetchPhraseResultDTO dto) {
        return R.success(crawlerService.storePhrasesFetchResult(dto));
    }

    @GetMapping("/fetchPronunciation/{wordId}")
    public R<Boolean> fetchPronunciation(@PathVariable Integer wordId) {
        return R.success(crawlerService.fetchPronunciation(wordId));
    }

    @GetMapping("/removeWord/{queueId}")
    public R<List<RemovePronunciatioinMqDTO>> removeWord(@PathVariable Integer queueId) {
        return R.success(cleanerService.removeWord(queueId));
    }

    @GetMapping("/removePhrase/{queueId}")
    public R<Boolean> removePhrase(@PathVariable Integer queueId) {
        return R.success(cleanerService.removePhrase(queueId));
    }

    @GetMapping("/generateTtsVoice/{type}")
    public R<Void> generateTtsVoice(@PathVariable("type") Integer type) {
        crawlerService.generateTtsVoice(EnumUtil.likeValueOf(ReviewAudioGenerationEnum.class, type));
        return R.success();
    }

}
