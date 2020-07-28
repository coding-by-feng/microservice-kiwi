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

package me.fengorz.kiwi.word.crawler.service.impl;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.api.R;
import me.fengorz.kiwi.common.api.constant.CommonConstants;
import me.fengorz.kiwi.common.sdk.util.lang.string.KiwiStringUtils;
import me.fengorz.kiwi.word.api.common.WordCrawlerConstants;
import me.fengorz.kiwi.word.api.dto.queue.WordFetchMessageDTO;
import me.fengorz.kiwi.word.api.dto.queue.fetch.FetchWordResultDTO;
import me.fengorz.kiwi.word.api.entity.WordFetchQueueDO;
import me.fengorz.kiwi.word.api.exception.JsoupFetchConnectException;
import me.fengorz.kiwi.word.api.feign.IWordFetchAPI;
import me.fengorz.kiwi.word.api.feign.IWordMainVariantAPI;
import me.fengorz.kiwi.word.crawler.service.IJsoupService;
import me.fengorz.kiwi.word.crawler.service.IWordFetchService;

/**
 * @Author zhanshifeng
 * @Date 2020/5/20 11:54 PM
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WordFetchServiceImpl implements IWordFetchService {

    private final IJsoupService jsoupService;
    private final IWordFetchAPI wordFetchAPI;
    private final IWordMainVariantAPI wordMainVariantAPIService;

    @Override
    public void handle(WordFetchMessageDTO messageDTO) {
        final long oldTime = System.currentTimeMillis();
        final String inputWord = messageDTO.getWord();
        final Integer queueId = messageDTO.getQueueId();
        WordFetchQueueDO queue = new WordFetchQueueDO().setWordName(inputWord).setQueueId(queueId);

        try {
            FetchWordResultDTO fetchWordResultDTO = jsoupService.fetchWordInfo(messageDTO).setQueueId(queueId);
            final String fetchWord = fetchWordResultDTO.getWordName();
            StringBuilder handleLog = new StringBuilder();

            R<Void> storeResult = wordFetchAPI.storeResult(fetchWordResultDTO);
            if (storeResult.isFail()) {
                handleException(queue, WordCrawlerConstants.STATUS_FETCH_FAIL, storeResult.getMsg());
            } else {
                // TODO ZSF 增加一个时态、单复数的变化关系对应逻辑
                if (KiwiStringUtils.isNotEquals(inputWord, fetchWordResultDTO.getWordName())) {
                    wordMainVariantAPIService.insertVariant(inputWord, fetchWord);
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

                queue.setFetchStatus(WordCrawlerConstants.STATUS_TO_FETCH_PRONUNCIATION);
                queue.setFetchResult(handleLog.toString());
                queue.setIsLock(CommonConstants.FLAG_NO);
            }
        } catch (JsoupFetchConnectException e) {
            handleException(queue, WordCrawlerConstants.STATUS_JSOUP_CONNECT_FAILED, e.getMessage());
        } catch (Exception e) {
            handleException(queue, WordCrawlerConstants.STATUS_FETCH_WORD_FAIL, e.getMessage());
        } finally {
            wordFetchAPI.updateQueueById(queue);
        }
    }

    private void handleException(WordFetchQueueDO queue, int status, String message) {
        queue.setFetchStatus(status);
        queue.setFetchResult(message);
        queue.setIsLock(CommonConstants.FLAG_YES);
        if (KiwiStringUtils.isNotBlank(message)) {
            log.error(message);
        }
    }
}
