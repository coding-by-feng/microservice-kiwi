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

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import me.fengorz.kiwi.common.api.constant.CommonConstants;
import me.fengorz.kiwi.common.sdk.util.bean.KiwiBeanUtils;
import me.fengorz.kiwi.common.sdk.util.validate.KiwiAssertUtils;
import me.fengorz.kiwi.common.sdk.web.security.SecurityUtils;
import me.fengorz.kiwi.word.api.entity.ParaphraseStarListDO;
import me.fengorz.kiwi.word.api.entity.ParaphraseStarRelDO;
import me.fengorz.kiwi.word.api.entity.column.WordParaphraseStarListColumn;
import me.fengorz.kiwi.word.api.vo.ParaphraseStarListVO;
import me.fengorz.kiwi.word.api.vo.star.ParaphraseStarItemVO;
import me.fengorz.kiwi.word.biz.mapper.ParaphraseStarListMapper;
import me.fengorz.kiwi.word.biz.service.base.IParaphraseStarListService;
import me.fengorz.kiwi.word.biz.service.base.IParaphraseStarRelService;
import me.fengorz.kiwi.word.biz.service.operate.IAsyncArchiveService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 单词本
 *
 * @author zhanshifeng
 * @date 2019-12-08 23:27:41
 */
@Service
@RequiredArgsConstructor
public class ParaphraseStarListServiceImpl extends
        ServiceImpl<ParaphraseStarListMapper, ParaphraseStarListDO> implements IParaphraseStarListService {

    private final ParaphraseStarListMapper mapper;
    private final IParaphraseStarRelService relService;
    private final IAsyncArchiveService archiveService;

    @Override
    public Integer countById(Integer id) {
        return this.count(new QueryWrapper<>(new ParaphraseStarListDO().setId(id)));
    }

    @Override
    public List<ParaphraseStarListVO> getCurrentUserList(Integer userId) {
        QueryWrapper<ParaphraseStarListDO> queryWrapper =
                new QueryWrapper<>(new ParaphraseStarListDO().setOwner(userId).setIsDel(CommonConstants.FLAG_N)).select(
                        ParaphraseStarListDO.class,
                        tableFieldInfo -> WordParaphraseStarListColumn.ID.equals(tableFieldInfo.getColumn())
                                || WordParaphraseStarListColumn.LIST_NAME.equals(tableFieldInfo.getColumn())
                                || WordParaphraseStarListColumn.REMARK.equals(tableFieldInfo.getColumn()));

        return KiwiBeanUtils.convertFrom(mapper.selectList(queryWrapper),
                ParaphraseStarListVO.class);
    }

    @Override
    public boolean updateListByUser(ParaphraseStarListDO entity, Integer id, Integer userId) {
        UpdateWrapper<ParaphraseStarListDO> updateWrapper =
                new UpdateWrapper<>(new ParaphraseStarListDO().setOwner(userId).setId(id));
        return this.update(entity, updateWrapper);
    }

    @Override
    public IPage<ParaphraseStarItemVO> selectListItems(Page page, Integer listId) {
        return this.mapper.selectListItems(page, listId);
    }

    @Override
    public IPage<ParaphraseStarItemVO> selectReviewListItems(Page page, Integer listId) {
        return this.mapper.selectReviewListItems(page, listId);
    }

    @Override
    public IPage<ParaphraseStarItemVO> selectRememberListItems(Page page, Integer listId) {
        return this.mapper.selectRememberListItems(page, listId);
    }

    @Override
    public void removeParaphraseStar(Integer paraphraseId, Integer listId) {
        LambdaQueryWrapper<ParaphraseStarRelDO> wrapper = new LambdaQueryWrapper<ParaphraseStarRelDO>()
                .eq(ParaphraseStarRelDO::getListId, listId).eq(ParaphraseStarRelDO::getParaphraseId, paraphraseId);
        int count = relService.count(wrapper);
        if (count < 0) {
            return;
        }
        relService.remove(wrapper);

        archiveService.invalidArchiveParaphraseRel(paraphraseId, listId, SecurityUtils.getCurrentUserId());
    }

    @Override
    public void rememberOne(Integer paraphraseId, Integer listId) {
        relService.update(
                new ParaphraseStarRelDO().setIsRemember(CommonConstants.FLAG_DEL_YES)
                        .setRememberTime(LocalDateTime.now()),
                Wrappers.<ParaphraseStarRelDO>lambdaQuery().eq(ParaphraseStarRelDO::getListId, listId)
                        .eq(ParaphraseStarRelDO::getParaphraseId, paraphraseId));
    }

    @Override
    public void keepInMind(Integer paraphraseId, Integer listId) {
        relService.update(
                new ParaphraseStarRelDO().setIsKeepInMind(CommonConstants.FLAG_DEL_YES)
                        .setKeepInMindTime(LocalDateTime.now()),
                Wrappers.<ParaphraseStarRelDO>lambdaQuery().eq(ParaphraseStarRelDO::getListId, listId)
                        .eq(ParaphraseStarRelDO::getParaphraseId, paraphraseId));
    }

    @Override
    public void forgetOne(Integer paraphraseId, Integer listId) {
        relService.update(
                new ParaphraseStarRelDO().setIsRemember(CommonConstants.FLAG_DEL_NO)
                        .setIsKeepInMind(CommonConstants.FLAG_DEL_NO),
                Wrappers.<ParaphraseStarRelDO>lambdaQuery().eq(ParaphraseStarRelDO::getListId, listId)
                        .eq(ParaphraseStarRelDO::getParaphraseId, paraphraseId));
    }

    @Override
    public void putIntoStarList(Integer paraphraseId, Integer listId) {
        LambdaQueryWrapper<ParaphraseStarRelDO> wrapper = new LambdaQueryWrapper<ParaphraseStarRelDO>()
                .eq(ParaphraseStarRelDO::getListId, listId).eq(ParaphraseStarRelDO::getParaphraseId, paraphraseId);
        KiwiAssertUtils.serviceEmpty(relService.count(wrapper), "paraphrase already exists!");
        relService.save(new ParaphraseStarRelDO().setListId(listId).setParaphraseId(paraphraseId));

        archiveService.archiveParaphraseRel(paraphraseId, listId, SecurityUtils.getCurrentUserId());
    }

}
