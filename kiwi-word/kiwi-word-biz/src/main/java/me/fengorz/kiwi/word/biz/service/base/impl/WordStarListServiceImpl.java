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

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import me.fengorz.kiwi.common.api.constant.CommonConstants;
import me.fengorz.kiwi.common.sdk.util.bean.KiwiBeanUtils;
import me.fengorz.kiwi.word.api.dto.mapper.in.CountEntityIsCollectDTO;
import me.fengorz.kiwi.word.api.dto.mapper.out.SelectWordStarListResultDTO;
import me.fengorz.kiwi.word.api.entity.WordStarListDO;
import me.fengorz.kiwi.word.api.entity.column.WordStarListColumn;
import me.fengorz.kiwi.word.api.vo.WordStarListVO;
import me.fengorz.kiwi.word.api.vo.star.WordStarItemParaphraseVO;
import me.fengorz.kiwi.word.api.vo.star.WordStarItemVO;
import me.fengorz.kiwi.word.biz.mapper.WordStarListMapper;
import me.fengorz.kiwi.word.biz.service.base.IWordStarListService;

/**
 * 单词本
 *
 * @author zhanshifeng
 * @date 2019-12-08 23:26:57
 */
@Service()
@RequiredArgsConstructor
public class WordStarListServiceImpl extends ServiceImpl<WordStarListMapper, WordStarListDO>
    implements IWordStarListService {

    private final WordStarListMapper wordStarListMapper;

    @Override
    public Integer countById(Integer id) {
        return this.count(new QueryWrapper<>(new WordStarListDO().setId(id).setIsDel(CommonConstants.FLAG_N)));
    }

    @Override
    public List<WordStarListVO> getCurrentUserList(Integer userId) {
        QueryWrapper<WordStarListDO> queryWrapper =
            new QueryWrapper<>(new WordStarListDO().setOwner(userId).setIsDel(CommonConstants.FLAG_N))
                .select(WordStarListDO.class, tableFieldInfo -> WordStarListColumn.ID.equals(tableFieldInfo.getColumn())
                    || WordStarListColumn.LIST_NAME.equals(tableFieldInfo.getColumn()));

        return KiwiBeanUtils.convertFrom(wordStarListMapper.selectList(queryWrapper), WordStarListVO.class);
    }

    @Override
    public boolean updateListByUser(WordStarListDO entity, Integer id, Integer userId) {
        UpdateWrapper<WordStarListDO> updateWrapper =
            new UpdateWrapper<>(new WordStarListDO().setOwner(userId).setId(id));
        return this.update(entity, updateWrapper);
    }

    @Override
    public IPage<WordStarItemVO> getListItems(Page page, Integer listId) {
        IPage iPage = wordStarListMapper.selectListItems(page, listId);
        if (iPage.getSize() > 0) {
            List<WordStarItemVO> voList = new ArrayList<>();
            for (Object record : iPage.getRecords()) {
                SelectWordStarListResultDTO resultDTO = (SelectWordStarListResultDTO)record;
                WordStarItemVO wordStarItemVO = new WordStarItemVO();
                wordStarItemVO.setWordName(resultDTO.getWordName());
                wordStarItemVO.setWordId(resultDTO.getWordId());
                String paraphrases = resultDTO.getParaphrases();
                if (StrUtil.isNotBlank(paraphrases)) {
                    List<WordStarItemParaphraseVO> paraphraseVOList = new ArrayList<>();
                    String[] paraphraseArr = paraphrases.split("##");
                    for (String paraphrase : paraphraseArr) {
                        String[] paraphraseItem = paraphrase.split("\\|\\|");
                        WordStarItemParaphraseVO paraphraseVO = new WordStarItemParaphraseVO();
                        if (StrUtil.isNotBlank(paraphraseItem[0])) {
                            paraphraseVO.setParaphraseEnglish(paraphraseItem[0]);
                            if (paraphraseItem.length > 1) {
                                paraphraseVO.setMeaningChinese(paraphraseItem[1]);
                            }
                        }
                        paraphraseVOList.add(paraphraseVO);
                    }
                    wordStarItemVO.setParahpraseList(paraphraseVOList);
                }
                voList.add(wordStarItemVO);
            }
            iPage.setRecords(voList);
        }
        return iPage;
    }

    @Override
    public boolean countWordIsCollect(Integer wordId, Integer owner) {
        Integer count =
            wordStarListMapper.countWordIsCollect(new CountEntityIsCollectDTO().setEntityId(wordId).setOwner(owner));
        return count > 0;
    }

}
