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
package me.fengorz.kiwi.word.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import me.fengorz.kiwi.word.api.entity.ParaphraseStarListDO;
import me.fengorz.kiwi.word.api.vo.star.ParaphraseStarItemVO;

/**
 * 单词本
 *
 * @author zhanshifeng
 * @date 2019-12-08 23:27:41
 */
public interface ParaphraseStarListMapper extends BaseMapper<ParaphraseStarListDO> {

    IPage<ParaphraseStarItemVO> selectItems(Page<?> page, Integer listId);

    IPage<ParaphraseStarItemVO> selectRecentItems(Page<?> page, Integer userId);

    IPage<ParaphraseStarItemVO> selectReviewItems(Page<?> page, Integer listId);

    IPage<ParaphraseStarItemVO> selectRecentReviewItems(Page<?> page, Integer userId);

    IPage<ParaphraseStarItemVO> selectRememberItems(Page<?> page, Integer listId);

    IPage<ParaphraseStarItemVO> selectRecentRememberItems(Page<?> page, Integer userId);
}
