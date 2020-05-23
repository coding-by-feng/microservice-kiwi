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

package me.fengorz.kiwi.word.crawler.service.impl;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.api.R;
import me.fengorz.kiwi.common.sdk.util.lang.string.KiwiStringUtils;
import me.fengorz.kiwi.word.api.common.WordCrawlerConstants;
import me.fengorz.kiwi.word.api.dto.fetch.FetchWordResultDTO;
import me.fengorz.kiwi.word.api.dto.fetch.WordMessageDTO;
import me.fengorz.kiwi.word.api.entity.WordFetchQueueDO;
import me.fengorz.kiwi.word.api.exception.JsoupFetchConnectRuntimeException;
import me.fengorz.kiwi.word.api.exception.JsoupFetchResultRuntimeException;
import me.fengorz.kiwi.word.api.feign.IRemoteWordFetchService;
import me.fengorz.kiwi.word.crawler.service.IJsoupService;
import me.fengorz.kiwi.word.crawler.service.IWordFetchService;
import org.springframework.stereotype.Service;

/**
 * @Author zhanshifeng
 * @Date 2020/5/20 11:54 PM
 */
@Slf4j
@Service
@AllArgsConstructor
public class WordFetchServiceImpl implements IWordFetchService {

    private final IJsoupService jsoupService;
    private final IRemoteWordFetchService remoteWordFetchService;

    @Override
    public void work(WordMessageDTO wordMessageDTO) {
        final String word = wordMessageDTO.getWord();
        WordFetchQueueDO wordFetchQueue = new WordFetchQueueDO()
                .setWordName(word).setFetchStatus(WordCrawlerConstants.STATUS_SUCCESS);
        try {
            long oldTime = System.currentTimeMillis();
            FetchWordResultDTO fetchWordResultDTO = jsoupService.fetchWordInfo(wordMessageDTO);

            // TODO ZSF 增加一个时态、单复数的变化关系对应逻辑


            // All exceptions are called back to the data in the word_fetch_queue
            R storeResult = remoteWordFetchService.storeFetchWordResult(fetchWordResultDTO);
            if (storeResult.isFail()) {
                subDealException(wordFetchQueue, storeResult.getCode().value(), storeResult.getMsg());
            } else {
                long newTime = System.currentTimeMillis();
                log.info("word({}) fetch store success! spent {}s", wordFetchQueue.getWordName(), (newTime - oldTime));
                remoteWordFetchService.invalid(wordFetchQueue.getWordName());
                log.info("word({}) fetch queue del success!", wordFetchQueue.getWordName());
                wordFetchQueue.setFetchStatus(WordCrawlerConstants.STATUS_SUCCESS);
                wordFetchQueue.setFetchResult(KiwiStringUtils.format("word({}) fetch store success! spent {}s", word));
            }
        } catch (JsoupFetchConnectRuntimeException e) {
            subDealException(wordFetchQueue, WordCrawlerConstants.STATUS_ERROR_JSOUP_FETCH_CONNECT_FAILED, e.getMessage());
        } catch (JsoupFetchResultRuntimeException e) {
            subDealException(wordFetchQueue, WordCrawlerConstants.STATUS_ERROR_JSOUP_RESULT_FETCH_FAILED, e.getMessage());
        } finally {
            remoteWordFetchService.updateByWordName(wordFetchQueue);
        }
    }

    private void subDealException(WordFetchQueueDO wordFetchQueue, int status, String message) {
        wordFetchQueue.setFetchStatus(status);
        wordFetchQueue.setFetchResult(message);
        if (KiwiStringUtils.isNotBlank(message)) {
            log.error(message);
        }
    }
}
