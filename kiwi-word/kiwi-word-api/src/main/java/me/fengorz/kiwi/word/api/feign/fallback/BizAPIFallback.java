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

import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.api.R;
import me.fengorz.kiwi.common.sdk.annotation.log.LogMarker;
import me.fengorz.kiwi.word.api.dto.queue.RemovePronunciatioinMqDTO;
import me.fengorz.kiwi.word.api.dto.queue.result.FetchPhraseResultDTO;
import me.fengorz.kiwi.word.api.dto.queue.result.FetchPhraseRunUpResultDTO;
import me.fengorz.kiwi.word.api.dto.queue.result.FetchWordResultDTO;
import me.fengorz.kiwi.word.api.entity.FetchQueueDO;
import me.fengorz.kiwi.word.api.feign.IBizAPI;
import me.fengorz.kiwi.word.api.vo.detail.WordQueryVO;

/**
 * @Author zhanshifeng @Date 2019/10/30 3:20 PM
 */
@Slf4j
@Component
public class BizAPIFallback extends AbstractFallback implements IBizAPI {

    @Override
    @LogMarker(isPrintParameter = true)
    public R<FetchQueueDO> getOne(Integer queueId) {
        log.error(throwable.getCause().getMessage());
        return R.feignCallFailed(throwable.getMessage());
    }

    @Override
    @LogMarker(isPrintParameter = true)
    public R<FetchQueueDO> getOneByWordName(String wordName) {
        log.error(throwable.getCause().getMessage());
        return R.feignCallFailed(throwable.getMessage());
    }

    @Override
    @LogMarker(isPrintParameter = true)
    public R<FetchQueueDO> getAnyOne(String wordName) {
        log.error(throwable.getCause().getMessage());
        return R.feignCallFailed(throwable.getMessage());
    }

    @Override
    @LogMarker(isPrintParameter = true)
    public R<List<FetchQueueDO>> pageQueue(Integer status, Integer current, Integer size, Integer infoType) {
        log.error(throwable.getCause().getMessage());
        return R.feignCallFailed(throwable.getMessage());
    }

    @Override
    @LogMarker(isPrintParameter = true)
    public R<List<FetchQueueDO>> listNotIntoCache() {
        log.error(throwable.getCause().getMessage());
        return R.feignCallFailed(throwable.getMessage());
    }

    @Override
    @LogMarker(isPrintParameter = true)
    public R<List<FetchQueueDO>> pageQueueLockIn(Integer status, Integer current, Integer size, Integer infoType) {
        log.error(throwable.getCause().getMessage());
        return R.feignCallFailed(throwable.getMessage());
    }

    @Override
    @LogMarker(isPrintParameter = true)
    public R<Boolean> updateQueueById(FetchQueueDO queueDO) {
        log.error(throwable.getCause().getMessage());
        return R.feignCallFailed(throwable.getMessage());
    }

    @Override
    @LogMarker(isPrintParameter = true)
    public R<Void> storeResult(FetchWordResultDTO dto) {
        log.error(throwable.getCause().getMessage());
        return R.feignCallFailed(throwable.getMessage());
    }

    @Override
    @LogMarker(isPrintParameter = true)
    public R<Boolean> fetchPronunciation(Integer wordId) {
        log.error(throwable.getCause().getMessage());
        return R.feignCallFailed(throwable.getMessage());
    }

    @Override
    @LogMarker(isPrintParameter = true)
    public R<List<RemovePronunciatioinMqDTO>> removeWord(String wordName, Integer queueId) {
        log.error(throwable.getCause().getMessage());
        return R.feignCallFailed(throwable.getMessage());
    }

    @Override
    @LogMarker(isPrintParameter = true)
    public R<List<RemovePronunciatioinMqDTO>> removeWord(Integer queueId) {
        log.error(throwable.getCause().getMessage());
        return R.feignCallFailed(throwable.getMessage());
    }

    @Override
    @LogMarker(isPrintParameter = true)
    public R<Boolean> removePhrase(Integer queueId) {
        log.error(throwable.getCause().getMessage());
        return R.feignCallFailed(throwable.getMessage());
    }

    @Override
    @LogMarker(isPrintParameter = true)
    public R<Boolean> updateByWordName(FetchQueueDO queueDO) {
        log.error(throwable.getCause().getMessage());
        return R.feignCallFailed(throwable.getMessage());
    }

    @Override
    @LogMarker(isPrintParameter = true)
    public R<Boolean> lock(String wordName) {
        log.error(throwable.getCause().getMessage());
        return R.feignCallFailed(throwable.getMessage());
    }

    @Override
    @LogMarker(isPrintParameter = true)
    public R<List<String>> listOverlapAnyway() {
        log.error(throwable.getCause().getMessage());
        return R.feignCallFailed();
    }

    @Override
    @LogMarker(isPrintParameter = true)
    public void handlePhrasesFetchResult(FetchPhraseRunUpResultDTO dto) {
        log.error(throwable.getCause().getMessage());
        R.feignCallFailed(throwable.getMessage());
    }

    @Override
    @LogMarker(isPrintParameter = true)
    public void storePhrasesFetchResult(FetchPhraseResultDTO dto) {
        log.error(throwable.getCause().getMessage());
        R.feignCallFailed(throwable.getMessage());
    }

    @Override
    @LogMarker(isPrintParameter = true)
    public R<WordQueryVO> queryWord(String wordName) {
        log.error("{} : {}", wordName, throwable.getCause().getMessage());
        return R.feignCallFailed(throwable.getMessage());
    }

    @Override
    @LogMarker(isPrintParameter = true)
    public void createTheDays() {
        log.error(throwable.getCause().getMessage());
        R.feignCallFailed(throwable.getMessage());
    }

    @Override
    public void generateTtsVoice() {
        log.error(throwable.getCause().getMessage());
        R.feignCallFailed(throwable.getMessage());
    }
}
