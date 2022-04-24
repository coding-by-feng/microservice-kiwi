/*
 *
 * Copyright [2019~2025] [codingByFeng]
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
package me.fengorz.kiwi.word.biz.service.base;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;

import me.fengorz.kiwi.word.api.entity.FetchQueueDO;

/**
 * 单词待抓取列表
 *
 * @author zhanshifeng
 * @date 2019-10-30 14:45:45
 */
public interface IWordFetchQueueService extends IService<FetchQueueDO> {

    @Deprecated
    boolean invalid(String wordName);

    @Deprecated
    boolean lock(String wordName);

    void startFetchOnAsync(String wordName);

    void startFetchPhraseOnAsync(String phrase, String derivation, Integer wordId);

    void startFetch(String wordName);

    void startForceFetchWord(String wordName);

    void startFetchPhrase(String phrase, String word, Integer wordId);

    void flagFetchBaseFinish(Integer queueId, Integer wordId);

    void flagWordQueryException(String wordName);

    List<FetchQueueDO> page2List(Integer status, Integer current, Integer size, Integer isLock, Integer infoType);

    List<FetchQueueDO> listNotIntoCache();

    @Deprecated
    void saveDerivation(String inputWordName, String fetchWordName);

    /**
     * 默认查单词
     *
     * @param wordName
     * @param infoType
     * @return
     */
    FetchQueueDO getOneInUnLock(String wordName, Integer... infoType);

    FetchQueueDO getOneInUnLock(Integer queueId);

    FetchQueueDO getOneAnyhow(String wordName, Integer... infoType);

    /**
     * 不管infoType是什么，return一个回来
     * @param wordName
     * @return
     */
    FetchQueueDO getAnyOne(String wordName);

    FetchQueueDO getOneAnyhow(Integer queueId);
}
