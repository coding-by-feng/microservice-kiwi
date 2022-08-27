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
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.bdf.core.service.SeqService;
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
import me.fengorz.kiwi.common.sdk.util.lang.collection.KiwiCollectionUtils;
import me.fengorz.kiwi.common.sdk.web.security.SecurityUtils;
import me.fengorz.kiwi.common.tts.TtsConstants;
import me.fengorz.kiwi.common.tts.model.TtsConfig;
import me.fengorz.kiwi.common.tts.service.TtsService;
import me.fengorz.kiwi.word.api.common.WordConstants;
import me.fengorz.kiwi.word.api.common.enumeration.ReviewAudioSourceEnum;
import me.fengorz.kiwi.word.api.common.enumeration.ReviewAudioTypeEnum;
import me.fengorz.kiwi.word.api.common.enumeration.ReviewDailyCounterTypeEnum;
import me.fengorz.kiwi.word.api.common.enumeration.ReviewPermanentAudioEnum;
import me.fengorz.kiwi.word.api.entity.*;
import me.fengorz.kiwi.word.api.vo.WordReviewDailyCounterVO;
import me.fengorz.kiwi.word.biz.mapper.*;
import me.fengorz.kiwi.word.biz.service.operate.AudioService;
import me.fengorz.kiwi.word.biz.service.operate.ReviewService;
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
public class ReviewServiceImpl implements ReviewService {

    private final BreakpointReviewMapper breakpointReviewMapper;
    private final SeqService seqService;
    private final ReviewDailyCounterMapper reviewDailyCounterMapper;
    private final ReviewAudioMapper reviewAudioMapper;
    private final ParaphraseMapper paraphraseMapper;
    private final ParaphraseExampleMapper paraphraseExampleMapper;
    private final WordMainMapper wordMainMapper;
    private final CharacterMapper characterMapper;
    private final ParaphraseStarRelMapper paraphraseStarRelMapper;
    private final DfsService dfsService;
    private final AudioService audioService;
    private final TtsConfig ttsConfig;
    private final TtsService ttsService;

    private final static Semaphore STORAGE = new Semaphore(10);

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
        synchronized (BARRIER_FOR_DAYS) {
            for (ReviewDailyCounterTypeEnum typeEnum : ReviewDailyCounterTypeEnum.values()) {
                if (findReviewCounterDO(userId, typeEnum.getType()) == null) {
                    createDO(typeEnum.getType(), userId);
                    log.info("userId[{}] ReviewDailyCounterType[{}] is lacking， created", userId, typeEnum.name());
                } else {
                    log.info("userId[{}] ReviewDailyCounterType[{}] is created.", userId, typeEnum.name());
                }
            }
            for (String apiKey : ttsConfig.listApiKey()) {
                ttsService.useTtsApiKey(apiKey, 0);
            }
            ttsService.useTtsApiKey(TtsConstants.CACHE_KEY_PREFIX_TTS.TOTAL_API_KEY, 0);
        }
    }

    @Async
    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public void increase(int type, Integer userId) {
        if (ReviewDailyCounterTypeEnum.REVIEW_AUDIO_VOICERSS_TTS_COUNTER.getType() == type) {
            synchronized (BARRIER) {
                ttsService.voiceRssGlobalIncreaseCounter();
            }
        }
        WordReviewDailyCounterDO counter = findReviewCounterDO(userId, type);
        if (counter == null) {
            createTheDays(userId);
            counter = findReviewCounterDO(userId, type);
        }
        counter.setReviewCount(counter.getReviewCount() + 1);
        reviewDailyCounterMapper.updateById(counter);
    }

    @Override
    public WordReviewDailyCounterVO findReviewCounterVO(int userId, int type) {
        return KiwiBeanUtils.convertFrom(findReviewCounterDO(userId, type), WordReviewDailyCounterVO.class);
    }

    @Override
    public List<WordReviewDailyCounterVO> listReviewCounterVO(int userId) {
        return KiwiBeanUtils.convertFrom(
            reviewDailyCounterMapper.selectList(
                Wrappers.<WordReviewDailyCounterDO>lambdaQuery().eq(WordReviewDailyCounterDO::getUserId, userId)
                    .eq(WordReviewDailyCounterDO::getToday, LocalDateTime.now().toLocalDate())),
            WordReviewDailyCounterVO.class, "id", "today");
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
    public WordReviewAudioDO findWordReviewAudio(Integer sourceId, Integer type)
        throws DfsOperateException, TtsException, DataCheckedException {
        WordReviewAudioDO wordReviewAwoudioDO = reviewAudioMapper.selectOne(Wrappers.<WordReviewAudioDO>lambdaQuery()
            .eq(WordReviewAudioDO::getSourceId, sourceId).eq(WordReviewAudioDO::getType, type));
        if (Objects.isNull(wordReviewAwoudioDO)) {
            wordReviewAwoudioDO = generateWordReviewAudio(false, sourceId, type);
        }
        return wordReviewAwoudioDO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public WordReviewAudioDO generateWordReviewAudio(boolean isReplace, Integer sourceId, Integer type)
        throws DfsOperateException, TtsException, DataCheckedException {
        WordReviewAudioDO wordReviewAudioDO = reviewAudioMapper.selectOne(Wrappers.<WordReviewAudioDO>lambdaQuery()
            .eq(WordReviewAudioDO::getSourceId, sourceId).eq(WordReviewAudioDO::getType, type));
        if (wordReviewAudioDO == null) {
            wordReviewAudioDO = new WordReviewAudioDO();
        } else if (isReplace) {
            try {
                dfsService.deleteFile(wordReviewAudioDO.getGroupName(), wordReviewAudioDO.getFilePath());
            } catch (DfsOperateDeleteException e) {
                log.error("Error deleting old wordReviewAudioDO", e);
            }
            this.delete(wordReviewAudioDO.getId());
            wordReviewAudioDO = new WordReviewAudioDO();
        } else {
            return wordReviewAudioDO;
        }
        try {
            String text = acquireText(sourceId, type);
            String uploadResult = audioService.generateVoice(text, type);
            wordReviewAudioDO.setId(seqService.genIntSequence(MapperConstant.T_INS_SEQUENCE));
            wordReviewAudioDO.setGroupName(WordDfsUtils.getGroupName(uploadResult));
            wordReviewAudioDO.setFilePath(WordDfsUtils.getUploadVoiceFilePath(uploadResult));
            wordReviewAudioDO.setSourceId(sourceId);
            wordReviewAudioDO.setType(type);
            wordReviewAudioDO.setIsDel(GlobalConstants.FLAG_DEL_NO);
            wordReviewAudioDO.setCreateTime(LocalDateTime.now());
            wordReviewAudioDO.setSourceUrl(ReviewAudioSourceEnum.VOICERSS.getSource());
            wordReviewAudioDO.setSourceText(StringUtils.defaultIfBlank(text, GlobalConstants.EMPTY));
            reviewAudioMapper.insert(wordReviewAudioDO);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw e;
        }

        return wordReviewAudioDO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void initPermanent(boolean isReplace, boolean isOnlyTest) throws DfsOperateException, TtsException {
        for (ReviewPermanentAudioEnum audio : ReviewPermanentAudioEnum.values()) {
            if (isOnlyTest && !ReviewPermanentAudioEnum.TEST.equals(audio)) {
                continue;
            }
            log.info("Audio is generating..., {}", audio.getText());
            WordReviewAudioDO wordReviewAudioDO = reviewAudioMapper.selectOne(
                Wrappers.<WordReviewAudioDO>lambdaQuery().eq(WordReviewAudioDO::getSourceId, audio.getSourceId())
                    .eq(WordReviewAudioDO::getType, audio.getType()));
            if (wordReviewAudioDO == null) {
                wordReviewAudioDO = new WordReviewAudioDO();
            } else if (isReplace) {
                try {
                    dfsService.deleteFile(wordReviewAudioDO.getGroupName(), wordReviewAudioDO.getFilePath());
                } catch (DfsOperateDeleteException e) {
                    log.error("Error deleting old wordReviewAudioDO", e);
                }
                reviewAudioMapper.deleteById(wordReviewAudioDO.getId());
                wordReviewAudioDO = new WordReviewAudioDO();
            } else {
                continue;
            }
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

    @Override
    public void generateTtsVoice(boolean isReplace) throws InterruptedException {
        List<ParaphraseStarRelDO> relList = paraphraseStarRelMapper.selectList(Wrappers
            .<ParaphraseStarRelDO>lambdaQuery().eq(ParaphraseStarRelDO::getIsKeepInMind, GlobalConstants.FLAG_DEL_NO));
        // noinspection AlibabaThreadPoolCreation
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        for (ParaphraseStarRelDO starRelDO : relList) {
            STORAGE.acquire();
            executorService.submit(() -> {
                try {
                    log.info("executorService submit a task for generateTtsVoiceFromParaphraseId.");
                    generateTtsVoiceFromParaphraseId(isReplace, starRelDO.getParaphraseId());
                } catch (Exception e) {
                    log.error("generateTtsVoice error, paraphraseId is {}", starRelDO.getParaphraseId());
                    log.error(e.getMessage(), e);
                } finally {
                    STORAGE.release();
                    System.gc();
                }
            });
        }
    }

    @Override
    public void generateTtsVoiceFromParaphraseId(Integer paraphraseId)
        throws DfsOperateException, TtsException, DataCheckedException {
        generateTtsVoiceFromParaphraseId(true, paraphraseId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public void cleanReviewVoiceByParaphraseId(Integer paraphraseId) {
        ListUtils
            .emptyIfNull(reviewAudioMapper
                .selectList(Wrappers.<WordReviewAudioDO>lambdaQuery().eq(WordReviewAudioDO::getSourceId, paraphraseId)))
            .forEach(this::cleanReviewAudio);
        List<ParaphraseExampleDO> examples = paraphraseExampleMapper.selectList(
            Wrappers.<ParaphraseExampleDO>lambdaQuery().eq(ParaphraseExampleDO::getParaphraseId, paraphraseId));
        if (CollectionUtils.isEmpty(examples)) {
            return;
        }
        examples.forEach(paraphraseExampleDO -> ListUtils
            .emptyIfNull(reviewAudioMapper.selectList(Wrappers.<WordReviewAudioDO>lambdaQuery()
                .eq(WordReviewAudioDO::getSourceId, paraphraseExampleDO.getExampleId())))
            .forEach(this::cleanReviewAudio));
    }

    private void cleanReviewAudio(WordReviewAudioDO wordReviewAudioDO) {
        try {
            dfsService.deleteFile(wordReviewAudioDO.getGroupName(), wordReviewAudioDO.getFilePath());
        } catch (DfsOperateDeleteException e) {
            log.error("Review audio file delete failed, group is {}, path is {}", wordReviewAudioDO.getGroupName(),
                wordReviewAudioDO.getFilePath());
        } finally {
            reviewAudioMapper.deleteById(wordReviewAudioDO.getId());
        }
    }

    @Override
    public Integer getReviewBreakpointPageNumber(Integer listId) {
        List<WordBreakpointReviewDO> list = listBreakpointReview(listId);
        if (KiwiCollectionUtils.isEmpty(list)) {
            return 0;
        } else {
            return list.get(0).getLastPage();
        }
    }

    private void generateTtsVoiceFromParaphraseId(boolean isReplace, Integer paraphraseId)
        throws DfsOperateException, TtsException, DataCheckedException {
        log.info("generateTtsVoiceFromParaphraseId beginning, paraphraseId is {}", paraphraseId);
        ParaphraseDO paraphraseDO = paraphraseMapper.selectById(paraphraseId);
        if (paraphraseDO == null) {
            log.info("paraphraseDO is null, skipping, paraphraseId is {}", paraphraseId);
            return;
        }
        CharacterDO characterDO = characterMapper.selectOne(
            Wrappers.<CharacterDO>lambdaQuery().eq(CharacterDO::getCharacterId, paraphraseDO.getCharacterId()));
        if (characterDO == null) {
            log.info("characterDO is null, skipping, characterDO is {}", paraphraseDO.getCharacterId());
            return;
        }
        List<ParaphraseExampleDO> paraphraseExamples = paraphraseExampleMapper.selectList(
            Wrappers.<ParaphraseExampleDO>lambdaQuery().eq(ParaphraseExampleDO::getParaphraseId, paraphraseId));
        generateWordReviewAudio(true, paraphraseDO.getWordId(), ReviewAudioTypeEnum.WORD_SPELLING.getType());
        generateWordReviewAudio(isReplace, paraphraseId, ReviewAudioTypeEnum.PARAPHRASE_EN.getType());
        generateWordReviewAudio(isReplace, paraphraseId, ReviewAudioTypeEnum.PARAPHRASE_CH.getType());
        generateWordReviewAudio(isReplace, characterDO.getCharacterId(), ReviewAudioTypeEnum.CHARACTER_CH.getType());
        for (ParaphraseExampleDO paraphraseExample : paraphraseExamples) {
            generateWordReviewAudio(isReplace, paraphraseExample.getExampleId(),
                ReviewAudioTypeEnum.EXAMPLE_EN.getType());
            generateWordReviewAudio(isReplace, paraphraseExample.getExampleId(),
                ReviewAudioTypeEnum.EXAMPLE_CH.getType());
        }
        log.info("generateTtsVoiceFromParaphraseId success, paraphraseId is {}", paraphraseId);
    }

    private String acquireText(Integer sourceId, Integer type) throws DataCheckedException {
        if (ReviewAudioTypeEnum.isParaphrase(type)) {
            ParaphraseDO paraphraseDO = Optional.ofNullable(paraphraseMapper.selectById(sourceId)).orElseThrow(
                () -> new ResourceNotFoundException(String.format("Paraphrase[id=%s] cannot be found!", sourceId)));
            if (ReviewAudioTypeEnum.isEnglish(type)) {
                return paraphraseDO.getParaphraseEnglish();
            } else if (ReviewAudioTypeEnum.isChinese(type)) {
                return StringUtils.defaultIfBlank(paraphraseDO.getMeaningChinese(),
                    ReviewPermanentAudioEnum.WORD_PARAPHRASE_MISSING.getText());
            }
        } else if (ReviewAudioTypeEnum.isExample(type)) {
            ParaphraseExampleDO paraphraseExampleDO = Optional.ofNullable(paraphraseExampleMapper.selectById(sourceId))
                .orElseThrow(() -> new ResourceNotFoundException("Paraphrase example cannot be found!"));
            if (ReviewAudioTypeEnum.isEnglish(type)) {
                return paraphraseExampleDO.getExampleSentence();
            } else if (ReviewAudioTypeEnum.isChinese(type)) {
                return paraphraseExampleDO.getExampleTranslate();
            }
        } else if (ReviewAudioTypeEnum.isSpelling(type)) {
            WordMainDO wordMainDO = Optional.ofNullable(wordMainMapper.selectById(sourceId))
                .orElseThrow(() -> new ResourceNotFoundException("Word cannot be found!"));
            StringBuilder sb = new StringBuilder();
            for (char alphabet : wordMainDO.getWordName().toCharArray()) {
                sb.append(alphabet).append(GlobalConstants.SYMBOL_COMMA);
            }
            return sb.toString();
        } else if (ReviewAudioTypeEnum.isCharacter(type)) {
            CharacterDO characterDO = Optional.ofNullable(characterMapper.selectById(sourceId))
                .orElseThrow(() -> new ResourceNotFoundException("Character cannot be found!"));
            return ReviewPermanentAudioEnum.WORD_CHARACTER.getText() + characterDO.getCharacterCode();
        } else if (ReviewAudioTypeEnum.isWord(type)) {
            WordMainDO wordMainDO = Optional.ofNullable(wordMainMapper.selectById(sourceId))
                .orElseThrow(() -> new ResourceNotFoundException("word cannot be found!"));
            return wordMainDO.getWordName();
        }
        throw new DataCheckedException("English text cannot be found!");
    }

    private String acquireChineseText(Integer sourceId, Integer type) throws DataCheckedException {
        if (ReviewAudioTypeEnum.isParaphrase(type)) {
            return Optional.ofNullable(paraphraseMapper.selectById(sourceId))
                .orElseThrow(() -> new ResourceNotFoundException("Paraphrase cannot be found!")).getMeaningChinese();
        } else if (ReviewAudioTypeEnum.isExample(type)) {
            return Optional.ofNullable(paraphraseExampleMapper.selectById(sourceId))
                .orElseThrow(() -> new ResourceNotFoundException("Paraphrase example cannot be found!"))
                .getExampleTranslate();
        }
        throw new DataCheckedException("Chinese text cannot be found!");
    }

    private WordReviewDailyCounterDO findReviewCounterDO(int userId, int type) {
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


    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    private void delete(Integer id) {
        reviewAudioMapper.deleteById(id);
    }

    private void firstRecordReviewPageNumber(int listId, Long pageNumber, int type, Integer userId) {
        WordBreakpointReviewDO breakpointReviewDO = new WordBreakpointReviewDO();
        breakpointReviewDO.setId(0).setLastPage(pageNumber.intValue()).setOperateTime(LocalDateTime.now()).setType(type)
            .setUserId(userId).setListId(listId);
        breakpointReviewMapper.insert(breakpointReviewDO);
    }

    private static final Object BARRIER = new Object();
    private static final Object BARRIER_FOR_DAYS = new Object();

}
