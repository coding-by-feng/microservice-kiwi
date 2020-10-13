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

import me.fengorz.kiwi.common.api.R;
import me.fengorz.kiwi.word.api.common.WordConstants;
import me.fengorz.kiwi.word.api.dto.queue.RemovePronunciatioinMqDTO;
import me.fengorz.kiwi.word.api.dto.queue.result.FetchPhraseResultDTO;
import me.fengorz.kiwi.word.api.dto.queue.result.FetchPhraseRunUpResultDTO;
import me.fengorz.kiwi.word.api.dto.queue.result.FetchWordResultDTO;
import me.fengorz.kiwi.word.api.entity.FetchQueueDO;
import me.fengorz.kiwi.word.api.feign.factory.BizFallbackFactory;
import me.fengorz.kiwi.word.api.vo.detail.WordQueryVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @Author zhanshifeng
 */
@FeignClient(contextId = "wordBizAPI", value = WordConstants.KIWI_WORD_BIZ,
        fallbackFactory = BizFallbackFactory.class)
public interface IBizAPI {

    String WORD_FETCH_QUEUE = "/word/fetch";
    String WORD_MAIN = "/word/main";

    @GetMapping(WORD_FETCH_QUEUE + "/getOne/{queueId}")
    R<FetchQueueDO> getOne(@PathVariable Integer queueId);

    @GetMapping(WORD_FETCH_QUEUE + "/getOneByWordName")
    R<FetchQueueDO> getOneByWordName(@RequestParam String wordName);

    @GetMapping(WORD_FETCH_QUEUE + "/pageQueue/{status}/{current}/{size}/{infoType}")
    R<List<FetchQueueDO>> pageQueue(@PathVariable Integer status, @PathVariable Integer current,
                                    @PathVariable Integer size, @PathVariable Integer infoType);

    @GetMapping(WORD_FETCH_QUEUE + "/listNotIntoCache")
    public R<List<FetchQueueDO>> listNotIntoCache();

    @GetMapping(WORD_FETCH_QUEUE + "/pageQueueLockIn/{status}/{current}/{size}/{infoType}")
    R<List<FetchQueueDO>> pageQueueLockIn(@PathVariable Integer status, @PathVariable Integer current,
                                          @PathVariable Integer size, @PathVariable Integer infoType);

    @PostMapping(WORD_FETCH_QUEUE + "/updateById")
    R<Boolean> updateQueueById(@RequestBody FetchQueueDO queueDO);

    @PostMapping(WORD_FETCH_QUEUE + "/storeResult")
    R<Void> storeResult(@RequestBody FetchWordResultDTO dto);

    @GetMapping(WORD_FETCH_QUEUE + "/fetchPronunciation/{wordId}")
    R<Boolean> fetchPronunciation(@PathVariable Integer wordId);

    @Deprecated
    @GetMapping(WORD_FETCH_QUEUE + "/removeWord/{wordName}/{queueId}")
    R<List<RemovePronunciatioinMqDTO>> removeWord(@PathVariable String wordName, @PathVariable Integer queueId);

    @GetMapping(WORD_FETCH_QUEUE + "/removeWord/{queueId}")
    R<List<RemovePronunciatioinMqDTO>> removeWord(@PathVariable Integer queueId);

    @GetMapping(WORD_FETCH_QUEUE + "/removePhrase/{queueId}")
    R<List<RemovePronunciatioinMqDTO>> removePhrase(@PathVariable Integer queueId);

    @Deprecated
    @PostMapping(WORD_FETCH_QUEUE + "/lock")
    R<Boolean> lock(@RequestParam String wordName);

    @PostMapping(WORD_FETCH_QUEUE + "/updateByWordName")
    R<Boolean> updateByWordName(@RequestBody FetchQueueDO queueDO);

    @GetMapping(WORD_MAIN + "/listOverlapInUnLock")
    R<List<String>> listOverlapInUnLock();

    @PostMapping(WORD_FETCH_QUEUE + "/handlePhrasesFetchResult")
    R<Boolean> handlePhrasesFetchResult(@RequestBody FetchPhraseRunUpResultDTO dto);

    @PostMapping(WORD_FETCH_QUEUE + "/storePhrasesFetchResult")
    R<Boolean> storePhrasesFetchResult(@RequestBody FetchPhraseResultDTO dto);

    @GetMapping("/query/{wordName}")
    R<WordQueryVO> queryWord(@PathVariable String wordName);

}
