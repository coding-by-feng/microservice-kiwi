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
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import me.fengorz.kiwi.common.sdk.constant.GlobalConstants;
import me.fengorz.kiwi.common.sdk.util.bean.KiwiBeanUtils;
import me.fengorz.kiwi.common.sdk.util.validate.KiwiAssertUtils;
import me.fengorz.kiwi.common.sdk.web.security.SecurityUtils;
import me.fengorz.kiwi.word.api.dto.mapper.in.CountEntityIsCollectDTO;
import me.fengorz.kiwi.word.api.dto.mapper.out.SelectWordStarListResultDTO;
import me.fengorz.kiwi.word.api.entity.WordStarListDO;
import me.fengorz.kiwi.word.api.entity.WordStarRelDO;
import me.fengorz.kiwi.word.api.entity.column.WordStarListColumn;
import me.fengorz.kiwi.word.api.vo.WordStarListVO;
import me.fengorz.kiwi.word.api.vo.star.WordStarItemParaphraseVO;
import me.fengorz.kiwi.word.api.vo.star.WordStarItemVO;
import me.fengorz.kiwi.word.biz.mapper.WordStarListMapper;
import me.fengorz.kiwi.word.biz.service.base.WordStarListService;
import me.fengorz.kiwi.word.biz.service.base.WordStarRelService;
import me.fengorz.kiwi.word.biz.service.operate.AsyncArchiveService;

/**
 * 单词本
 *
 * @author zhanshifeng
 * @date 2019-12-08 23:26:57
 */
@Service()
@RequiredArgsConstructor
public class WordStarListServiceImpl extends ServiceImpl<WordStarListMapper, WordStarListDO>
    implements WordStarListService {

    private final WordStarListMapper wordStarListMapper;
    private final WordStarRelService relService;
    private final AsyncArchiveService archiveService;

    @Override
    public Integer countById(Integer id) {
        return this.count(new QueryWrapper<>(new WordStarListDO().setId(id).setIsDel(GlobalConstants.FLAG_N)));
    }

    @Override
    public List<WordStarListVO> getCurrentUserList(Integer userId) {
        QueryWrapper<WordStarListDO> queryWrapper =
            new QueryWrapper<>(new WordStarListDO().setOwner(userId).setIsDel(GlobalConstants.FLAG_N))
                .select(WordStarListDO.class, tableFieldInfo -> WordStarListColumn.ID.equals(tableFieldInfo.getColumn())
                    || WordStarListColumn.LIST_NAME.equals(tableFieldInfo.getColumn()));

        return KiwiBeanUtils.convertFrom(wordStarListMapper.selectList(queryWrapper), WordStarListVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateListByUser(WordStarListDO entity, Integer id, Integer userId) {
        UpdateWrapper<WordStarListDO> updateWrapper =
            new UpdateWrapper<>(new WordStarListDO().setOwner(userId).setId(id));
        return this.update(entity, updateWrapper);
    }

    @Override
    public IPage<WordStarItemVO> getListItems(Page<WordStarListDO> page, Integer listId) {
        IPage<SelectWordStarListResultDTO> listItems = wordStarListMapper.selectListItems(page, listId);
        IPage<WordStarItemVO> iPage = new Page<>();
        if (listItems.getSize() > 0) {
            List<WordStarItemVO> voList = new ArrayList<>();
            for (SelectWordStarListResultDTO record : listItems.getRecords()) {
                WordStarItemVO wordStarItemVO = new WordStarItemVO();
                wordStarItemVO.setWordName(record.getWordName());
                wordStarItemVO.setWordId(record.getWordId());
                String paraphrases = record.getParaphrases();
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
            iPage.setPages(listItems.getPages());
            iPage.setSize(listItems.getSize());
            iPage.setTotal(listItems.getTotal());
            iPage.setCurrent(listItems.getCurrent());
        }
        return iPage;
    }

    @Override
    public boolean countWordIsCollect(Integer wordId, Integer owner) {
        Integer count =
            wordStarListMapper.countWordIsCollect(new CountEntityIsCollectDTO().setEntityId(wordId).setOwner(owner));
        return count > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void putIntoStarList(Integer wordId, Integer listId) {
        LambdaQueryWrapper<WordStarRelDO> queryWrapper = Wrappers.<WordStarRelDO>lambdaQuery()
            .eq(WordStarRelDO::getListId, listId).eq(WordStarRelDO::getWordId, wordId);
        if (relService.count(queryWrapper) > 0) {
            return;
        }
        relService.save(new WordStarRelDO().setListId(listId).setWordId(wordId));
        archiveService.archiveWordRel(wordId, listId, SecurityUtils.getCurrentUserId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeStarList(Integer wordId, Integer listId) {
        LambdaQueryWrapper<WordStarRelDO> queryWrapper = new LambdaQueryWrapper<WordStarRelDO>()
            .eq(WordStarRelDO::getListId, listId).eq(WordStarRelDO::getWordId, wordId);
        KiwiAssertUtils.assertNotEmpty(relService.count(queryWrapper), "wordStar is not exists!");
        relService.remove(queryWrapper);

        archiveService.invalidArchiveWordRel(wordId, listId, SecurityUtils.getCurrentUserId());
    }
}
