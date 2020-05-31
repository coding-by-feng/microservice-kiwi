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

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.api.R;
import me.fengorz.kiwi.common.api.ResultCode;
import me.fengorz.kiwi.common.api.annotation.log.SysLog;
import me.fengorz.kiwi.common.api.constant.CommonConstants;
import me.fengorz.kiwi.common.api.exception.dfs.DfsOperateDeleteException;
import me.fengorz.kiwi.common.api.exception.dfs.DfsOperateException;
import me.fengorz.kiwi.common.sdk.controller.BaseController;
import me.fengorz.kiwi.common.sdk.util.log.KiwiLogUtils;
import me.fengorz.kiwi.word.api.common.WordCrawlerConstants;
import me.fengorz.kiwi.word.api.dto.fetch.FetchWordResultDTO;
import me.fengorz.kiwi.word.api.dto.remote.WordFetchQueuePageDTO;
import me.fengorz.kiwi.word.api.entity.WordFetchQueueDO;
import me.fengorz.kiwi.word.api.exception.WordResultStoreRuntimeException;
import me.fengorz.kiwi.word.biz.service.IWordFetchQueueService;
import me.fengorz.kiwi.word.biz.service.operate.IWordOperateService;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;


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
@RequestMapping("/word/fetch/queue")
public class WordFetchQueueController extends BaseController {

    private final IWordFetchQueueService wordFetchQueueService;
    private final IWordOperateService wordOperateService;
    private final SqlSessionFactory sqlSessionFactory;

    /**
     * 分页查询
     *
     * @return
     */
    @RequestMapping(value = "/getWordFetchQueuePage", method = {RequestMethod.POST, RequestMethod.GET})
    public R<IPage<WordFetchQueueDO>> getWordFetchQueuePage(@RequestBody @Valid WordFetchQueuePageDTO wordFetchQueuePage) {
        return R.success(wordFetchQueueService.page(wordFetchQueuePage.getPage(), Wrappers.query(wordFetchQueuePage.getWordFetchQueue())));
    }

    @GetMapping("/fetchNewWord/{wordName}")
    public R<Void> fetchNewWord(@PathVariable String wordName) {
        return R.auto(wordFetchQueueService.fetchNewWord(wordName));
    }

    /**
     * 修改单词待抓取列表
     *
     * @param wordFetchQueue 单词待抓取列表
     * @return R
     */
    @SysLog("修改单词待抓取列表")
    @PutMapping("/updateById")
    // @PreAuthorize("@pms.hasPermission('queue_wordfetchqueue_edit')")
    public R<Boolean> updateById(@RequestBody WordFetchQueueDO wordFetchQueue) {
        return R.success(wordFetchQueueService.updateById(wordFetchQueue));
    }

    @SysLog("修改单词待抓取列表")
    @PostMapping("/updateByWordName")
    // @PreAuthorize("@pms.hasPermission('queue_wordfetchqueue_edit')")
    public R<Boolean> updateByWordName(@RequestBody WordFetchQueueDO wordFetchQueue) {
        return R.success(wordFetchQueueService.update(wordFetchQueue, new QueryWrapper<>(new WordFetchQueueDO()
                .setWordName(wordFetchQueue.getWordName()))));
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


    @PostMapping("/storeFetchWordResult")
    public R<Void> storeFetchWordResult(@RequestBody @Valid FetchWordResultDTO fetchWordResultDTO) {
        try {
            wordOperateService.storeFetchWordResult(fetchWordResultDTO);
        } catch (WordResultStoreRuntimeException e) {
            return R.failed(ResultCode.build(
                    () -> WordCrawlerConstants.STATUS_ERROR_WORD_ID_NOT_NULL, () -> CommonConstants.EMPTY), e.getMessage());
        } catch (DfsOperateDeleteException e) {
            log.error(KiwiLogUtils.getClassName() + CommonConstants.SYMBOL_DOT + KiwiLogUtils.getMethodName(), e.getMessage());
            wordOperateService.dfsDeleteExceptionBackCall(fetchWordResultDTO.getWordName());
            return R.failed(ResultCode.build(
                    () -> WordCrawlerConstants.STATUS_ERROR_DFS_OPERATE_DELETE_FAILED, () -> CommonConstants.EMPTY), e.getMessage());
        } catch (DfsOperateException e) {
            return R.failed(ResultCode.build(
                    () -> WordCrawlerConstants.STATUS_ERROR_DFS_OPERATE_FAILED, () -> CommonConstants.EMPTY), e.getMessage());
        }
        return R.success();
    }
}
