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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.bdf.core.service.ISeqService;
import me.fengorz.kiwi.common.sdk.annotation.log.LogMarker;
import me.fengorz.kiwi.common.sdk.constant.GlobalConstants;
import me.fengorz.kiwi.common.sdk.constant.MapperConstant;
import me.fengorz.kiwi.common.sdk.exception.ServiceException;
import me.fengorz.kiwi.common.sdk.util.lang.string.KiwiStringUtils;
import me.fengorz.kiwi.word.api.common.WordCrawlerConstants;
import me.fengorz.kiwi.word.api.common.enumeration.CrawlerStatusEnum;
import me.fengorz.kiwi.word.api.entity.FetchQueueDO;
import me.fengorz.kiwi.word.biz.mapper.FetchQueueMapper;
import me.fengorz.kiwi.word.biz.mapper.WordMainMapper;
import me.fengorz.kiwi.word.biz.service.base.IWordFetchQueueService;
import me.fengorz.kiwi.word.biz.util.WordBizUtils;

/**
 * 单词待抓取列表
 *
 * @author zhanshifeng
 * @date 2019-10-30 14:45:45
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WordFetchQueueServiceImpl extends ServiceImpl<FetchQueueMapper, FetchQueueDO>
    implements IWordFetchQueueService {

    private final ISeqService seqService;
    private final WordMainMapper mainMapper;

    @Transactional(rollbackFor = Exception.class, noRollbackFor = ServiceException.class)
    private void fetch(String wordName, String derivation, Integer wordId, Integer... infoType) {
        // 如果没传infoType要判断是否包含空格
        int thisInfoType;
        thisInfoType = WordBizUtils.buildThisInfoType(wordName, infoType);

        FetchQueueDO one = this.getOneAnyhow(wordName, thisInfoType);

        // 取反类型获取队列
        if (one == null) {
            one = this.getOneAnyhow(wordName, WordBizUtils.getOpposition(thisInfoType));
        }

        if (one != null) {
            if (one.getIsLock() > 0) {
                return;
            }
            // 抓取成功的禁止再重复抓取
            if (one.getFetchStatus() >= WordCrawlerConstants.STATUS_ALL_SUCCESS && one.getWordId() > 0) {
                if (mainMapper.selectById(one.getWordId()) != null) {
                    return;
                }
            }

            if (one.getInTime().compareTo(LocalDateTime.now().minusMinutes(1)) > 0) {
                return;
            }
            this.updateById(one.setFetchStatus(WordCrawlerConstants.STATUS_TO_FETCH).setIsLock(GlobalConstants.FLAG_YES)
                .setOperateTime(LocalDateTime.now()).setInfoType(thisInfoType));
            return;
        }

        this.insertOne(wordId, wordName, derivation, WordCrawlerConstants.STATUS_TO_FETCH, thisInfoType);
    }

    private void insertOne(Integer wordId, String wordName, String derivation, int status, Integer... infoType) {
        FetchQueueDO queueDO =
            new FetchQueueDO().setQueueId(seqService.genIntSequence(MapperConstant.T_INS_SEQUENCE)).setWordId(wordId)
                .setWordName(wordName).setDerivation(KiwiStringUtils.isNotBlank(derivation) ? derivation : null)
                .setFetchStatus(status).setFetchPriority(100).setInTime(LocalDateTime.now())
                .setOperateTime(LocalDateTime.now()).setIsLock(GlobalConstants.FLAG_YES);
        if (infoType == null || infoType.length == 0) {
            this.save(queueDO);
        } else {
            this.save(queueDO.setInfoType(infoType[0]));
        }
    }

    @Async
    @Override
    @LogMarker(isPrintParameter = true, isPrintExecutionTime = true)
    public void startFetchOnAsync(String wordName) {
        this.fetch(wordName, null, null);
    }

    @Async
    @Override
    public void startFetchPhraseOnAsync(String phrase, String word, Integer wordId) {
        this.fetch(phrase, word, wordId, WordCrawlerConstants.QUEUE_INFO_TYPE_PHRASE);
    }

    @Override
    public void startFetch(String wordName) {
        this.fetch(wordName, null, null);
    }

    @Override
    public void startForceFetchWord(String wordName) {
        this.fetch(wordName, wordName, null, WordCrawlerConstants.QUEUE_INFO_TYPE_WORD);
    }

    @Override
    public void startFetchPhrase(String phrase, String word, Integer wordId) {
        this.fetch(phrase, word, wordId, WordCrawlerConstants.QUEUE_INFO_TYPE_PHRASE);
    }

    @Transactional(rollbackFor = Exception.class)
    private boolean del(String wordName) {
        return this.remove(new LambdaQueryWrapper<FetchQueueDO>().eq(FetchQueueDO::getWordName, wordName));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean invalid(String wordName) {
        if (this.isExist(wordName)) {
            return false;
        }
        return this.update(new FetchQueueDO().setIsValid(GlobalConstants.FLAG_N).setIsLock(GlobalConstants.FLAG_NO),
            new LambdaQueryWrapper<FetchQueueDO>().eq(FetchQueueDO::getWordName, wordName));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean lock(String wordName) {
        if (this.isExist(wordName)) {
            return false;
        }
        return this.update(new FetchQueueDO().setIsLock(GlobalConstants.FLAG_YES), Wrappers.<FetchQueueDO>lambdaUpdate()
            .eq(FetchQueueDO::getWordName, wordName).eq(FetchQueueDO::getIsLock, GlobalConstants.FLAG_NO));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void flagFetchBaseFinish(Integer queueId, Integer wordId) {
        this.updateById(new FetchQueueDO().setQueueId(queueId).setWordId(wordId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void flagWordQueryException(String wordName) {
        FetchQueueDO one = this.getOneAnyhow(wordName);
        // 如果队列记录不存在
        if (one == null) {
            log.info("The word [{}] has not been fetched and is about to be fetched.", wordName);
            this.insertOne(null, wordName, wordName, WordCrawlerConstants.STATUS_TO_DEL_BASE);
            return;
        }

        // 爬虫状态进行中的不可以打断
        if (WordBizUtils.fetchQueueIsRunning(one.getFetchStatus())) {
            log.warn("The word {} queue is locked and cannot change the queue state.", wordName);
            return;
        }

        this.updateById(
            one.setFetchStatus(CrawlerStatusEnum.STATUS_TO_QUERY_ERROR.getStatus()).setIsLock(GlobalConstants.FLAG_NO));
        log.info("Update the status of the word {} to query error!", wordName);
    }

    @Override
    public List<FetchQueueDO> page2List(Integer status, Integer current, Integer size, Integer isLock,
        Integer infoType) {
        return Optional
            .of(this.page(new Page<>(current, size),
                Wrappers.<FetchQueueDO>lambdaQuery().eq(FetchQueueDO::getFetchStatus, status)
                    .eq(FetchQueueDO::getIsLock, isLock).eq(FetchQueueDO::getInfoType, infoType)
                    .le(FetchQueueDO::getFetchTime, WordCrawlerConstants.WORD_MAX_FETCH_LIMITED_TIME)))
            .get().getRecords();
    }

    @Override
    public List<FetchQueueDO> listNotIntoCache() {
        return Optional.of(this.page(new Page<>(1, 20),
            Wrappers.<FetchQueueDO>lambdaQuery()
                .ge(FetchQueueDO::getFetchStatus, WordCrawlerConstants.STATUS_PERFECT_SUCCESS)
                .eq(FetchQueueDO::getIsLock, GlobalConstants.FLAG_NO)
                .eq(FetchQueueDO::getIsIntoCache, GlobalConstants.FLAG_NO)))
            .get().getRecords();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
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
    @LogMarker(isPrintReturnValue = true)
    public FetchQueueDO getOneInUnLock(String wordName, Integer... infoType) {
        return this.getOne(Wrappers.<FetchQueueDO>lambdaQuery().eq(FetchQueueDO::getWordName, wordName)
            .eq(FetchQueueDO::getIsLock, GlobalConstants.FLAG_NO).eq(FetchQueueDO::getInfoType,
                infoType == null || infoType.length == 0 ? WordCrawlerConstants.QUEUE_INFO_TYPE_WORD : infoType[0]));
    }

    @Override
    public FetchQueueDO getOneInUnLock(Integer queueId) {
        return this.getOne(Wrappers.<FetchQueueDO>lambdaQuery().eq(FetchQueueDO::getQueueId, queueId)
            .eq(FetchQueueDO::getIsLock, GlobalConstants.FLAG_NO));
    }

    /**
     * 拿到记录，无论是否锁住
     *
     * @param wordName
     * @return
     */
    @Override
    public FetchQueueDO getOneAnyhow(String wordName, Integer... infoType) {
        return this.getOne(
            Wrappers.<FetchQueueDO>lambdaQuery().eq(FetchQueueDO::getWordName, wordName)
                    .eq(FetchQueueDO::getInfoType, WordBizUtils.buildThisInfoType(wordName, infoType)));
    }

    @Override
    @LogMarker(isPrintReturnValue = true, isPrintParameter = true, isPrintExecutionTime = true)
    public FetchQueueDO getAnyOne(String wordName) {
        return this.getOne(Wrappers.<FetchQueueDO>lambdaQuery().eq(FetchQueueDO::getWordName, wordName));
    }

    @Override
    public FetchQueueDO getOneAnyhow(Integer queueId) {
        return this.getOne(Wrappers.<FetchQueueDO>lambdaQuery().eq(FetchQueueDO::getQueueId, queueId));
    }

    @Deprecated
    private boolean isExist(String wordName) {
        return this.getOneInUnLock(wordName) == null;
    }
}
