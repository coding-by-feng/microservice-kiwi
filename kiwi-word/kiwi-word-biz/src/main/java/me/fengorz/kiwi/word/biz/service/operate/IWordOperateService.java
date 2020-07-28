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

import me.fengorz.kiwi.common.api.exception.ServiceException;
import me.fengorz.kiwi.common.api.exception.dfs.DfsOperateDeleteException;
import me.fengorz.kiwi.word.api.dto.queue.fetch.FetchWordReplaceDTO;
import me.fengorz.kiwi.word.api.vo.detail.WordParaphraseVO;
import me.fengorz.kiwi.word.api.vo.detail.WordQueryVO;

/**
 * @Author zhanshifeng
 */
public interface IWordOperateService {

    boolean removeWord(String wordName) throws DfsOperateDeleteException;

    void dfsDeleteExceptionBackCall(String wordName);

    WordQueryVO queryWord(String wordName) throws ServiceException, DfsOperateDeleteException;

    boolean putWordIntoStarList(Integer wordId, Integer listId) throws ServiceException;

    boolean removeWordStarList(Integer wordId, Integer listId) throws ServiceException;

    /* paraphrase methods begin */

    boolean putParaphraseIntoStarList(Integer paraphraseId, Integer listId) throws ServiceException;

    WordParaphraseVO findWordParaphraseVO(Integer paraphraseId);

    /* paraphrase methods end */

    boolean putExampleIntoStarList(Integer exampleId, Integer listId) throws ServiceException;

    boolean removeExampleStar(Integer exampleId, Integer listId) throws ServiceException;

    /* wordVariant methods begin */

    /**
     * @param inputWordName
     *            界面输入要查询的单词
     * @param fetchWordName
     *            实际爬虫抓取到的单词原形
     * @return
     */
    boolean insertVariant(String inputWordName, String fetchWordName) throws DfsOperateDeleteException;

    /* wordVariant methods end */

    /* cache mothods begin */

    void evict(String wordName) throws DfsOperateDeleteException;

    FetchWordReplaceDTO cacheGetFetchReplace(String wordName);

    FetchWordReplaceDTO cachePutFetchReplace(String wordName, FetchWordReplaceDTO dto);

    /* cache mothods end */

}
