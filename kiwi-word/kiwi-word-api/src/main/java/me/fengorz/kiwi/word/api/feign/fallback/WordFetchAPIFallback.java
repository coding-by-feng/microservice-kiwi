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

import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.core.metadata.IPage;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.api.R;
import me.fengorz.kiwi.word.api.dto.fetch.FetchWordResultDTO;
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
    public R<IPage<WordFetchQueueDO>> getWordFetchQueuePage(WordFetchQueuePageDTO wordFetchQueuePage) {
        log.error("getWordFetchQueuePage error, wordFetchQueuePage=" + wordFetchQueuePage, throwable);
        return R.feignCallFailed();
    }

    @Override
    public R updateQueueById(WordFetchQueueDO wordFetchQueue) {
        log.error("update wordFetchQueue error, wordFetchQueue=" + wordFetchQueue, throwable);
        return R.feignCallFailed();
    }

    @Override
    public R storeFetchWordResult(FetchWordResultDTO fetchWordResultDTO) {
        log.error("update storeFetchWordResult error, fetchWordResultDTO=" + fetchWordResultDTO, throwable);
        // TODO ZSF This method of R applies to the template file
        return R.feignCallFailed();
    }

    @Override
    public R updateByWordName(WordFetchQueueDO wordFetchQueue) {
        log.error("updateByWordName error, wordFetchQueue=" + wordFetchQueue, throwable);
        return R.feignCallFailed(throwable.getMessage());
    }

    @Override
    public R invalid(String wordName) {
        return R.feignCallFailed();
    }

    @Override
    public R lock(String wordName) {
        return R.feignCallFailed();
    }
}
