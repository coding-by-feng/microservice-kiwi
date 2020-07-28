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

package me.fengorz.kiwi.word.api.feign;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import me.fengorz.kiwi.common.api.R;
import me.fengorz.kiwi.word.api.common.WordConstants;
import me.fengorz.kiwi.word.api.dto.queue.fetch.FetchWordResultDTO;
import me.fengorz.kiwi.word.api.dto.remote.WordFetchQueuePageDTO;
import me.fengorz.kiwi.word.api.entity.WordFetchQueueDO;
import me.fengorz.kiwi.word.api.feign.factory.WordFetchFallbackFactory;

/**
 * @Author zhanshifeng
 */
@FeignClient(contextId = "remoteWordFetchService", value = WordConstants.KIWI_WORD_BIZ,
    fallbackFactory = WordFetchFallbackFactory.class)
public interface IWordFetchAPI {

    String WORD_FETCH_QUEUE = "/word/fetch";

    @PostMapping(WORD_FETCH_QUEUE + "/pageQueue")
    R<List<WordFetchQueueDO>> pageQueue(@RequestBody WordFetchQueuePageDTO dto);

    @PutMapping(WORD_FETCH_QUEUE + "/updateById")
    R<Boolean> updateQueueById(@RequestBody WordFetchQueueDO queueDO);

    @PostMapping(WORD_FETCH_QUEUE + "/storeFetchWordResult")
    R<Void> storeResult(@RequestBody FetchWordResultDTO dto);

    @Deprecated
    @PostMapping(WORD_FETCH_QUEUE + "/updateByWordName")
    R<Boolean> updateByWordName(@RequestBody WordFetchQueueDO queueDO);

    @Deprecated
    @PostMapping(WORD_FETCH_QUEUE + "/invalid")
    R<Boolean> invalid(@RequestParam String wordName);

    @PostMapping(WORD_FETCH_QUEUE + "/lock")
    R<Boolean> lock(@RequestParam String wordName);

}
