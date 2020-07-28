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

package me.fengorz.kiwi.word.api.feign.fallback;

import java.util.List;

import org.springframework.stereotype.Component;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.api.R;
import me.fengorz.kiwi.word.api.dto.queue.fetch.FetchWordResultDTO;
import me.fengorz.kiwi.word.api.dto.remote.WordFetchQueuePageDTO;
import me.fengorz.kiwi.word.api.entity.WordFetchQueueDO;
import me.fengorz.kiwi.word.api.feign.IWordFetchAPI;

/**
 * @Description TODO
 * @Author zhanshifeng
 * @Date 2019/10/30 3:20 PM
 */
@Slf4j
@Component
public class WordFetchAPIFallback implements IWordFetchAPI {

    @Setter
    private Throwable throwable;

    @Override
    public R<List<WordFetchQueueDO>> pageQueue(WordFetchQueuePageDTO dto) {
        log.error("getWordFetchQueuePage error, wordFetchQueuePage=" + dto, throwable);
        return R.feignCallFailed();
    }

    @Override
    public R<Boolean> updateQueueById(WordFetchQueueDO queueDO) {
        log.error("update wordFetchQueue error, wordFetchQueue=" + queueDO, throwable);
        return R.feignCallFailed();
    }

    @Override
    public R<Void> storeResult(FetchWordResultDTO dto) {
        log.error("update storeFetchWordResult error, fetchWordResultDTO=" + dto, throwable);
        // TODO ZSF This method of R applies to the template file
        return R.feignCallFailed();
    }

    @Override
    public R<Boolean> updateByWordName(WordFetchQueueDO queueDO) {
        log.error("updateByWordName error, wordFetchQueue=" + queueDO, throwable);
        return R.feignCallFailed(throwable.getMessage());
    }

    @Override
    public R<Boolean> invalid(String wordName) {
        return R.feignCallFailed();
    }

    @Override
    public R<Boolean> lock(String wordName) {
        return R.feignCallFailed();
    }
}
