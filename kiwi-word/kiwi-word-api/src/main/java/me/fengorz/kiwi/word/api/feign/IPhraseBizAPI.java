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
import me.fengorz.kiwi.word.api.dto.queue.result.FetchPhraseResultDTO;
import me.fengorz.kiwi.word.api.dto.queue.result.FetchPhraseRunUpResultDTO;
import me.fengorz.kiwi.word.api.feign.factory.WordMainFallBackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 单词主表
 *
 * @author zhanshifeng
 * @date 2019-11-01 14:29:33
 */
@FeignClient(contextId = "phraseBizAPI", value = WordConstants.KIWI_WORD_BIZ,
        fallbackFactory = WordMainFallBackFactory.class)
public interface IPhraseBizAPI {

    String WORD_FETCH_QUEUE = "/word/fetch";

    @PostMapping(WORD_FETCH_QUEUE + "/handlePhrasesFetchResult")
    R<Boolean> handlePhrasesFetchResult(@RequestBody FetchPhraseRunUpResultDTO dto);

    @PostMapping(WORD_FETCH_QUEUE + "/storePhrasesFetchResult")
    R<Boolean> storePhrasesFetchResult(@RequestBody FetchPhraseResultDTO dto);


}