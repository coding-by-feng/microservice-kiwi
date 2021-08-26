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
package me.fengorz.kiwi.word.biz.service.base;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import me.fengorz.kiwi.word.api.entity.ParaphraseStarListDO;
import me.fengorz.kiwi.word.api.vo.ParaphraseStarListVO;
import me.fengorz.kiwi.word.api.vo.star.ParaphraseStarItemVO;

import java.util.List;

/**
 * 单词本
 *
 * @author zhanshifeng
 * @date 2019-12-08 23:27:41
 */
public interface IParaphraseStarListService extends IService<ParaphraseStarListDO> {

    Integer countById(Integer id);

    List<ParaphraseStarListVO> getCurrentUserList(Integer userId);

    boolean updateListByUser(ParaphraseStarListDO entity, Integer id, Integer userId);

    IPage<ParaphraseStarItemVO> selectListItems(Page page, Integer listId);

    IPage<ParaphraseStarItemVO> selectReviewListItems(Page page, Integer listId);

    IPage<ParaphraseStarItemVO> selectRememberListItems(Page page, Integer listId);

    void putIntoStarList(Integer paraphraseId, Integer listId);

    void removeParaphraseStar(Integer paraphraseId, Integer listId);

    void rememberOne(Integer paraphraseId, Integer listId);

    void keepInMind(Integer paraphraseId, Integer listId);

    void forgetOne(Integer paraphraseId, Integer listId);

    List<Integer> findAllUserId();
}
