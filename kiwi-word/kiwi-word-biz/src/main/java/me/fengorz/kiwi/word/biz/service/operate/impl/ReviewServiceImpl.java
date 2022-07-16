/*
 *
 * Copyright [2019~2025] [codingByFeng]
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
package me.fengorz.kiwi.word.biz.service.operate.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.bdf.core.service.ISeqService;
import me.fengorz.kiwi.common.fastdfs.service.DfsService;
import me.fengorz.kiwi.common.sdk.constant.GlobalConstants;
import me.fengorz.kiwi.common.sdk.constant.MapperConstant;
import me.fengorz.kiwi.common.sdk.exception.AuthException;
import me.fengorz.kiwi.common.sdk.exception.DataCheckedException;
import me.fengorz.kiwi.common.sdk.exception.ResourceNotFoundException;
import me.fengorz.kiwi.common.sdk.exception.dfs.DfsOperateDeleteException;
import me.fengorz.kiwi.common.sdk.exception.dfs.DfsOperateException;
import me.fengorz.kiwi.common.sdk.exception.tts.TtsException;
import me.fengorz.kiwi.common.sdk.util.bean.KiwiBeanUtils;
import me.fengorz.kiwi.common.sdk.web.security.SecurityUtils;
import me.fengorz.kiwi.word.api.common.ReviewDailyCounterTypeEnum;
import me.fengorz.kiwi.word.api.common.WordConstants;
import me.fengorz.kiwi.word.api.entity.WordBreakpointReviewDO;
import me.fengorz.kiwi.word.api.entity.WordReviewAudioDO;
import me.fengorz.kiwi.word.api.entity.WordReviewDailyCounterDO;
import me.fengorz.kiwi.word.api.vo.WordReviewDailyCounterVO;
import me.fengorz.kiwi.word.biz.enumeration.ReviewAudioSourceEnum;
import me.fengorz.kiwi.word.biz.enumeration.ReviewAudioTypeEnum;
import me.fengorz.kiwi.word.biz.enumeration.ReviewPermanentAudioEnum;
import me.fengorz.kiwi.word.biz.mapper.*;
import me.fengorz.kiwi.word.biz.service.operate.AudioService;
import me.fengorz.kiwi.word.biz.service.operate.IReviewService;
import me.fengorz.kiwi.word.biz.util.WordDfsUtils;

/**
 * 复习功能服务类
 *
 * @author zhanShiFeng
 * @date 2021-06-06 14:53:44
 */
@Slf4j
@Service
@AllArgsConstructor
public class ReviewServiceImpl implements IReviewService {

    private final BreakpointReviewMapper breakpointReviewMapper;
    private final ISeqService seqService;
    private final ReviewDailyCounterMapper reviewDailyCounterMapper;
    private final ReviewAudioMapper reviewAudioMapper;
    private final ParaphraseMapper paraphraseMapper;
    private final ParaphraseExampleMapper paraphraseExampleMapper;
    private final DfsService dfsService;
    private final AudioService audioService;

    @Override
    public List<WordBreakpointReviewDO> listBreakpointReview(Integer listId) {
        return breakpointReviewMapper
            .selectList(Wrappers.<WordBreakpointReviewDO>lambdaQuery().eq(WordBreakpointReviewDO::getListId, listId)
                .eq(WordBreakpointReviewDO::getUserId, SecurityUtils.getCurrentUserId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addOne(Integer listId, Integer lastPage) {
        // TODO ZSF 应用上分布式缓存锁
        WordBreakpointReviewDO reviewDO =
            new WordBreakpointReviewDO().setId(seqService.genIntSequence(MapperConstant.T_INS_SEQUENCE))
                .setOperateTime(LocalDateTime.now()).setUserId(SecurityUtils.getCurrentUserId())
                .setType(WordConstants.BREAKPOINT_REVIEW_TYPE_PARAPHRASE).setLastPage(lastPage).setListId(listId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createTheDays(Integer userId) {
        if (userId == null) {
            throw new AuthException("userId cannot be null!");
        }
        if (getDO(userId, ReviewDailyCounterTypeEnum.REVIEW.getType()) == null) {
            createDO(ReviewDailyCounterTypeEnum.REVIEW.getType(), userId);
        }
        if (getDO(userId, ReviewDailyCounterTypeEnum.KEEP_IN_MIND.getType()) == null) {
            createDO(ReviewDailyCounterTypeEnum.KEEP_IN_MIND.getType(), userId);
        }
        if (getDO(userId, ReviewDailyCounterTypeEnum.REMEMBER.getType()) == null) {
            createDO(ReviewDailyCounterTypeEnum.REMEMBER.getType(), userId);
        }
    }

    @Async
    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public void increase(int type, Integer userId) {
        WordReviewDailyCounterDO counter = getDO(userId, type);
        if (counter == null) {
            this.createTheDays(userId);
            counter = getDO(userId, type);
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
    public void recordReviewPageNumber(int listId, Long pageNumber, int type, Integer userId) {
        // If the record exists, update the page number directly.
        LambdaQueryWrapper<WordBreakpointReviewDO> queryWrapper =
            Wrappers.<WordBreakpointReviewDO>lambdaQuery().eq(WordBreakpointReviewDO::getUserId, userId)
                .eq(WordBreakpointReviewDO::getType, type).eq(WordBreakpointReviewDO::getListId, listId);
        WordBreakpointReviewDO breakpoint = breakpointReviewMapper.selectOne(queryWrapper);
        if (breakpoint == null) {
            firstRecordReviewPageNumber(listId, pageNumber, type, userId);
        } else {
            breakpoint.setLastPage(pageNumber.intValue()).setOperateTime(LocalDateTime.now());
            breakpointReviewMapper.updateById(breakpoint);
        }
    }

    @Override
    public WordReviewAudioDO findWordReviewAudio(Integer sourceId, Integer type) {
        return Optional.ofNullable(reviewAudioMapper.selectOne(Wrappers.<WordReviewAudioDO>lambdaQuery()
            .eq(WordReviewAudioDO::getSourceId, sourceId).eq(WordReviewAudioDO::getType, type))).orElseGet(() -> {

                WordReviewAudioDO wordReviewAudioDO = new WordReviewAudioDO();
                try {
                    String englishText = acquireEnglishText(sourceId, type);
                    String uploadResult = audioService.generateEnglishVoice(englishText);
                    wordReviewAudioDO.setId(seqService.genIntSequence(MapperConstant.T_INS_SEQUENCE));
                    wordReviewAudioDO.setGroupName(WordDfsUtils.getGroupName(uploadResult));
                    wordReviewAudioDO.setFilePath(WordDfsUtils.getUploadVoiceFilePath(uploadResult));
                    wordReviewAudioDO.setSourceId(sourceId);
                    wordReviewAudioDO.setType(type);
                    wordReviewAudioDO.setIsDel(GlobalConstants.FLAG_DEL_NO);
                    wordReviewAudioDO.setCreateTime(LocalDateTime.now());
                    wordReviewAudioDO.setSourceUrl(ReviewAudioSourceEnum.VOICERSS.getSource());
                    wordReviewAudioDO.setSourceText(englishText);
                    reviewAudioMapper.insert(wordReviewAudioDO);
                } catch (TtsException | DfsOperateException | DataCheckedException e) {
                    log.error(e.getMessage(), e);
                }
                return wordReviewAudioDO;
            });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void initPermanent(boolean isReplace) throws DfsOperateException, TtsException {
        for (ReviewPermanentAudioEnum audio : ReviewPermanentAudioEnum.values()) {
            Optional.ofNullable(reviewAudioMapper.selectOne(
                Wrappers.<WordReviewAudioDO>lambdaQuery().eq(WordReviewAudioDO::getSourceId, audio.getSourceId())
                    .eq(WordReviewAudioDO::getType, audio.getType())))
                .ifPresent(wordReviewAudioDO -> {
                    if (!isReplace) {
                        return;
                    }
                    try {
                        dfsService.deleteFile(wordReviewAudioDO.getGroupName(), wordReviewAudioDO.getFilePath());
                    } catch (DfsOperateDeleteException e) {
                        log.error("Error deleting old wordReviewAudioDO", e);
                    } finally {
                        reviewAudioMapper.deleteById(wordReviewAudioDO.getId());
                    }
                });
            log.info("Audio is generating..., {}", audio.getText());
            WordReviewAudioDO wordReviewAudioDO = new WordReviewAudioDO();
            try {
                String uploadResult = audioService.generateVoice(audio.getText(), audio.getType());
                wordReviewAudioDO.setId(seqService.genIntSequence(MapperConstant.T_INS_SEQUENCE));
                wordReviewAudioDO.setGroupName(WordDfsUtils.getGroupName(uploadResult));
                wordReviewAudioDO.setFilePath(WordDfsUtils.getUploadVoiceFilePath(uploadResult));
                wordReviewAudioDO.setSourceId(audio.getSourceId());
                wordReviewAudioDO.setType(audio.getType());
                wordReviewAudioDO.setIsDel(GlobalConstants.FLAG_DEL_NO);
                wordReviewAudioDO.setCreateTime(LocalDateTime.now());
                wordReviewAudioDO.setSourceUrl(ReviewAudioSourceEnum.VOICERSS.getSource());
                wordReviewAudioDO.setSourceText(audio.getText());
                reviewAudioMapper.insert(wordReviewAudioDO);
            } catch (TtsException | DfsOperateException e) {
                log.error(e.getMessage(), e);
                throw e;
            }
        }
    }

    private String acquireEnglishText(Integer sourceId, Integer type) throws DataCheckedException {
        if (ReviewAudioTypeEnum.isParaphrase(type)) {
            return Optional.ofNullable(paraphraseMapper.selectById(sourceId))
                .orElseThrow(() -> new ResourceNotFoundException("Paraphrase cannot be found!")).getParaphraseEnglish();
        } else if (ReviewAudioTypeEnum.isExample(type)) {
            return Optional.ofNullable(paraphraseExampleMapper.selectById(sourceId))
                .orElseThrow(() -> new ResourceNotFoundException("Paraphrase example cannot be found!"))
                .getExampleSentence();
        }
        throw new DataCheckedException("English text cannot be found!");
    }

    private String acquireChineseText(Integer sourceId, Integer type) throws DataCheckedException {
        if (ReviewAudioTypeEnum.isParaphrase(type)) {
            return Optional.ofNullable(paraphraseMapper.selectById(sourceId))
                .orElseThrow(() -> new ResourceNotFoundException("Paraphrase cannot be found!"))
                .getParaphraseEnglishTranslate();
        } else if (ReviewAudioTypeEnum.isExample(type)) {
            return Optional.ofNullable(paraphraseExampleMapper.selectById(sourceId))
                .orElseThrow(() -> new ResourceNotFoundException("Paraphrase example cannot be found!"))
                .getExampleTranslate();
        }
        throw new DataCheckedException("Chinese text cannot be found!");
    }

    private WordReviewDailyCounterDO getDO(int userId, int type) {
        LambdaQueryWrapper<WordReviewDailyCounterDO> wrapper = Wrappers.<WordReviewDailyCounterDO>lambdaQuery()
            .eq(WordReviewDailyCounterDO::getUserId, userId).eq(WordReviewDailyCounterDO::getType, type)
            .eq(WordReviewDailyCounterDO::getToday, LocalDateTime.now().toLocalDate());
        return reviewDailyCounterMapper.selectOne(wrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    private void createDO(int type, Integer userId) {
        WordReviewDailyCounterDO counterDO = new WordReviewDailyCounterDO();
        counterDO.setId(0).setUserId(userId).setReviewCount(0).setToday(LocalDateTime.now().toLocalDate())
            .setType(type);
        reviewDailyCounterMapper.insert(counterDO);
    }

    private void firstRecordReviewPageNumber(int listId, Long pageNumber, int type, Integer userId) {
        WordBreakpointReviewDO breakpointReviewDO = new WordBreakpointReviewDO();
        breakpointReviewDO.setId(0).setLastPage(pageNumber.intValue()).setOperateTime(LocalDateTime.now()).setType(type)
            .setUserId(userId).setListId(listId);
        breakpointReviewMapper.insert(breakpointReviewDO);
    }

}
