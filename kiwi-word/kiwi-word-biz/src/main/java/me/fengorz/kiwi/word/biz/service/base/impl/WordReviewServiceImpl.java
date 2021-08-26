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
package me.fengorz.kiwi.word.biz.service.base.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.AllArgsConstructor;
import me.fengorz.kiwi.bdf.core.service.ISeqService;
import me.fengorz.kiwi.common.api.constant.MapperConstant;
import me.fengorz.kiwi.common.sdk.util.bean.KiwiBeanUtils;
import me.fengorz.kiwi.common.sdk.web.security.SecurityUtils;
import me.fengorz.kiwi.word.api.common.ReviewDailyCounterTypeEnum;
import me.fengorz.kiwi.word.api.common.WordConstants;
import me.fengorz.kiwi.word.api.entity.WordBreakpointReviewDO;
import me.fengorz.kiwi.word.api.entity.WordReviewDailyCounterDO;
import me.fengorz.kiwi.word.api.vo.WordReviewDailyCounterVO;
import me.fengorz.kiwi.word.biz.mapper.BreakpointReviewMapper;
import me.fengorz.kiwi.word.biz.mapper.ReviewDailyCounterMapper;
import me.fengorz.kiwi.word.biz.service.base.IWordReviewService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 断点复习记录表
 *
 * @author zhanShiFeng
 * @date 2021-06-06 14:53:44
 */
@Service
@AllArgsConstructor
public class WordReviewServiceImpl implements IWordReviewService {

    private final BreakpointReviewMapper breakpointReviewMapper;
    private final ISeqService seqService;
    private final ReviewDailyCounterMapper reviewDailyCounterMapper;

    @Override
    public List<WordBreakpointReviewDO> listBreakpointReview(Integer listId) {
        return breakpointReviewMapper.selectList(Wrappers.<WordBreakpointReviewDO>lambdaQuery()
                .eq(WordBreakpointReviewDO::getListId, listId)
                .eq(WordBreakpointReviewDO::getUserId, SecurityUtils.getCurrentUserId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addOne(Integer listId, Integer lastPage) {
        // TODO ZSF 应用上分布式缓存锁
        WordBreakpointReviewDO reviewDO = new WordBreakpointReviewDO().setId(seqService.genIntSequence(MapperConstant.T_INS_SEQUENCE))
                .setOperateTime(LocalDateTime.now()).setUserId(SecurityUtils.getCurrentUserId())
                .setType(WordConstants.BREAKPOINT_REVIEW_TYPE_PARAPHRASE).setLastPage(lastPage).setListId(listId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createTheDays() {
        Integer userId = SecurityUtils.getCurrentUserId();
        if (getDO(userId, ReviewDailyCounterTypeEnum.REVIEW.getType()) == null) {
            createDO(ReviewDailyCounterTypeEnum.REVIEW.getType());
        }
        if (getDO(userId, ReviewDailyCounterTypeEnum.KEEP_IN_MIND.getType()) == null) {
            createDO(ReviewDailyCounterTypeEnum.KEEP_IN_MIND.getType());
        }
        if (getDO(userId, ReviewDailyCounterTypeEnum.REMEMBER.getType()) == null) {
            createDO(ReviewDailyCounterTypeEnum.REMEMBER.getType());
        }
    }

    @Async
    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public void increase(int type) {
        WordReviewDailyCounterDO counter = getDO(SecurityUtils.getCurrentUserId(), type);
        if (counter == null) {
            this.createTheDays();
            counter = getDO(SecurityUtils.getCurrentUserId(), type);
        }
        counter.setReviewCount(counter.getReviewCount() + 1);
        reviewDailyCounterMapper.updateById(counter);
    }

    @Override
    public WordReviewDailyCounterVO getVO(int userId, int type) {
        return KiwiBeanUtils.convertFrom(getDO(userId, type), WordReviewDailyCounterVO.class);
    }

    @Override
    @Async
    @Transactional(rollbackFor = RuntimeException.class, propagation = Propagation.REQUIRES_NEW)
    public void recordReviewPageNumber(int listId, Long pageNumber, int type) {
        // If the record exists, update the page number directly.
        LambdaQueryWrapper<WordBreakpointReviewDO> queryWrapper = Wrappers.<WordBreakpointReviewDO>lambdaQuery()
                .eq(WordBreakpointReviewDO::getUserId, SecurityUtils.getCurrentUserId())
                .eq(WordBreakpointReviewDO::getType, type)
                .eq(WordBreakpointReviewDO::getListId, listId);
        WordBreakpointReviewDO breakpoint = breakpointReviewMapper.selectOne(queryWrapper);
        if (breakpoint == null) {
            firstRecordReviewPageNumber(listId, pageNumber, type);
        } else {
            breakpoint.setLastPage(pageNumber.intValue())
                    .setOperateTime(LocalDateTime.now());
            breakpointReviewMapper.updateById(breakpoint);
        }
    }

    private WordReviewDailyCounterDO getDO(int userId, int type) {
        LambdaQueryWrapper<WordReviewDailyCounterDO> wrapper = Wrappers.<WordReviewDailyCounterDO>lambdaQuery()
                .eq(WordReviewDailyCounterDO::getUserId, userId)
                .eq(WordReviewDailyCounterDO::getType, type)
                .eq(WordReviewDailyCounterDO::getToday, LocalDateTime.now().toLocalDate());
        return reviewDailyCounterMapper.selectOne(wrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    private void createDO(int type) {
        WordReviewDailyCounterDO counterDO = new WordReviewDailyCounterDO();
        counterDO.setId(0).setUserId(SecurityUtils.getCurrentUserId())
                .setReviewCount(0)
                .setToday(LocalDateTime.now().toLocalDate())
                .setType(type);
        reviewDailyCounterMapper.insert(counterDO);
    }

    private void firstRecordReviewPageNumber(int listId, Long pageNumber, int type) {
        WordBreakpointReviewDO breakpointReviewDO = new WordBreakpointReviewDO();
        breakpointReviewDO.setId(0)
                .setLastPage(pageNumber.intValue())
                .setOperateTime(LocalDateTime.now())
                .setType(type)
                .setUserId(SecurityUtils.getCurrentUserId())
                .setListId(listId);
        breakpointReviewMapper.insert(breakpointReviewDO);
    }

}
