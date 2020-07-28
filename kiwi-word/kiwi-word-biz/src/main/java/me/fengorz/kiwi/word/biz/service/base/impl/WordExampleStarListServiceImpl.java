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
package me.fengorz.kiwi.word.biz.service.base.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import lombok.RequiredArgsConstructor;
import me.fengorz.kiwi.common.api.constant.CommonConstants;
import me.fengorz.kiwi.common.sdk.util.bean.KiwiBeanUtils;
import me.fengorz.kiwi.word.api.entity.WordExampleStarListDO;
import me.fengorz.kiwi.word.api.entity.column.WordParaphraseExampleListColumn;
import me.fengorz.kiwi.word.api.vo.WordExampleStarListVO;
import me.fengorz.kiwi.word.api.vo.star.ExampleStarItemVO;
import me.fengorz.kiwi.word.biz.mapper.WordExampleStarListMapper;
import me.fengorz.kiwi.word.biz.service.base.IWordExampleStarListService;

/**
 * @author zhanshifeng
 * @date 2019-12-08 23:27:12
 */
@Service()
@RequiredArgsConstructor
public class WordExampleStarListServiceImpl extends ServiceImpl<WordExampleStarListMapper, WordExampleStarListDO>
    implements IWordExampleStarListService {

    private final WordExampleStarListMapper wordExampleStarListMapper;

    @Override
    public Integer countById(Integer id) {
        return this.count(new QueryWrapper<>(new WordExampleStarListDO().setId(id)));
    }

    @Override
    public List<WordExampleStarListVO> getCurrentUserList(Integer userId) {
        QueryWrapper<WordExampleStarListDO> queryWrapper =
            new QueryWrapper<>(new WordExampleStarListDO().setOwner(userId).setIsDel(CommonConstants.FLAG_N)).select(
                WordExampleStarListDO.class,
                tableFieldInfo -> WordParaphraseExampleListColumn.ID.equals(tableFieldInfo.getColumn())
                    || WordParaphraseExampleListColumn.LIST_NAME.equals(tableFieldInfo.getColumn())
                    || WordParaphraseExampleListColumn.REMARK.equals(tableFieldInfo.getColumn()));

        return KiwiBeanUtils.convertFrom(wordExampleStarListMapper.selectList(queryWrapper),
            WordExampleStarListVO.class);
    }

    @Override
    public IPage<ExampleStarItemVO> getListItems(Page page, Integer listId) {
        return this.wordExampleStarListMapper.selectListItems(page, listId);
    }
}
