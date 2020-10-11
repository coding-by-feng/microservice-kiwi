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

package me.fengorz.kiwi.word.biz.service.operate;

import me.fengorz.kiwi.common.api.annotation.cache.KiwiCacheKey;
import me.fengorz.kiwi.word.api.dto.queue.result.FetchWordReplaceDTO;
import me.fengorz.kiwi.word.api.entity.WordMainDO;
import me.fengorz.kiwi.word.api.vo.detail.ParaphraseVO;
import me.fengorz.kiwi.word.api.vo.detail.WordQueryVO;

/**
 * @Author zhanshifeng
 */
public interface IOperateService {

    /* paraphrase methods begin */

    WordQueryVO queryWord(@KiwiCacheKey String wordName, Integer... intoType);

    ParaphraseVO findWordParaphraseVO(Integer paraphraseId);

    /* paraphrase methods end */

    /* wordVariant methods begin */

    /**
     * @param inputWordName
     *            界面输入要查询的单词
     * @param fetchWordName
     *            实际爬虫抓取到的单词原形
     * @return
     */
    boolean insertVariant(String inputWordName, String fetchWordName);

    /* wordVariant methods end */

    /* cache mothods begin */

    void evict(@KiwiCacheKey String wordName, WordMainDO one);

    FetchWordReplaceDTO getCacheReplace(String wordName);

    FetchWordReplaceDTO cacheReplace(String wordName, FetchWordReplaceDTO dto);

    void fetchReplaceCallBack(String wordName);

    /* cache mothods end */

}
