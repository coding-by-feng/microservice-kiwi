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
package me.fengorz.kiwi.word.biz.service.base.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import me.fengorz.kiwi.bdf.core.service.ISeqService;
import me.fengorz.kiwi.common.api.constant.CommonConstants;
import me.fengorz.kiwi.common.api.constant.MapperConstant;
import me.fengorz.kiwi.common.api.exception.ServiceException;
import me.fengorz.kiwi.word.api.common.WordCrawlerConstants;
import me.fengorz.kiwi.word.api.entity.FetchQueueDO;
import me.fengorz.kiwi.word.biz.mapper.FetchQueueMapper;
import me.fengorz.kiwi.word.biz.service.base.IWordFetchQueueService;
import me.fengorz.kiwi.word.biz.util.WordBizUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 单词待抓取列表
 *
 * @author zhanshifeng
 * @date 2019-10-30 14:45:45
 */
@Service()
@RequiredArgsConstructor
public class WordFetchQueueServiceImpl extends ServiceImpl<FetchQueueMapper, FetchQueueDO>
        implements IWordFetchQueueService {

    private final ISeqService seqService;

    @Transactional(rollbackFor = Exception.class, noRollbackFor = ServiceException.class)
    private void fetchNewWord(String wordName) {
        FetchQueueDO one = this.getOneAnyhow(wordName);

        if (one != null) {
            if (one.getIsLock() > 0) {
                return;
            }
            if (one.getInTime().compareTo(LocalDateTime.now().minusMinutes(1)) > 0) {
                return;
            }
            this.updateById(one.setFetchStatus(WordCrawlerConstants.STATUS_TO_FETCH).setIsLock(CommonConstants.FLAG_YES).setInTime(LocalDateTime.now()));
            return;
        }

        this.save(new FetchQueueDO().setQueueId(seqService.genIntSequence(MapperConstant.T_INS_SEQUENCE))
                .setWordName(wordName).setFetchStatus(WordCrawlerConstants.STATUS_TO_FETCH).setFetchPriority(100).setIsLock(CommonConstants.FLAG_YES));
    }

    @Async
    @Override
    public void flagStartFetchOnAsync(String wordName) {
        this.fetchNewWord(wordName);
    }

    private boolean del(String wordName) {
        return this.remove(new LambdaQueryWrapper<FetchQueueDO>().eq(FetchQueueDO::getWordName, wordName));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean invalid(String wordName) {
        if (!this.isExist(wordName)) {
            return false;
        }
        return this.update(new FetchQueueDO().setIsValid(CommonConstants.FLAG_N).setIsLock(CommonConstants.FLAG_NO),
                new LambdaQueryWrapper<FetchQueueDO>().eq(FetchQueueDO::getWordName, wordName));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean lock(String wordName) {
        if (!this.isExist(wordName)) {
            return false;
        }
        return this.update(new FetchQueueDO().setIsLock(CommonConstants.FLAG_YES),
                Wrappers.<FetchQueueDO>lambdaUpdate().eq(FetchQueueDO::getWordName, wordName)
                        .eq(FetchQueueDO::getIsLock, CommonConstants.FLAG_NO));
    }

    @Override
    public void flagFetchBaseFinish(Integer queueId, Integer wordId) {
        this.updateById(
                new FetchQueueDO().setQueueId(queueId).setWordId(wordId));
    }

    @Override
    public void flagWordQueryException(String wordName) {

        FetchQueueDO one = this.getOneAnyhow(wordName);
        // 爬虫状态进行中的不可以打断
        if (WordBizUtils.fetchQueueIsRunning(one.getFetchStatus())) {
            return;
        }

        this.updateById(one.setFetchStatus(WordCrawlerConstants.STATUS_TO_QUERY_ERROR).setIsLock(CommonConstants.FLAG_NO));
    }

    @Override
    public List<FetchQueueDO> page2List(Integer status, Integer current, Integer size, Integer isLock) {
        return Optional
                .of(this.page(new Page<>(current, size), Wrappers.<FetchQueueDO>lambdaQuery()
                        .eq(FetchQueueDO::getFetchStatus, status).eq(FetchQueueDO::getIsLock, isLock).le(FetchQueueDO::getFetchTime, WordCrawlerConstants.WORD_MAX_FETCH_LIMITED_TIME)))
                .get().getRecords();
    }

    @Override
    public List<FetchQueueDO> listNotIntoCache() {
        return Optional
                .of(this.page(new Page<>(1, 20), Wrappers.<FetchQueueDO>lambdaQuery()
                        .eq(FetchQueueDO::getFetchStatus, WordCrawlerConstants.STATUS_ALL_SUCCESS)
                        .eq(FetchQueueDO::getIsLock, CommonConstants.FLAG_NO)
                        .eq(FetchQueueDO::getIsIntoCache, CommonConstants.FLAG_NO)))
                .get().getRecords();
    }

    @Override
    public void saveDerivation(String inputWordName, String fetchWordName) {
        Optional.ofNullable(this.getOneInUnLock(inputWordName)).ifPresent(one -> {
            this.updateById(one.setDerivation(fetchWordName));
        });
    }

    /**
     * 拿到非锁住状态的记录
     *
     * @param wordName
     * @return
     */
    @Override
    public FetchQueueDO getOneInUnLock(String wordName) {
        return this.getOne(Wrappers.<FetchQueueDO>lambdaQuery().eq(FetchQueueDO::getWordName, wordName)
                .eq(FetchQueueDO::getIsLock, CommonConstants.FLAG_NO));
    }

    @Override
    public FetchQueueDO getOneInUnLock(Integer queueId) {
        return this.getOne(Wrappers.<FetchQueueDO>lambdaQuery().eq(FetchQueueDO::getQueueId, queueId)
                .eq(FetchQueueDO::getIsLock, CommonConstants.FLAG_NO));
    }

    /**
     * 拿到记录，无论是否锁住
     *
     * @param wordName
     * @return
     */
    @Override
    public FetchQueueDO getOneAnyhow(String wordName) {
        return this.getOne(Wrappers.<FetchQueueDO>lambdaQuery().eq(FetchQueueDO::getWordName, wordName));
    }

    @Override
    public FetchQueueDO getOneAnyhow(Integer queueId) {
        return this.getOne(Wrappers.<FetchQueueDO>lambdaQuery().eq(FetchQueueDO::getQueueId, queueId));
    }

    private boolean isExist(String wordName) {
        return this.getOneInUnLock(wordName) != null;
    }

}
