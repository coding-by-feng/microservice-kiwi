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

import com.baomidou.mybatisplus.extension.api.R;
import me.fengorz.kiwi.word.api.common.WordConstants;
import me.fengorz.kiwi.word.api.dto.queue.RemovePronunciatioinMqDTO;
import me.fengorz.kiwi.word.api.dto.queue.fetch.FetchWordResultDTO;
import me.fengorz.kiwi.word.api.entity.WordFetchQueueDO;
import me.fengorz.kiwi.word.api.feign.factory.WordFetchFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @Author zhanshifeng
 */
@FeignClient(contextId = "wordFetchApi", value = WordConstants.KIWI_WORD_BIZ,
    fallbackFactory = WordFetchFallbackFactory.class)
public interface IWordFetchAPI {

    String WORD_FETCH_QUEUE = "/word/fetch";

    @GetMapping(WORD_FETCH_QUEUE + "/pageQueue/{status}/{current}/{size}")
    R<List<WordFetchQueueDO>> pageQueue(@PathVariable Integer status, @PathVariable Integer current,
                                        @PathVariable Integer size);

    @GetMapping(WORD_FETCH_QUEUE + "/pageQueueLockIn/{status}/{current}/{size}")
    R<List<WordFetchQueueDO>> pageQueueLockIn(@PathVariable Integer status, @PathVariable Integer current,
        @PathVariable Integer size);

    @PostMapping(WORD_FETCH_QUEUE + "/updateById")
    R<Boolean> updateQueueById(@RequestBody WordFetchQueueDO queueDO);

    @PostMapping(WORD_FETCH_QUEUE + "/storeResult")
    R<Void> storeResult(@RequestBody FetchWordResultDTO dto);

    @GetMapping(WORD_FETCH_QUEUE + "/fetchPronunciation/{wordId}")
    R<Boolean> fetchPronunciation(@PathVariable Integer wordId);

    @GetMapping(WORD_FETCH_QUEUE + "/removeWord/{wordName}/{queueId}")
    R<List<RemovePronunciatioinMqDTO>> removeWord(@PathVariable String wordName, @PathVariable Integer queueId);

    @PostMapping(WORD_FETCH_QUEUE + "/lock")
    R<Boolean> lock(@RequestParam String wordName);

    @PostMapping(WORD_FETCH_QUEUE + "/updateByWordName")
    R<Boolean> updateByWordName(@RequestBody WordFetchQueueDO queueDO);

    @PostMapping(WORD_FETCH_QUEUE + "/invalid")
    R<Boolean> invalid(@RequestParam String wordName);

}
