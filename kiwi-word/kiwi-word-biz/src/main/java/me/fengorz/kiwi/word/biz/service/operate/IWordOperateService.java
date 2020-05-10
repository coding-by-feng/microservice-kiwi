/*
 *
 *   Copyright [2019~2025] [codingByFeng]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 *
 */

package me.fengorz.kiwi.word.biz.service.operate;

import me.fengorz.kiwi.common.api.exception.ServiceException;
import me.fengorz.kiwi.common.fastdfs.exception.DfsOperateDeleteException;
import me.fengorz.kiwi.common.fastdfs.exception.DfsOperateException;
import me.fengorz.kiwi.word.api.dto.fetch.FetchWordResultDTO;
import me.fengorz.kiwi.word.api.entity.WordMainDO;
import me.fengorz.kiwi.word.api.exception.WordResultStoreException;
import me.fengorz.kiwi.word.api.vo.WordCharacterVO;
import me.fengorz.kiwi.word.api.vo.WordQueryVO;

public interface IWordOperateService {

    void removeWordRelatedData(WordMainDO wordMainDO) throws DfsOperateException, DfsOperateDeleteException;

    boolean storeFetchWordResult(FetchWordResultDTO fetchWordResultDTO) throws WordResultStoreException, DfsOperateException, DfsOperateDeleteException;

    void dfsDeleteExceptionBackCall(String wordName);

    WordQueryVO queryWord(String wordName) throws ServiceException;

    boolean putWordIntoStarList(Integer wordId, Integer listId) throws ServiceException;

    boolean removeWordStarList(Integer wordId, Integer listId) throws ServiceException;

    boolean putParaphraseIntoStarList(Integer paraphraseId, Integer listId) throws ServiceException;

    boolean putExampleIntoStarList(Integer exampleId, Integer listId) throws ServiceException;

    WordCharacterVO getByParaphraseId(Integer paraphraseId) throws ServiceException;

    boolean removeExampleStar(Integer exampleId, Integer listId) throws ServiceException;
}
