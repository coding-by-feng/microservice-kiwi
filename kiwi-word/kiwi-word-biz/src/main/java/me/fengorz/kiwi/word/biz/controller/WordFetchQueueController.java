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
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.api.R;
import me.fengorz.kiwi.common.api.constant.CommonConstants;
import me.fengorz.kiwi.common.fastdfs.exception.DfsOperateDeleteException;
import me.fengorz.kiwi.common.fastdfs.exception.DfsOperateException;
import me.fengorz.kiwi.common.api.annotation.log.SysLog;
import me.fengorz.kiwi.common.sdk.controller.BaseController;
import me.fengorz.kiwi.common.sdk.util.log.EnhancedLogUtils;
import me.fengorz.kiwi.word.api.common.CrawlerConstants;
import me.fengorz.kiwi.word.api.dto.fetch.FetchWordResultDTO;
import me.fengorz.kiwi.word.api.dto.remote.WordFetchQueuePageDTO;
import me.fengorz.kiwi.word.api.entity.WordFetchQueueDO;
import me.fengorz.kiwi.word.api.exception.WordResultStoreException;
import me.fengorz.kiwi.word.biz.service.IWordFetchQueueService;
import me.fengorz.kiwi.word.biz.service.operate.IWordOperateService;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;


/**
 * 单词待抓取列表
 *
 * @author codingByFeng
 * @date 2019-10-30 14:45:45
 */
@RestController
@AllArgsConstructor
@RequestMapping("/word/fetch/queue")
@Slf4j
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
    public R getWordFetchQueuePage(@RequestBody @Valid WordFetchQueuePageDTO wordFetchQueuePage) {
        return R.ok(wordFetchQueueService.page(wordFetchQueuePage.getPage(), Wrappers.query(wordFetchQueuePage.getWordFetchQueue())));
    }


    /**
     * 通过id查询单词待抓取列表
     *
     * @param queueId id
     * @return R
     */
    @GetMapping("/{queueId}")
    public R getById(@PathVariable("queueId") Integer queueId) {
        return R.ok(wordFetchQueueService.getById(queueId));
    }

    /**
     * 新增单词待抓取列表
     *
     * @param wordFetchQueue 单词待抓取列表
     * @return R
     */
    @SysLog("新增单词待抓取列表")
    @PostMapping("/save")
    // @PreAuthorize("@pms.hasPermission('queue_wordfetchqueue_add')")
    public R save(@RequestBody @Valid WordFetchQueueDO wordFetchQueue) {
        return R.auto(wordFetchQueueService.insertNewQueue(wordFetchQueue), wordFetchQueue.getWordName() + " already exists!");
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
    public R updateById(@RequestBody WordFetchQueueDO wordFetchQueue) {
        return R.ok(wordFetchQueueService.updateById(wordFetchQueue));
    }

    @SysLog("修改单词待抓取列表")
    @PostMapping("/updateByWordName")
    // @PreAuthorize("@pms.hasPermission('queue_wordfetchqueue_edit')")
    public R updateByWordName(@RequestBody WordFetchQueueDO wordFetchQueue) {
        return R.ok(wordFetchQueueService.update(wordFetchQueue, new QueryWrapper<>(new WordFetchQueueDO()
                .setWordName(wordFetchQueue.getWordName()))));
    }

    /**
     * 通过id删除单词待抓取列表
     *
     * @param queueId id
     * @return R
     */
    @SysLog("通过id删除单词待抓取列表")
    @DeleteMapping("/{queueId}")
    // @PreAuthorize("@pms.hasPermission('queue_wordfetchqueue_del')")
    public R removeById(@PathVariable Integer queueId) {
        return R.ok(wordFetchQueueService.removeById(queueId));
    }


    @PostMapping("/storeFetchWordResult")
    public R storeFetchWordResult(@RequestBody @Valid FetchWordResultDTO fetchWordResultDTO) {
        try {
            wordOperateService.storeFetchWordResult(fetchWordResultDTO);
        } catch (WordResultStoreException e) {
            return R.failed(CrawlerConstants.STATUS_ERROR_WORD_ID_NOT_NULL, e.getMessage());
        } catch (DfsOperateException e) {
            return R.failed(CrawlerConstants.STATUS_ERROR_DFS_OPERATE_FAILED, e.getMessage());
        } catch (DfsOperateDeleteException e) {
            log.error(EnhancedLogUtils.getClassName() + CommonConstants.DOT + EnhancedLogUtils.getMethodName(), e.getMessage());
            wordOperateService.dfsDeleteExceptionBackCall(fetchWordResultDTO.getWordName());
            return R.failed(CrawlerConstants.STATUS_ERROR_DFS_OPERATE_DELETE_FAILED, e.getMessage());
        }
        return R.ok();
    }
}
