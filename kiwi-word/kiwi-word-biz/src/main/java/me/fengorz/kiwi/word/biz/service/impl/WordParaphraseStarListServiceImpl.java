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
package me.fengorz.kiwi.word.biz.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import lombok.RequiredArgsConstructor;
import me.fengorz.kiwi.common.api.R;
import me.fengorz.kiwi.common.api.constant.CommonConstants;
import me.fengorz.kiwi.common.sdk.util.bean.KiwiBeanUtils;
import me.fengorz.kiwi.word.api.entity.WordParaphraseStarListDO;
import me.fengorz.kiwi.word.api.entity.WordParaphraseStarRelDO;
import me.fengorz.kiwi.word.api.entity.column.WordParaphraseStarListColumn;
import me.fengorz.kiwi.word.api.vo.WordParaphraseStarListVO;
import me.fengorz.kiwi.word.api.vo.star.ParaphraseStarItemVO;
import me.fengorz.kiwi.word.biz.mapper.WordParaphraseStarListMapper;
import me.fengorz.kiwi.word.biz.mapper.WordParaphraseStarRelMapper;
import me.fengorz.kiwi.word.biz.service.IWordParaphraseStarListService;

/**
 * 单词本
 *
 * @author zhanshifeng
 * @date 2019-12-08 23:27:41
 */
@Service
@RequiredArgsConstructor
public class WordParaphraseStarListServiceImpl extends
    ServiceImpl<WordParaphraseStarListMapper, WordParaphraseStarListDO> implements IWordParaphraseStarListService {

    private final WordParaphraseStarListMapper wordParaphraseStarListMapper;
    private final WordParaphraseStarRelMapper wordParaphraseStarRelMapper;

    @Override
    public Integer countById(Integer id) {
        return this.count(new QueryWrapper<>(new WordParaphraseStarListDO().setId(id)));
    }

    @Override
    public List<WordParaphraseStarListVO> getCurrentUserList(Integer userId) {
        QueryWrapper<WordParaphraseStarListDO> queryWrapper =
            new QueryWrapper<>(new WordParaphraseStarListDO().setOwner(userId).setIsDel(CommonConstants.FLAG_N)).select(
                WordParaphraseStarListDO.class,
                tableFieldInfo -> WordParaphraseStarListColumn.ID.equals(tableFieldInfo.getColumn())
                    || WordParaphraseStarListColumn.LIST_NAME.equals(tableFieldInfo.getColumn())
                    || WordParaphraseStarListColumn.REMARK.equals(tableFieldInfo.getColumn()));

        return KiwiBeanUtils.convertFrom(wordParaphraseStarListMapper.selectList(queryWrapper),
            WordParaphraseStarListVO.class);
    }

    @Override
    public R updateListByUser(WordParaphraseStarListDO entity, Integer id, Integer userId) {
        UpdateWrapper<WordParaphraseStarListDO> updateWrapper =
            new UpdateWrapper<>(new WordParaphraseStarListDO().setOwner(userId).setId(id));
        return R.success(this.update(entity, updateWrapper));
    }

    @Override
    public IPage<ParaphraseStarItemVO> selectListItems(Page page, Integer listId) {
        return this.wordParaphraseStarListMapper.selectListItems(page, listId);
    }

    @Override
    public IPage<ParaphraseStarItemVO> selectReviewListItems(Page page, Integer listId) {
        return this.wordParaphraseStarListMapper.selectReviewListItems(page, listId);
    }

    @Override
    public int removeParaphraseStar(Integer paraphraseId, Integer listId) {
        LambdaQueryWrapper<WordParaphraseStarRelDO> wrapper = new LambdaQueryWrapper<WordParaphraseStarRelDO>()
            .eq(WordParaphraseStarRelDO::getListId, listId).eq(WordParaphraseStarRelDO::getParaphraseId, paraphraseId);
        Integer count = wordParaphraseStarRelMapper.selectCount(wrapper);
        if (count < 0) {
            return 0;
        }
        return wordParaphraseStarRelMapper.delete(wrapper);
    }

    @Override
    public void rememberOne(Integer paraphraseId, Integer listId) {
        wordParaphraseStarRelMapper.update(
            new WordParaphraseStarRelDO().setIsRemember(CommonConstants.FLAG_DEL_YES)
                .setRememberTime(LocalDateTime.now()),
            Wrappers.<WordParaphraseStarRelDO>lambdaQuery().eq(WordParaphraseStarRelDO::getListId, listId)
                .eq(WordParaphraseStarRelDO::getParaphraseId, paraphraseId));
    }

}
