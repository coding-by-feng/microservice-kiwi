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

package me.fengorz.kiwi.word.api.feign;

import me.fengorz.kiwi.common.api.R;
import me.fengorz.kiwi.word.api.common.WordCrawlerConstants;
import me.fengorz.kiwi.word.api.dto.fetch.FetchWordResultDTO;
import me.fengorz.kiwi.word.api.dto.remote.WordFetchQueuePageDTO;
import me.fengorz.kiwi.word.api.entity.WordFetchQueueDO;
import me.fengorz.kiwi.word.api.feign.factory.RemoteWordFetchServiceFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(contextId = "remoteWordFetchService", value = WordCrawlerConstants.VOCABULARY_ENHANCER_CRAWLER_BIZ, fallbackFactory = RemoteWordFetchServiceFallbackFactory.class)
public interface IRemoteWordFetchService {

    String WORD_FETCH_QUEUE = "/word/fetch/queue";

    @PostMapping(WORD_FETCH_QUEUE + "/getWordFetchQueuePage")
    R getWordFetchQueuePage(@RequestBody WordFetchQueuePageDTO wordFetchQueuePage);

    @PutMapping(WORD_FETCH_QUEUE + "/updateById")
    R updateQueueById(@RequestBody WordFetchQueueDO wordFetchQueue);

    @PostMapping(WORD_FETCH_QUEUE + "/storeFetchWordResult")
    R storeFetchWordResult(@RequestBody FetchWordResultDTO fetchWordResultDTO);

    @PostMapping(WORD_FETCH_QUEUE + "/save")
    R save(@RequestBody WordFetchQueueDO wordFetchQueue);

    @GetMapping(WORD_FETCH_QUEUE + "/fetchNewWord/{wordName}")
    R fetchNewWord(@PathVariable String wordName);

    @PostMapping(WORD_FETCH_QUEUE + "/updateByWordName")
    R updateByWordName(@RequestBody WordFetchQueueDO wordFetchQueue);

}
