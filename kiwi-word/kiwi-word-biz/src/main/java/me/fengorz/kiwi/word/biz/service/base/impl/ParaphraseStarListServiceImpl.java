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

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.sdk.constant.GlobalConstants;
import me.fengorz.kiwi.common.sdk.util.bean.KiwiBeanUtils;
import me.fengorz.kiwi.common.sdk.web.security.SecurityUtils;
import me.fengorz.kiwi.word.api.common.enumeration.ReviewBreakpointTypeEnum;
import me.fengorz.kiwi.word.api.common.enumeration.ReviewDailyCounterTypeEnum;
import me.fengorz.kiwi.word.api.entity.ParaphraseStarListDO;
import me.fengorz.kiwi.word.api.entity.ParaphraseStarRelDO;
import me.fengorz.kiwi.word.api.entity.column.WordParaphraseStarListColumn;
import me.fengorz.kiwi.word.api.vo.ParaphraseStarListVO;
import me.fengorz.kiwi.word.api.vo.star.ParaphraseStarItemVO;
import me.fengorz.kiwi.word.biz.mapper.ParaphraseStarListMapper;
import me.fengorz.kiwi.word.biz.service.base.ParaphraseStarListService;
import me.fengorz.kiwi.word.biz.service.base.ParaphraseStarRelService;
import me.fengorz.kiwi.word.biz.service.operate.AsyncArchiveService;
import me.fengorz.kiwi.word.biz.service.operate.ReviewService;

/**
 * 单词本
 *
 * @author zhanshifeng
 * @date 2019-12-08 23:27:41
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ParaphraseStarListServiceImpl extends ServiceImpl<ParaphraseStarListMapper, ParaphraseStarListDO>
    implements ParaphraseStarListService {

    private final ParaphraseStarListMapper mapper;
    private final ParaphraseStarRelService relService;
    private final AsyncArchiveService archiveService;
    private final ReviewService reviewService;

    @Override
    public Integer countById(Integer id) {
        return this.count(new QueryWrapper<>(new ParaphraseStarListDO().setId(id)));
    }

    @Override
    public List<ParaphraseStarListVO> getCurrentUserList(Integer userId) {
        QueryWrapper<ParaphraseStarListDO> queryWrapper =
            new QueryWrapper<>(new ParaphraseStarListDO().setOwner(userId).setIsDel(GlobalConstants.FLAG_N)).select(
                ParaphraseStarListDO.class,
                tableFieldInfo -> WordParaphraseStarListColumn.ID.equals(tableFieldInfo.getColumn())
                    || WordParaphraseStarListColumn.LIST_NAME.equals(tableFieldInfo.getColumn())
                    || WordParaphraseStarListColumn.REMARK.equals(tableFieldInfo.getColumn()));

        return KiwiBeanUtils.convertFrom(mapper.selectList(queryWrapper), ParaphraseStarListVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateListByUser(ParaphraseStarListDO entity, Integer id, Integer userId) {
        UpdateWrapper<ParaphraseStarListDO> updateWrapper =
            new UpdateWrapper<>(new ParaphraseStarListDO().setOwner(userId).setId(id));
        return this.update(entity, updateWrapper);
    }

    @Override
    public IPage<ParaphraseStarItemVO> selectListItems(Page<ParaphraseStarListDO> page, Integer listId) {
        if (listId == 0) {
            return mapper.selectRecentItems(page, SecurityUtils.getCurrentUserId());
        }
        return mapper.selectItems(page, listId);
    }

    /**
     * In review mode, It needs to save the page number, and it can choose to review again at the breakpoint next time.
     *
     * @param page
     * @param listId
     * @return
     */
    @Override
    public IPage<ParaphraseStarItemVO> selectReviewListItems(Page<ParaphraseStarListDO> page, Integer listId) {
        // When querying the list to be remembered, record the current query page number.
        reviewService.recordReviewPageNumber(listId, page.getCurrent(), ReviewBreakpointTypeEnum.REMEMBER.getType(),
            SecurityUtils.getCurrentUserId());
        if (listId == 0) {
            return mapper.selectRecentReviewItems(page, SecurityUtils.getCurrentUserId());
        }
        return mapper.selectReviewItems(page, listId);
    }

    @Override
    public IPage<ParaphraseStarItemVO> selectRememberListItems(Page<ParaphraseStarListDO> page, Integer listId) {
        // When querying the list to be kept in mind, record the current query page number.
        reviewService.recordReviewPageNumber(listId, page.getCurrent(), ReviewBreakpointTypeEnum.KEEP_IN_MIND.getType(),
            SecurityUtils.getCurrentUserId());
        if (listId == 0) {
            return mapper.selectRecentRememberItems(page, SecurityUtils.getCurrentUserId());
        }
        return mapper.selectRememberItems(page, listId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
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
    @Transactional(rollbackFor = Exception.class)
    public void rememberOne(Integer paraphraseId, Integer listId) {
        reviewService.increase(ReviewDailyCounterTypeEnum.REMEMBER.getType(), SecurityUtils.getCurrentUserId());
        relService.update(
            new ParaphraseStarRelDO().setIsRemember(GlobalConstants.FLAG_DEL_YES).setRememberTime(LocalDateTime.now()),
            Wrappers.<ParaphraseStarRelDO>lambdaQuery().eq(ParaphraseStarRelDO::getListId, listId)
                .eq(ParaphraseStarRelDO::getParaphraseId, paraphraseId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void keepInMind(Integer paraphraseId, Integer listId) {
        reviewService.increase(ReviewDailyCounterTypeEnum.KEEP_IN_MIND.getType(), SecurityUtils.getCurrentUserId());
        relService.update(
            new ParaphraseStarRelDO().setIsKeepInMind(GlobalConstants.FLAG_DEL_YES)
                .setKeepInMindTime(LocalDateTime.now()),
            Wrappers.<ParaphraseStarRelDO>lambdaQuery().eq(ParaphraseStarRelDO::getListId, listId)
                .eq(ParaphraseStarRelDO::getParaphraseId, paraphraseId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void forgetOne(Integer paraphraseId, Integer listId) {
        relService.update(
            new ParaphraseStarRelDO().setIsRemember(GlobalConstants.FLAG_DEL_NO)
                .setIsKeepInMind(GlobalConstants.FLAG_DEL_NO),
            Wrappers.<ParaphraseStarRelDO>lambdaQuery().eq(ParaphraseStarRelDO::getListId, listId)
                .eq(ParaphraseStarRelDO::getParaphraseId, paraphraseId));
    }

    @Override
    public List<Integer> findAllUserId() {
        return null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public void putIntoStarList(Integer paraphraseId, Integer listId) {
        log.info("Method putIntoStarList is invoked, paraphraseId is {}, listId is {}", paraphraseId, listId);
        LambdaQueryWrapper<ParaphraseStarRelDO> wrapper = Wrappers.<ParaphraseStarRelDO>lambdaQuery()
            .eq(ParaphraseStarRelDO::getListId, listId).eq(ParaphraseStarRelDO::getParaphraseId, paraphraseId);
        if (relService.count(wrapper) > 0) {
            return;
        }
        relService.save(new ParaphraseStarRelDO().setListId(listId).setParaphraseId(paraphraseId));
        archiveService.archiveParaphraseRel(paraphraseId, listId, SecurityUtils.getCurrentUserId());
    }
}
