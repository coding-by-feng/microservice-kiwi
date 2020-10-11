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

package me.fengorz.kiwi.vocabulary.crawler.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.api.R;
import me.fengorz.kiwi.common.api.constant.CommonConstants;
import me.fengorz.kiwi.common.api.exception.dfs.DfsOperateDeleteException;
import me.fengorz.kiwi.common.fastdfs.service.IDfsService;
import me.fengorz.kiwi.common.sdk.util.lang.collection.KiwiCollectionUtils;
import me.fengorz.kiwi.common.sdk.util.lang.string.KiwiStringUtils;
import me.fengorz.kiwi.vocabulary.crawler.component.producer.base.ISender;
import me.fengorz.kiwi.vocabulary.crawler.service.IFetchService;
import me.fengorz.kiwi.vocabulary.crawler.service.IJsoupService;
import me.fengorz.kiwi.word.api.common.WordCrawlerConstants;
import me.fengorz.kiwi.word.api.dto.queue.*;
import me.fengorz.kiwi.word.api.dto.queue.result.FetchPhraseResultDTO;
import me.fengorz.kiwi.word.api.dto.queue.result.FetchPhraseRunUpResultDTO;
import me.fengorz.kiwi.word.api.dto.queue.result.FetchWordResultDTO;
import me.fengorz.kiwi.word.api.entity.FetchQueueDO;
import me.fengorz.kiwi.word.api.exception.JsoupFetchConnectException;
import me.fengorz.kiwi.word.api.exception.JsoupFetchPronunciationException;
import me.fengorz.kiwi.word.api.exception.WordRemoveException;
import me.fengorz.kiwi.word.api.feign.IPhraseBizAPI;
import me.fengorz.kiwi.word.api.feign.IWordBizAPI;
import me.fengorz.kiwi.word.api.feign.IWordMainVariantAPI;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * @Author zhanshifeng
 * @Date 2020/5/20 11:54 PM
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FetchServiceImpl implements IFetchService {

    private final IJsoupService jsoupService;
    private final IWordBizAPI wordBizAPI;
    private final IPhraseBizAPI phraseBizAPI;
    private final IWordMainVariantAPI wordVariantAPI;
    private final ISender sender;
    private final IDfsService dfsService;

    @Override
    public void handle(FetchWordMqDTO messageDTO) {
        final long oldTime = System.currentTimeMillis();
        final String inputWord = messageDTO.getWord();
        final Integer queueId = messageDTO.getQueueId();
        FetchQueueDO queue = new FetchQueueDO().setWordName(inputWord).setQueueId(queueId);

        try {
            StringBuilder handleLog = new StringBuilder().append(queue.getFetchResult() != null ? queue.getFetchResult() : CommonConstants.EMPTY).append(CommonConstants.SYMBOL_LF);
            FetchWordResultDTO fetchWordResultDTO = jsoupService.fetchWordInfo(messageDTO).setQueueId(queueId);
            final String fetchWord = fetchWordResultDTO.getWordName();
            queue.setDerivation(fetchWord);

            R<Void> storeResult = wordBizAPI.storeResult(fetchWordResultDTO);
            if (storeResult.isFail()) {
                handleException(queue, WordCrawlerConstants.STATUS_FETCH_FAIL, storeResult.getMsg());
            } else {
                if (KiwiStringUtils.isNotEquals(inputWord, fetchWord)) {
                    wordVariantAPI.insertVariant(inputWord, fetchWord);
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
                queue.setFetchStatus(WordCrawlerConstants.STATUS_TO_FETCH_PRONUNCIATION);
                queue.setFetchResult(handleLog.toString());
                queue.setIsLock(CommonConstants.FLAG_YES);
            }
        } catch (JsoupFetchConnectException e) {
            handleException(queue, WordCrawlerConstants.STATUS_JSOUP_CONNECT_FAILED, e.getMessage());
        } catch (Exception e) {
            handleException(queue, WordCrawlerConstants.STATUS_FETCH_FAIL, e.getMessage());
        } finally {
            wordBizAPI.updateQueueById(queue);
        }
    }

    @Override
    public void handle(FetchPronunciationMqDTO dto) {
        FetchQueueDO queue = new FetchQueueDO().setQueueId(Objects.requireNonNull(dto.getQueueId()));
        try {
            R<Boolean> response =
                    Optional.of(wordBizAPI.fetchPronunciation(Objects.requireNonNull(dto.getWordId()))).get();
            if (response.isSuccess()) {
                queue.setIsLock(CommonConstants.FLAG_NO);
                queue.setFetchStatus(WordCrawlerConstants.STATUS_ALL_SUCCESS);
            } else {
                throw new JsoupFetchPronunciationException();
            }
        } catch (Exception e) {
            this.handleException(queue, WordCrawlerConstants.STATUS_TO_FETCH_PRONUNCIATION_FAIL, "fetch pronunciation error!");
        }
        wordBizAPI.updateQueueById(queue);
    }


    @Override
    public void handle(RemoveWordMqDTO dto) {
        FetchQueueDO queue = new FetchQueueDO().setQueueId(Objects.requireNonNull(dto.getQueueId()));
        try {
            R<List<RemovePronunciatioinMqDTO>> response =
                    Optional.of(wordBizAPI.removeWord(dto.getQueueId())).get();
            if (response.isSuccess()) {
                // 删除完老的基础数据重新开始抓取单词
                queue.setFetchStatus(WordCrawlerConstants.STATUS_TO_FETCH);
                queue.setWordId(0);
                queue.setIsLock(CommonConstants.FLAG_YES);
                response.getData().forEach(sender::removePronunciation);
            } else {
                throw new WordRemoveException();
            }
        } catch (Exception e) {
            this.handleException(queue, WordCrawlerConstants.STATUS_DEL_BASE_FAIL, "remove word error!");
        }
        wordBizAPI.updateQueueById(queue);
    }

    @Override
    public void handle(RemovePronunciatioinMqDTO dto) {
        try {
            dfsService.deleteFile(dto.getGroupName(), dto.getVoiceFilePath());
        } catch (DfsOperateDeleteException e) {
            FetchQueueDO queue = new FetchQueueDO().setQueueId(Objects.requireNonNull(dto.getQueueId()));
            this.handleException(queue, WordCrawlerConstants.STATUS_DEL_PRONUNCIATION_FAIL, "del pronunciation error!");
            wordBizAPI.updateQueueById(queue);
        }
    }

    @Override
    public void retrievePhrase(FetchPhraseRunUpMqDTO dto) {
        FetchQueueDO queue = new FetchQueueDO().setQueueId(Objects.requireNonNull(dto.getQueueId()));
        try {
            FetchPhraseRunUpResultDTO resultDTO = jsoupService.fetchPhraseRunUp(dto);
            if (KiwiCollectionUtils.isNotEmpty(resultDTO.getPhrases())) {
                phraseBizAPI.handlePhrasesFetchResult(resultDTO);
            }
            queue.setIsLock(CommonConstants.FLAG_NO);
            queue.setFetchStatus(WordCrawlerConstants.STATUS_PERFECT_SUCCESS);
        } catch (JsoupFetchConnectException e) {
            this.handleException(queue, WordCrawlerConstants.STATUS_FETCH_RELATED_PHRASE_FAIL, "fetch related phrase error!");
        } finally {
            wordBizAPI.updateQueueById(queue);
        }
    }

    @Override
    public void handle(FetchPhraseMqDTO dto) {
        FetchQueueDO queue = new FetchQueueDO().setQueueId(Objects.requireNonNull(dto.getQueueId()));
        try {
            FetchPhraseResultDTO resultDTO = jsoupService.fetchPhraseInfo(dto);
            phraseBizAPI.storePhrasesFetchResult(resultDTO);
            queue.setIsLock(CommonConstants.FLAG_YES);
            queue.setFetchStatus(WordCrawlerConstants.STATUS_PERFECT_SUCCESS);
        } catch (Exception e) {
            this.handleException(queue, WordCrawlerConstants.STATUS_FETCH_PHRASE_FAIL, "fetch phrase real info error!");
        } finally {
            wordBizAPI.updateQueueById(queue);
        }
    }

    private void handleException(FetchQueueDO queue, int status, String message) {
        queue.setFetchStatus(status);
        queue.setFetchResult(queue.getFetchResult() == null ? message : queue.getFetchResult() + message);
        queue.setIsLock(CommonConstants.FLAG_NO);
        if (KiwiStringUtils.isNotBlank(message)) {
            log.error(message);
        }
    }
}
