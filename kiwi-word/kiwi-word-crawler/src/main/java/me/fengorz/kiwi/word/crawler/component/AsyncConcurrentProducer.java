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

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.api.R;
import me.fengorz.kiwi.common.api.constant.CommonConstants;
import me.fengorz.kiwi.common.sdk.util.bean.KiwiBeanUtils;
import me.fengorz.kiwi.word.api.common.WordCrawlerConstants;
import me.fengorz.kiwi.word.api.dto.fetch.WordMessageDTO;
import me.fengorz.kiwi.word.api.dto.remote.WordFetchQueuePageDTO;
import me.fengorz.kiwi.word.api.entity.WordFetchQueueDO;
import me.fengorz.kiwi.word.api.feign.IRemoteWordFetchService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * @Description TODO
 * @Author codingByFeng
 * @Date 2019/10/30 10:33 AM
 */
@Component
@Slf4j
@AllArgsConstructor
public class AsyncConcurrentProducer {

    private final WordFetchProducer wordFetchProducer;
    private final IRemoteWordFetchService remoteWordFetchService;

    public void fetch() {
        WordFetchQueueDO wordFetchQueue = new WordFetchQueueDO()
                .setFetchStatus(WordCrawlerConstants.STATUS_TO_FETCH)
                .setIsValid(CommonConstants.TRUE);
        WordFetchQueuePageDTO wordFetchQueuePage = new WordFetchQueuePageDTO().
                setWordFetchQueue(wordFetchQueue).setPage(new Page(1, 20));
        R result = remoteWordFetchService.getWordFetchQueuePage(wordFetchQueuePage);

        if (result != null && result.isOK()) {
            Optional.ofNullable(result.getData()).ifPresent(o -> {
                Map map = (Map) o;
                List<LinkedHashMap> list = (List<LinkedHashMap>) map.get(WordCrawlerConstants.RECORDS);
                list.forEach(hashMap -> {
                    WordFetchQueueDO word = (WordFetchQueueDO) KiwiBeanUtils.mapConvertPOJO(hashMap, WordFetchQueueDO.class);
                    this.fetchWord(word);
                });
            });
        }


    }

    /**
     * 异步调用爬虫待抓取队列的消息发送
     *
     * @param wordFetchQueue
     */
    @Async
    public void fetchWord(WordFetchQueueDO wordFetchQueue) {
        wordFetchQueue.setFetchStatus(WordCrawlerConstants.STATUS_FETCHING);
        R updateResult = this.remoteWordFetchService.updateQueueById(wordFetchQueue);
        if (Objects.nonNull(updateResult) && updateResult.isOK()) {
            this.wordFetchProducer.send(new WordMessageDTO(wordFetchQueue.getWordName()));
        }
    }

}
