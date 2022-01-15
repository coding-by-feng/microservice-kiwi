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

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.api.R;
import me.fengorz.kiwi.word.api.dto.queue.RemovePronunciatioinMqDTO;
import me.fengorz.kiwi.word.api.dto.queue.result.FetchPhraseResultDTO;
import me.fengorz.kiwi.word.api.dto.queue.result.FetchPhraseRunUpResultDTO;
import me.fengorz.kiwi.word.api.dto.queue.result.FetchWordResultDTO;
import me.fengorz.kiwi.word.api.entity.FetchQueueDO;
import me.fengorz.kiwi.word.api.feign.IBizAPI;
import me.fengorz.kiwi.word.api.vo.detail.WordQueryVO;
import org.springframework.stereotype.Component;

import java.util.List;

/** @Author zhanshifeng @Date 2019/10/30 3:20 PM */
@Slf4j
@Component
public class BizAPIFallback implements IBizAPI {

  @Setter private Throwable throwable;

  @Override
  public R<FetchQueueDO> getOne(Integer queueId) {
    log.error(throwable.getCause().getMessage());
    return R.feignCallFailed(throwable.getMessage());
  }

  @Override
  public R<FetchQueueDO> getOneByWordName(String wordName) {
    log.error(throwable.getCause().getMessage());
    return R.feignCallFailed(throwable.getMessage());
  }

  @Override
  public R<List<FetchQueueDO>> pageQueue(
      Integer status, Integer current, Integer size, Integer infoType) {
    log.error(throwable.getCause().getMessage());
    return R.feignCallFailed(throwable.getMessage());
  }

  @Override
  public R<List<FetchQueueDO>> listNotIntoCache() {
    log.error(throwable.getCause().getMessage());
    return R.feignCallFailed(throwable.getMessage());
  }

  @Override
  public R<List<FetchQueueDO>> pageQueueLockIn(
      Integer status, Integer current, Integer size, Integer infoType) {
    log.error(throwable.getCause().getMessage());
    return R.feignCallFailed(throwable.getMessage());
  }

  @Override
  public R<Boolean> updateQueueById(FetchQueueDO queueDO) {
    log.error(throwable.getCause().getMessage());
    return R.feignCallFailed(throwable.getMessage());
  }

  @Override
  public R<Void> storeResult(FetchWordResultDTO dto) {
    log.error(throwable.getCause().getMessage());
    return R.feignCallFailed(throwable.getMessage());
  }

  @Override
  public R<Boolean> fetchPronunciation(Integer wordId) {
    log.error(throwable.getCause().getMessage());
    return R.feignCallFailed(throwable.getMessage());
  }

  @Override
  public R<List<RemovePronunciatioinMqDTO>> removeWord(String wordName, Integer queueId) {
    log.error(throwable.getCause().getMessage());
    return R.feignCallFailed(throwable.getMessage());
  }

  @Override
  public R<List<RemovePronunciatioinMqDTO>> removeWord(Integer queueId) {
    log.error(throwable.getCause().getMessage());
    return R.feignCallFailed(throwable.getMessage());
  }

  @Override
  public R<Boolean> removePhrase(Integer queueId) {
    log.error(throwable.getCause().getMessage());
    return R.feignCallFailed(throwable.getMessage());
  }

  @Override
  public R<Boolean> updateByWordName(FetchQueueDO queueDO) {
    log.error(throwable.getCause().getMessage());
    return R.feignCallFailed(throwable.getMessage());
  }

  @Override
  public R<Boolean> lock(String wordName) {
    log.error(throwable.getCause().getMessage());
    return R.feignCallFailed(throwable.getMessage());
  }

  @Override
  public R<List<String>> listOverlapInUnLock() {
    return R.feignCallFailed();
  }

  @Override
  public R<Boolean> handlePhrasesFetchResult(FetchPhraseRunUpResultDTO dto) {
    log.error(throwable.getCause().getMessage());
    return R.feignCallFailed(throwable.getMessage());
  }

  @Override
  public R<Boolean> storePhrasesFetchResult(FetchPhraseResultDTO dto) {
    log.error(throwable.getCause().getMessage());
    return R.feignCallFailed(throwable.getMessage());
  }

  @Override
  public R<WordQueryVO> queryWord(String keyword) {
    log.error(throwable.getCause().getMessage());
    return R.feignCallFailed(throwable.getMessage());
  }

  @Override
  public R<Void> createTheDays() {
    log.error(throwable.getCause().getMessage());
    return R.feignCallFailed(throwable.getMessage());
  }
}
