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

import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.api.R;
import me.fengorz.kiwi.common.api.feign.AbstractFallback;
import me.fengorz.kiwi.common.sdk.annotation.log.LogMarker;
import me.fengorz.kiwi.word.api.dto.queue.RemovePronunciatioinMqDTO;
import me.fengorz.kiwi.word.api.dto.queue.result.FetchPhraseResultDTO;
import me.fengorz.kiwi.word.api.dto.queue.result.FetchPhraseRunUpResultDTO;
import me.fengorz.kiwi.word.api.dto.queue.result.FetchWordResultDTO;
import me.fengorz.kiwi.word.api.entity.FetchQueueDO;
import me.fengorz.kiwi.word.api.feign.DictFetchApi;
import me.fengorz.kiwi.word.api.vo.detail.WordQueryVO;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @Author zhanshifeng @Date 2019/10/30 3:20 PM
 */
@Slf4j
@Component
public class DictFetchApiFallback extends AbstractFallback implements DictFetchApi {

    @Override
    @LogMarker(isPrintParameter = true)
    public R<FetchQueueDO> getOne(Integer queueId) {
        return handleError();
    }

    @Override
    @LogMarker(isPrintParameter = true)
    public R<FetchQueueDO> getOneByWordName(String wordName) {
        return handleError();
    }

    @Override
    @LogMarker(isPrintParameter = true)
    public R<FetchQueueDO> getAnyOne(String wordName) {
        return handleError();
    }

    @Override
    @LogMarker(isPrintParameter = true)
    public R<List<FetchQueueDO>> pageQueue(Integer status, Integer current, Integer size, Integer infoType) {
        return handleError();
    }

    @Override
    @LogMarker(isPrintParameter = true)
    public R<List<FetchQueueDO>> listNotIntoCache() {
        return handleError();
    }

    @Override
    @LogMarker(isPrintParameter = true)
    public R<List<FetchQueueDO>> pageQueueInLock(Integer status, Integer current, Integer size, Integer infoType) {
        return handleError();
    }

    @Override
    @LogMarker(isPrintParameter = true)
    public R<Boolean> updateQueueById(FetchQueueDO queueDO) {
        return handleError();
    }

    @Override
    @LogMarker(isPrintParameter = true)
    public R<Void> storeResult(FetchWordResultDTO dto) {
        return handleError();
    }

    @Override
    @LogMarker(isPrintParameter = true)
    public R<Boolean> fetchPronunciation(Integer wordId) {
        return handleError();
    }

    @Override
    @LogMarker(isPrintParameter = true)
    public R<List<RemovePronunciatioinMqDTO>> removeWord(String wordName, Integer queueId) {
        return handleError();
    }

    @Override
    @LogMarker(isPrintParameter = true)
    public R<List<RemovePronunciatioinMqDTO>> removeWord(Integer queueId) {
        return handleError();
    }

    @Override
    @LogMarker(isPrintParameter = true)
    public R<Boolean> removePhrase(Integer queueId) {
        return handleError();
    }

    @Override
    @LogMarker(isPrintParameter = true)
    public R<Boolean> updateByWordName(FetchQueueDO queueDO) {
        return handleError();
    }

    @Override
    @LogMarker(isPrintParameter = true)
    public R<Boolean> lock(String wordName) {
        return handleError();
    }

    @Override
    @LogMarker(isPrintParameter = true)
    public R<List<String>> listOverlapAnyway() {
        return handleError();
    }

    @Override
    @LogMarker(isPrintParameter = true)
    public void handlePhrasesFetchResult(FetchPhraseRunUpResultDTO dto) {
        handleErrorNotReturn();
    }

    @Override
    @LogMarker(isPrintParameter = true)
    public void storePhrasesFetchResult(FetchPhraseResultDTO dto) {
        handleErrorNotReturn();
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
        handleErrorNotReturn();
    }

    @Override
    public void refreshAllApiKey() {
        handleErrorNotReturn();
    }

    @Override
    public void generateTtsVoice(Integer type) {
        handleErrorNotReturn();
    }

    @Override
    public void reGenIncorrectAudioByVoicerss() {
        handleErrorNotReturn();
    }

}
