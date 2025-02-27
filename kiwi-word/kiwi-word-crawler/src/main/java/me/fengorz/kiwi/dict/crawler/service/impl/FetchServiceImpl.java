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

package me.fengorz.kiwi.dict.crawler.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.api.R;
import me.fengorz.kiwi.common.dfs.DfsService;
import me.fengorz.kiwi.common.sdk.constant.GlobalConstants;
import me.fengorz.kiwi.common.sdk.exception.dfs.DfsOperateDeleteException;
import me.fengorz.kiwi.common.sdk.util.lang.collection.KiwiCollectionUtils;
import me.fengorz.kiwi.common.sdk.util.lang.string.KiwiStringUtils;
import me.fengorz.kiwi.dict.crawler.component.producer.base.MqSender;
import me.fengorz.kiwi.dict.crawler.service.FetchService;
import me.fengorz.kiwi.dict.crawler.service.JsoupService;
import me.fengorz.kiwi.dict.crawler.util.CrawlerUtils;
import me.fengorz.kiwi.word.api.common.ApiCrawlerConstants;
import me.fengorz.kiwi.word.api.common.enumeration.CrawlerStatusEnum;
import me.fengorz.kiwi.word.api.common.enumeration.WordTypeEnum;
import me.fengorz.kiwi.word.api.dto.queue.*;
import me.fengorz.kiwi.word.api.dto.queue.result.FetchPhraseResultDTO;
import me.fengorz.kiwi.word.api.dto.queue.result.FetchPhraseRunUpResultDTO;
import me.fengorz.kiwi.word.api.dto.queue.result.FetchWordResultDTO;
import me.fengorz.kiwi.word.api.entity.FetchQueueDO;
import me.fengorz.kiwi.word.api.exception.JsoupFetchConnectException;
import me.fengorz.kiwi.word.api.exception.JsoupFetchPronunciationException;
import me.fengorz.kiwi.word.api.exception.PhraseRemoveException;
import me.fengorz.kiwi.word.api.exception.WordRemoveException;
import me.fengorz.kiwi.word.api.feign.DictFetchApi;
import me.fengorz.kiwi.word.api.feign.QueryApi;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * @Author Kason Zhan @Date 2020/5/20 11:54 PM
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FetchServiceImpl implements FetchService {

    private final JsoupService jsoupService;
    private final DictFetchApi dictFetchApi;
    private final QueryApi queryApi;
    private final MqSender MQSender;
    @Resource(name = "googleCloudStorageService")
    private DfsService dfsService;

    @Override
    public void handle(FetchWordMqDTO messageDTO) {
        final long oldTime = System.currentTimeMillis();
        final String inputWord = messageDTO.getWord();
        final Integer queueId = messageDTO.getQueueId();
        FetchQueueDO queue = new FetchQueueDO().setWordName(inputWord).setQueueId(queueId);
        boolean isUpdate = false;
        try {
            StringBuilder handleLog = new StringBuilder()
                .append(queue.getFetchResult() != null ? queue.getFetchResult() : GlobalConstants.EMPTY)
                .append(GlobalConstants.SYMBOL_LF);
            FetchWordResultDTO fetchWordResultDTO = jsoupService.fetchWordInfo(messageDTO).setQueueId(queueId);
            final String fetchWord = fetchWordResultDTO.getWordName();
            queue.setDerivation(fetchWord);

            R<Void> storeResult = dictFetchApi.storeResult(fetchWordResultDTO);
            if (storeResult.isFail()) {
                handleException(queue, ApiCrawlerConstants.STATUS_FETCH_FAIL, storeResult.getMsg());
            } else {
                if (KiwiStringUtils.isNotEquals(inputWord, fetchWord)) {
                    queryApi.insertVariant(inputWord, fetchWord);
                    String insertVariantResult = KiwiStringUtils
                        .format("word({}) has a variant({}), variant insert success!", fetchWord, inputWord);
                    log.info(insertVariantResult);
                    handleLog.append(insertVariantResult);
                }

                long newTime = System.currentTimeMillis();
                String fetchResult = KiwiStringUtils.format("word({}) fetch store success! spent {}s",
                    queue.getWordName(), (newTime - oldTime));
                log.info(fetchResult);
                handleLog.append(fetchWord);

                // 标记单词基础信息已经抓取完毕，即将抓取发音文件
                queue.setFetchStatus(CrawlerStatusEnum.STATUS_TO_FETCH_PRONUNCIATION.getStatus());
                queue.setFetchResult(handleLog.toString());
                queue.setIsLock(GlobalConstants.FLAG_YES);
            }
            isUpdate = true;
        } catch (JsoupFetchConnectException e) {
            handleException(queue, CrawlerStatusEnum.STATUS_JSOUP_CONNECT_FAILED.getStatus(), e.getMessage());
            isUpdate = true;
        } catch (Exception e) {
            handleException(queue, ApiCrawlerConstants.STATUS_FETCH_FAIL, e.getMessage());
            isUpdate = true;
        } finally {
            if (isUpdate) {
                queue.setIsIntoCache(GlobalConstants.FLAG_NO);
                dictFetchApi.updateQueueById(queue);
            }
        }
    }

    @Override
    public void handle(FetchPronunciationMqDTO dto) {
        FetchQueueDO queue = new FetchQueueDO().setQueueId(Objects.requireNonNull(dto.getQueueId()));
        boolean isUpdate = false;
        try {
            R<Boolean> response = Optional.of(dictFetchApi.fetchPronunciation(Objects.requireNonNull(dto.getWordId()))).get();
            if (response.isSuccess()) {
                queue.setIsLock(GlobalConstants.FLAG_NO);
                queue.setFetchStatus(ApiCrawlerConstants.STATUS_ALL_SUCCESS);
            } else {
                throw new JsoupFetchPronunciationException();
            }
            isUpdate = true;
        } catch (Exception e) {
            this.handleException(queue, ApiCrawlerConstants.STATUS_TO_FETCH_PRONUNCIATION_FAIL,
                "fetch pronunciation error!");
            isUpdate = true;
        } finally {
            if (isUpdate) {
                dictFetchApi.updateQueueById(queue);
            }
        }
    }

    @Override
    public void removeWord(RemoveMqDTO dto) {
        FetchQueueDO queue = new FetchQueueDO().setQueueId(Objects.requireNonNull(dto.getQueueId()));
        boolean isUpdate = false;
        try {
            R<List<RemovePronunciatioinMqDTO>> response = Optional.of(dictFetchApi.removeWord(dto.getQueueId())).get();
            if (response.isSuccess()) {
                // 删除完老的基础数据重新开始抓取单词
                queue.setFetchStatus(CrawlerStatusEnum.STATUS_TO_FETCH.getStatus());
                queue.setWordId(0);
                queue.setIsLock(GlobalConstants.FLAG_YES);
                response.getData().forEach(MQSender::removePronunciation);
            } else {
                throw new WordRemoveException(queue.getWordName());
            }
            isUpdate = true;
        } catch (Exception e) {
            queue.setWordId(0);
            this.handleException(queue, ApiCrawlerConstants.STATUS_DEL_BASE_FAIL, "remove word error!");
            isUpdate = true;
        } finally {
            if (isUpdate) {
                dictFetchApi.updateQueueById(queue);
            }
        }
    }

    @Override
    public void handle(RemovePronunciatioinMqDTO dto) {
        FetchQueueDO queue = new FetchQueueDO().setQueueId(Objects.requireNonNull(dto.getQueueId()));
        boolean isUpdate = false;
        try {
            dfsService.deleteFile(dto.getGroupName(), dto.getVoiceFilePath());
        } catch (DfsOperateDeleteException e) {
            this.handleException(queue, ApiCrawlerConstants.STATUS_DEL_PRONUNCIATION_FAIL, "del pronunciation error!");
            isUpdate = true;
        } finally {
            if (isUpdate) {
                dictFetchApi.updateQueueById(queue);
            }
        }
    }

    @Override
    public void retrievePhrase(FetchPhraseRunUpMqDTO dto) {
        FetchQueueDO queue = new FetchQueueDO().setQueueId(Objects.requireNonNull(dto.getQueueId()));
        boolean isUpdate = false;
        try {
            FetchPhraseRunUpResultDTO resultDTO = jsoupService.fetchPhraseRunUp(dto);
            if (KiwiCollectionUtils.isNotEmpty(resultDTO.getPhrases())) {
                dictFetchApi.handlePhrasesFetchResult(resultDTO);
            }
            queue.setIsLock(GlobalConstants.FLAG_NO);
            queue.setFetchStatus(ApiCrawlerConstants.STATUS_PERFECT_SUCCESS);
            isUpdate = true;
        } catch (JsoupFetchConnectException e) {
            this.handleException(queue, ApiCrawlerConstants.STATUS_FETCH_RELATED_PHRASE_FAIL,
                "fetch related phrase error!");
            isUpdate = true;
        } finally {
            if (isUpdate) {
                dictFetchApi.updateQueueById(queue);
            }
        }
    }

    @Override
    public void handle(FetchPhraseMqDTO dto) {
        FetchQueueDO queue = new FetchQueueDO().setQueueId(Objects.requireNonNull(dto.getQueueId()));
        boolean isUpdate = false;
        try {
            FetchPhraseResultDTO resultDTO = jsoupService.fetchPhraseInfo(dto);
            if (CrawlerUtils.is2word(resultDTO)) {
                queue.setInfoType(WordTypeEnum.WORD.getType());
                queue.setFetchStatus(CrawlerStatusEnum.STATUS_TO_FETCH.getStatus());
                queue.setFetchTime(0);
            } else {
                dictFetchApi.storePhrasesFetchResult(resultDTO);
                queue.setFetchStatus(ApiCrawlerConstants.STATUS_PERFECT_SUCCESS);
            }
            queue.setIsLock(GlobalConstants.FLAG_YES);
            isUpdate = true;
        } catch (Exception e) {
            this.handleException(queue, ApiCrawlerConstants.STATUS_FETCH_PHRASE_FAIL, "fetch phrase real info error!");
            isUpdate = true;
        } finally {
            if (isUpdate) {
                queue.setIsIntoCache(GlobalConstants.FLAG_NO);
                dictFetchApi.updateQueueById(queue);
            }
        }
    }

    @Override
    public void removePhrase(RemoveMqDTO dto) {
        FetchQueueDO queue = new FetchQueueDO().setQueueId(Objects.requireNonNull(dto.getQueueId()));
        boolean isUpdate = false;
        try {
            R<Boolean> response = Optional.of(dictFetchApi.removePhrase(dto.getQueueId())).get();
            if (response.isSuccess()) {
                // 删除完老的基础数据重新开始抓取单词
                queue.setFetchStatus(CrawlerStatusEnum.STATUS_TO_FETCH.getStatus());
                queue.setWordId(0);
                queue.setIsLock(GlobalConstants.FLAG_YES);
                isUpdate = true;
            } else {
                throw new PhraseRemoveException();
            }
        } catch (Exception e) {
            queue.setWordId(0);
            this.handleException(queue, ApiCrawlerConstants.STATUS_DEL_PHRASE_FAIL, "remove phrase error!");
            isUpdate = true;
        } finally {
            if (isUpdate) {
                dictFetchApi.updateQueueById(queue);
            }
        }
    }

    private void handleException(FetchQueueDO queue, int status, String message) {
        queue.setFetchStatus(status);
        queue.setFetchResult(queue.getFetchResult() == null ? message : queue.getFetchResult() + message);
        queue.setIsLock(GlobalConstants.FLAG_NO);
        if (KiwiStringUtils.isNotBlank(message)) {
            log.error(message);
        }
    }
}
