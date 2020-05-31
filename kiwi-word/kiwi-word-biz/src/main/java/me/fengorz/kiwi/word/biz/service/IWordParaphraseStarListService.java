/*
 *
 *   Copyright [2019~2025] [zhanshifeng]
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
package me.fengorz.kiwi.word.biz.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import me.fengorz.kiwi.common.api.R;
import me.fengorz.kiwi.word.api.entity.WordParaphraseStarListDO;
import me.fengorz.kiwi.word.api.vo.WordParaphraseStarListVO;
import me.fengorz.kiwi.word.api.vo.star.ParaphraseStarItemVO;

import java.util.List;

/**
 * 单词本
 *
 * @author zhanshifeng
 * @date 2019-12-08 23:27:41
 */
public interface IWordParaphraseStarListService extends IService<WordParaphraseStarListDO> {

    Integer countById(Integer id);

    List<WordParaphraseStarListVO> getCurrentUserList(Integer userId);

    R updateListByUser(WordParaphraseStarListDO entity, Integer id, Integer userId);

    IPage<ParaphraseStarItemVO> getListItems(Page page, Integer listId);

    int removeParaphraseStar(Integer paraphraseId, Integer listId);
}
