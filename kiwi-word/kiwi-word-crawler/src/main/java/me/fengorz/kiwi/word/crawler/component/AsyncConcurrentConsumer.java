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

package me.fengorz.kiwi.word.crawler.component;

import cn.hutool.core.util.StrUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.api.R;
import me.fengorz.kiwi.word.api.common.WordCrawlerConstants;
import me.fengorz.kiwi.word.api.dto.fetch.FetchWordResultDTO;
import me.fengorz.kiwi.word.api.dto.fetch.WordMessageDTO;
import me.fengorz.kiwi.word.api.entity.WordFetchQueueDO;
import me.fengorz.kiwi.word.api.exception.JsoupFetchConnectException;
import me.fengorz.kiwi.word.api.exception.JsoupFetchResultException;
import me.fengorz.kiwi.word.api.feign.IRemoteWordFetchService;
import me.fengorz.kiwi.word.crawler.service.IJsoupService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * @Description 采用异步机制去抓取数据和存在数据，这样子可以提交爬虫的效率
 * @Author zhanshifeng
 * @Date 2019/11/27 8:21 PM
 */
@Component
@Slf4j
@AllArgsConstructor
public class AsyncConcurrentConsumer {

    private final IJsoupService jsoupService;
    private final IRemoteWordFetchService remoteWordFetchService;

    @Async
    public void asyncFetchWord(WordMessageDTO wordMessageDTO) {
        WordFetchQueueDO wordFetchQueue = new WordFetchQueueDO().setWordName(wordMessageDTO.getWord()).setFetchStatus(WordCrawlerConstants.STATUS_FETCHED);
        try {
            long oldTime = System.currentTimeMillis();
            FetchWordResultDTO fetchWordResultDTO = jsoupService.fetchWordInfo(wordMessageDTO);
            // All exceptions are called back to the data in the word_fetch_queue
            R storeResult = remoteWordFetchService.storeFetchWordResult(fetchWordResultDTO);
            if (storeResult.isFail()) {
                subDealException(wordFetchQueue, storeResult.getCode(), storeResult.getMsg());
            } else {
                long newTime = System.currentTimeMillis();
                log.info("word({}) fetch store success! spent {}s", wordFetchQueue.getWordName(), (newTime - oldTime));
                remoteWordFetchService.removeById(wordMessageDTO.getWord());
                log.info("word({}) fetch queue del success!", wordFetchQueue.getWordName());
            }
        } catch (JsoupFetchConnectException e) {
            subDealException(wordFetchQueue, WordCrawlerConstants.STATUS_ERROR_JSOUP_FETCH_CONNECT_FAILED, e.getMessage());
        } catch (JsoupFetchResultException e) {
            subDealException(wordFetchQueue, WordCrawlerConstants.STATUS_ERROR_JSOUP_RESULT_FETCH_FAILED, e.getMessage());
        }
    }

    private void subDealException(WordFetchQueueDO wordFetchQueue, int status, String message) {
        wordFetchQueue.setFetchStatus(status);
        wordFetchQueue.setFetchResult(message);
        if (StrUtil.isNotBlank(message)) {
            log.error(message);
        }
        remoteWordFetchService.updateByWordName(wordFetchQueue);
    }
}
