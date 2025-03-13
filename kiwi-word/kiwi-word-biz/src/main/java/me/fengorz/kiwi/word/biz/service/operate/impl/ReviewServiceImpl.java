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

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.db.service.SeqService;
import me.fengorz.kiwi.common.dfs.DfsService;
import me.fengorz.kiwi.common.dfs.DfsUtils;
import me.fengorz.kiwi.common.sdk.annotation.cache.KiwiCacheKey;
import me.fengorz.kiwi.common.sdk.annotation.cache.KiwiCacheKeyPrefix;
import me.fengorz.kiwi.common.sdk.annotation.log.LogMarker;
import me.fengorz.kiwi.common.sdk.constant.CacheConstants;
import me.fengorz.kiwi.common.sdk.constant.GlobalConstants;
import me.fengorz.kiwi.common.sdk.exception.AuthException;
import me.fengorz.kiwi.common.sdk.exception.DataCheckedException;
import me.fengorz.kiwi.common.sdk.exception.ResourceNotFoundException;
import me.fengorz.kiwi.common.sdk.exception.dfs.DfsOperateDeleteException;
import me.fengorz.kiwi.common.sdk.exception.dfs.DfsOperateException;
import me.fengorz.kiwi.common.sdk.exception.tts.TtsException;
import me.fengorz.kiwi.common.sdk.util.bean.KiwiBeanUtils;
import me.fengorz.kiwi.common.sdk.util.lang.array.KiwiArrayUtils;
import me.fengorz.kiwi.common.sdk.util.lang.collection.KiwiCollectionUtils;
import me.fengorz.kiwi.common.sdk.util.validate.KiwiAssertUtils;
import me.fengorz.kiwi.common.sdk.web.security.SecurityUtils;
import me.fengorz.kiwi.common.tts.enumeration.TtsSourceEnum;
import me.fengorz.kiwi.common.tts.service.TtsService;
import me.fengorz.kiwi.word.api.common.ApiCrawlerConstants;
import me.fengorz.kiwi.word.api.common.WordConstants;
import me.fengorz.kiwi.word.api.common.enumeration.ReviseAudioTypeEnum;
import me.fengorz.kiwi.word.api.common.enumeration.ReviseDailyCounterTypeEnum;
import me.fengorz.kiwi.word.api.common.enumeration.RevisePermanentAudioEnum;
import me.fengorz.kiwi.word.api.entity.*;
import me.fengorz.kiwi.word.api.model.ParaphraseTtsGenerationPayload;
import me.fengorz.kiwi.word.api.vo.WordReviewDailyCounterVO;
import me.fengorz.kiwi.word.biz.mapper.*;
import me.fengorz.kiwi.word.biz.service.base.ReviewAudioGenerationService;
import me.fengorz.kiwi.word.biz.service.base.ReviewAudioService;
import me.fengorz.kiwi.word.biz.service.initialing.RevisePermanentAudioHelper;
import me.fengorz.kiwi.word.biz.service.operate.AudioService;
import me.fengorz.kiwi.word.biz.service.operate.ReviewService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

/**
 * 复习功能服务类
 *
 * @author zhanShiFeng
 * @date 2021-06-06 14:53:44
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final BreakpointReviewMapper breakpointReviewMapper;
    private final SeqService seqService;
    private final ReviewDailyCounterMapper reviewDailyCounterMapper;
    private final ReviewAudioService reviewAudioService;
    private final ReviewAudioGenerationService reviewAudioGenerationService;
    private final ParaphraseMapper paraphraseMapper;
    private final ParaphraseExampleMapper paraphraseExampleMapper;
    private final WordMainMapper wordMainMapper;
    private final CharacterMapper characterMapper;
    private final ParaphraseStarRelMapper paraphraseStarRelMapper;
    @Resource(name = "googleCloudStorageService")
    private DfsService dfsService;
    private final AudioService audioService;
    @Resource(name = "voiceRssTtsService")
    private TtsService voiceRssTtsService;
    private final ParaphraseTtsGenerationPayload paraphraseTtsGenerationPayload;
    private final RevisePermanentAudioHelper revisePermanentAudioHelper;

    @Deprecated
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
        WordBreakpointReviewDO reviewDO = new WordBreakpointReviewDO().setId(seqService.genCommonIntSequence())
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
            for (ReviseDailyCounterTypeEnum typeEnum : ReviseDailyCounterTypeEnum.values()) {
                if (findReviewCounterDO(userId, typeEnum.getType()) == null) {
                    createDO(typeEnum.getType(), userId);
                    log.info("userId[{}] ReviewDailyCounterType[{}] is lacking， created", userId, typeEnum.name());
                } else {
                    log.info("userId[{}] ReviewDailyCounterType[{}] is created.", userId, typeEnum.name());
                }
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public void increase(int type, Integer userId) {
        if (ReviseDailyCounterTypeEnum.REVIEW_AUDIO_VOICERSS_TTS_COUNTER.getType() == type) {
            synchronized (BARRIER) {
                voiceRssTtsService.voiceRssGlobalIncreaseCounter();
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
    public WordReviewAudioDO generateWordReviewAudio(Integer sourceId, Integer type)
            throws DfsOperateException, TtsException, DataCheckedException {
        ReviseAudioTypeEnum typeEnum = ReviseAudioTypeEnum.fromValue(type);
        TtsSourceEnum sourceEnum = paraphraseTtsGenerationPayload.getFromReviewAudioTypeEnum(typeEnum);
        return generateUseTts(sourceId, typeEnum, sourceEnum);
    }

    private void generateWordReviewAudio(Integer sourceId, ReviseAudioTypeEnum typeEnum)
            throws DfsOperateException, TtsException, DataCheckedException {
        TtsSourceEnum sourceEnum = paraphraseTtsGenerationPayload.getFromReviewAudioTypeEnum(typeEnum);
        generateUseTts(sourceId, typeEnum, sourceEnum);
    }

    @Override
    @LogMarker(isPrintParameter = true, isPrintReturnValue = true)
    @KiwiCacheKeyPrefix(WordConstants.CACHE_KEY_PREFIX_REVIEW.METHOD_REVIEW_AUDIO)
    @Cacheable(cacheNames = WordConstants.CACHE_NAMES, keyGenerator = CacheConstants.CACHE_KEY_GENERATOR_BEAN,
            unless = "#result == null")
    public WordReviewAudioDO findWordReviewAudio(@KiwiCacheKey(1) Integer sourceId, @KiwiCacheKey(2) Integer type)
            throws DfsOperateException, TtsException, DataCheckedException {
        List<WordReviewAudioDO> wordReviewAudioList = reviewAudioService.list(Wrappers.<WordReviewAudioDO>lambdaQuery()
                .eq(WordReviewAudioDO::getSourceId, sourceId).eq(WordReviewAudioDO::getType, type));
        WordReviewAudioDO wordReviewAudioDO;
        if (wordReviewAudioList.size() != 1) {
            if (wordReviewAudioList.size() > 1) {
                removeWordReviewAudio(sourceId);
            }
            ReviseAudioTypeEnum typeEnum = ReviseAudioTypeEnum.fromValue(type);
            wordReviewAudioDO = generateWordReviewAudio(sourceId, type);
        } else {
            wordReviewAudioDO = wordReviewAudioList.get(0);
        }
        log.info("wordReviewAudioDO be found, {}", wordReviewAudioDO);
        return wordReviewAudioDO;
    }

    @Override
    public WordReviewAudioDO findCharacterReviewAudio(String characterCode) {
        return revisePermanentAudioHelper.getCacheStoreWithStringKey().get(characterCode);
    }

    @Override
    public void removeWordReviewAudio(Integer sourceId) {
        reviewAudioService.cleanBySourceId(sourceId);
    }

    @Override
    @KiwiCacheKeyPrefix(WordConstants.CACHE_KEY_PREFIX_REVIEW.METHOD_REVIEW_AUDIO)
    @CacheEvict(cacheNames = WordConstants.CACHE_NAMES, keyGenerator = CacheConstants.CACHE_KEY_GENERATOR_BEAN)
    public void evictWordReviewAudio(@KiwiCacheKey(1) Integer sourceId, @KiwiCacheKey(2) Integer type) {
    }

    @Override
    public void reGenReviewAudioForParaphrase(Integer sourceId) {
        removeWordReviewAudio(sourceId);
        generateTtsVoiceFromParaphraseId(sourceId);
    }

    @Override
    public void reGenReviewAudioForExample(Integer sourceId) throws DfsOperateException, TtsException, DataCheckedException {
        removeWordReviewAudio(sourceId);
        generateUseTts(sourceId, ReviseAudioTypeEnum.EXAMPLE_CH, TtsSourceEnum.BAIDU);
    }

    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    private WordReviewAudioDO generateUseTts(Integer sourceId, ReviseAudioTypeEnum type, TtsSourceEnum sourceType)
            throws DfsOperateException, TtsException, DataCheckedException {
        this.evictWordReviewAudio(sourceId, type.getType());
        WordReviewAudioDO wordReviewAudioDO;
        List<WordReviewAudioDO> list = reviewAudioService.list(sourceId, type.getType());
        Boolean isReplace = paraphraseTtsGenerationPayload.getIsReplace(type);
        log.info("{} isReplace={}", type.name(), isReplace);
        if (CollectionUtils.isEmpty(list)) {
            wordReviewAudioDO = new WordReviewAudioDO();
        } else if (isReplace || list.size() > 1) {
            log.warn("Method replaceWordReviewAudioDO is invoking, sourceId={}, type={}", sourceId, type.name());
            wordReviewAudioDO = replaceWordReviewAudioDO(list);
        } else {
            wordReviewAudioDO = list.get(0);
            log.info("The wordReviewAudioDO has been found, skip generation");
            return wordReviewAudioDO;
        }
        try {
            String text = acquireText(sourceId, type.getType());
            String uploadResult = null;
            if (TtsSourceEnum.VOICERSS.equals(sourceType)) {
                uploadResult = audioService.generateVoice(text, type.getType());
            } else if (TtsSourceEnum.BAIDU.equals(sourceType)) {
                uploadResult = audioService.generateVoiceUseBaiduTts(text);
            }
            KiwiAssertUtils.assertNotEmpty(uploadResult, "uploadResult must not be empty");
            wordReviewAudioDO.setId(seqService.genCommonIntSequence());
            wordReviewAudioDO.setGroupName(DfsUtils.getGroupName(uploadResult));
            wordReviewAudioDO.setFilePath(DfsUtils.getUploadVoiceFilePath(uploadResult));
            wordReviewAudioDO.setSourceId(sourceId);
            wordReviewAudioDO.setType(type.getType());
            wordReviewAudioDO.setIsDel(GlobalConstants.FLAG_DEL_NO);
            wordReviewAudioDO.setCreateTime(LocalDateTime.now());
            wordReviewAudioDO.setSourceUrl(sourceType.getSource());
            wordReviewAudioDO.setSourceText(StringUtils.defaultIfBlank(text, GlobalConstants.EMPTY));
            reviewAudioService.cleanAndInsert(wordReviewAudioDO);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw e;
        }

        reviewAudioGenerationService.markGenerateFinish(sourceId, wordReviewAudioDO.getId(), type);
        return wordReviewAudioDO;
    }

    private WordReviewAudioDO replaceWordReviewAudioDO(List<WordReviewAudioDO> wordReviewAudioList) {
        for (WordReviewAudioDO wordReviewAudioDO : wordReviewAudioList) {
            try {
                dfsService.deleteFile(wordReviewAudioDO.getGroupName(), wordReviewAudioDO.getFilePath());
            } catch (DfsOperateDeleteException e) {
                log.error("Error deleting old wordReviewAudioDO file", e);
            }
            reviewAudioService.cleanById(wordReviewAudioDO.getId());
        }
        return new WordReviewAudioDO();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void initPermanent(boolean isOnlyTest) throws DfsOperateException, TtsException {
        for (RevisePermanentAudioEnum audioEnum : RevisePermanentAudioEnum.values()) {
            if (isOnlyTest && !RevisePermanentAudioEnum.TEST.equals(audioEnum)) {
                continue;
            }
            if (!revisePermanentAudioHelper.getPermanentAudioEnums().contains(audioEnum)) {
                continue;
            }
            log.info("Audio is generating..., {}", audioEnum.getText());
            WordReviewAudioDO wordReviewAudioDO =
                    reviewAudioService.selectOne(audioEnum.getSourceId(), audioEnum.getType());
            Boolean isReplace = paraphraseTtsGenerationPayload.getIsReplace(audioEnum.getType());
            if (wordReviewAudioDO == null) {
                wordReviewAudioDO = new WordReviewAudioDO();
            } else if (isReplace) {
                try {
                    dfsService.deleteFile(wordReviewAudioDO.getGroupName(), wordReviewAudioDO.getFilePath());
                } catch (DfsOperateDeleteException e) {
                    log.error("Error deleting old wordReviewAudioDO", e);
                }
                reviewAudioService.cleanById(wordReviewAudioDO.getId());
                wordReviewAudioDO = new WordReviewAudioDO();
            } else {
                continue;
            }
            try {
                String uploadResult = null;
                TtsSourceEnum ttsSource = revisePermanentAudioHelper.queryTtsSource(audioEnum);
                if (TtsSourceEnum.VOICERSS.equals(ttsSource)) {
                    uploadResult = audioService.generateVoice(audioEnum.getText(), audioEnum.getType());
                } else if (TtsSourceEnum.BAIDU.equals(ttsSource)) {
                    uploadResult = audioService.generateVoiceUseBaiduTts(audioEnum.getText());
                }
                KiwiAssertUtils.assertNotEmpty(uploadResult, "uploadResult must not be empty");

                wordReviewAudioDO.setId(seqService.genCommonIntSequence());
                wordReviewAudioDO.setGroupName(DfsUtils.getGroupName(uploadResult));
                wordReviewAudioDO.setFilePath(DfsUtils.getUploadVoiceFilePath(uploadResult));
                wordReviewAudioDO.setSourceId(audioEnum.getSourceId());
                wordReviewAudioDO.setType(audioEnum.getType());
                wordReviewAudioDO.setIsDel(GlobalConstants.FLAG_DEL_NO);
                wordReviewAudioDO.setCreateTime(LocalDateTime.now());
                wordReviewAudioDO.setSourceUrl(ttsSource.getSource());
                wordReviewAudioDO.setSourceText(audioEnum.getText());
                reviewAudioService.cleanAndInsert(wordReviewAudioDO);
            } catch (TtsException | DfsOperateException e) {
                log.error(e.getMessage(), e);
                throw e;
            }
        }
    }

    @Override
    @Deprecated
    public void generateTtsVoice() throws InterruptedException {
        List<ParaphraseStarRelDO> relList = paraphraseStarRelMapper.selectList(Wrappers
                .<ParaphraseStarRelDO>lambdaQuery().eq(ParaphraseStarRelDO::getIsKeepInMind, GlobalConstants.FLAG_DEL_NO));
        // noinspection AlibabaThreadPoolCreation
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        for (ParaphraseStarRelDO starRelDO : relList) {
            STORAGE.acquire();
            executorService.submit(() -> {
                try {
                    log.info("executorService submit a task for generateTtsVoiceFromParaphraseId.");
                    generateTtsVoiceFromParaphraseId(starRelDO.getParaphraseId());
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
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public void cleanReviewVoiceByParaphraseId(Integer paraphraseId) {
        log.info("cleanReviewVoiceByParaphraseId paraphraseId = {}", paraphraseId);
        ListUtils.emptyIfNull(reviewAudioService
                        .list(Wrappers.<WordReviewAudioDO>lambdaQuery().eq(WordReviewAudioDO::getSourceId, paraphraseId)))
                .forEach(this::cleanReviewAudio);
        List<ParaphraseExampleDO> examples = paraphraseExampleMapper.selectList(
                Wrappers.<ParaphraseExampleDO>lambdaQuery().eq(ParaphraseExampleDO::getParaphraseId, paraphraseId));
        if (CollectionUtils.isEmpty(examples)) {
            return;
        }
        examples.forEach(paraphraseExampleDO -> ListUtils
                .emptyIfNull(reviewAudioService.list(Wrappers.<WordReviewAudioDO>lambdaQuery()
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
            reviewAudioService.cleanById(wordReviewAudioDO.getId());
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

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public void generateTtsVoiceFromParaphraseId(Integer paraphraseId) {
        log.info("generateTtsVoiceFromParaphraseId beginning, paraphraseId is {}", paraphraseId);
        final ParaphraseDO paraphraseDO = paraphraseMapper.selectById(paraphraseId);
        if (paraphraseDO == null) {
            log.warn("paraphraseDO is null, skip generateTtsVoiceFromParaphraseId, paraphraseId is {}", paraphraseId);
            cleanReviewVoiceByParaphraseId(paraphraseId);
            return;
        }
        final CharacterDO characterDO = characterMapper.selectOne(
                Wrappers.<CharacterDO>lambdaQuery().eq(CharacterDO::getCharacterId, paraphraseDO.getCharacterId()));
        final List<ParaphraseExampleDO> paraphraseExamples = paraphraseExampleMapper.selectList(
                Wrappers.<ParaphraseExampleDO>lambdaQuery().eq(ParaphraseExampleDO::getParaphraseId, paraphraseId));
        final Set<ReviseAudioTypeEnum> generatedTypes = new HashSet<>();
        paraphraseTtsGenerationPayload.getPairs().forEach(pair -> {
            try {
                ReviseAudioTypeEnum type = pair.getLeft();
                log.info("{} starting process.", type.name());
                if (generatedTypes.contains(type) || ReviseAudioTypeEnum.COMBO.equals(type)
                        || !paraphraseTtsGenerationPayload.getEnable(type)) {
                    log.info("Type({}) has generated, skipping processing.", type.name());
                    return;
                }
                Set<Integer> sourceIds =
                        buildSourceIds(paraphraseId, paraphraseDO, characterDO, paraphraseExamples, type);
                if (CollectionUtils.isEmpty(sourceIds)) {
                    log.info("sourceIds is empty, type={}, skipping generateWordReviewAudio()", type.name());
                    return;
                }
                for (Integer sourceId : sourceIds) {
                    if (sourceId == null) {
                        log.info("sourceId is null, type={}, skipping generateWordReviewAudio()", type.name());
                        continue;
                    }
                    log.info("sourceId={}, type={}, starting generateWordReviewAudio()", sourceId, type.name());
                    generateWordReviewAudio(sourceId, type);
                }
                generatedTypes.add(type);
            } catch (DfsOperateException | TtsException | DataCheckedException e) {
                log.error("generateWordReviewAudio invoke failed");
            }
        });

        this.generateComboFromParaphraseId(paraphraseId);
    }

    private Set<Integer> buildSourceIds(Integer paraphraseId, ParaphraseDO paraphraseDO, CharacterDO characterDO,
                                        List<ParaphraseExampleDO> paraphraseExamples, ReviseAudioTypeEnum type) {
        Set<Integer> sourceIds = new HashSet<>(paraphraseExamples.size());
        if (ReviseAudioTypeEnum.isWord(type.getType())) {
            log.info("Method buildSourceIds invoke success, source id from wordId.");
            sourceIds.add(paraphraseDO.getWordId());
        } else if (ReviseAudioTypeEnum.isParaphrase(type.getType())) {
            log.info("Method buildSourceIds invoke success, source id from paraphraseId.");
            sourceIds.add(paraphraseId);
        } else if (ReviseAudioTypeEnum.isExample(type.getType())) {
            for (ParaphraseExampleDO paraphraseExample : paraphraseExamples) {
                sourceIds.add(paraphraseExample.getExampleId());
            }
            log.info("Method buildSourceIds invoke success, source id from exampleIds.");
        } else if (ReviseAudioTypeEnum.isCharacter(type.getType())) {
            if (characterDO == null) {
                log.error("characterDO is null, skipping, characterDO is {}", paraphraseDO.getCharacterId());
                return sourceIds;
            }
            RevisePermanentAudioEnum revisePermanentAudioEnum =
                    revisePermanentAudioHelper.getPermanentAudioEnumMap().get(characterDO.getCharacterCode());
            if (revisePermanentAudioEnum != null) {
                sourceIds.add(revisePermanentAudioEnum.getSourceId());
                log.info("revisePermanentAudioEnum is exists, paraphraseId={}, characterId={},characterCode={}, type={}",
                        paraphraseId, characterDO.getCharacterId(), characterDO.getCharacterCode(), type.name());
            } else {
                log.info("revisePermanentAudioEnum is not exists, paraphraseId={}, characterId={}, characterCode={}, type={}",
                        paraphraseId, characterDO.getCharacterId(), characterDO.getCharacterCode(), type.name());
                reviewAudioGenerationService.markGenerateNotFinish(characterDO.getCharacterId(), 0,
                        ReviseAudioTypeEnum.CHARACTER_CH);
            }
        }
        return sourceIds;
    }

    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    private void generateComboFromParaphraseId(Integer paraphraseId) {
        if (!paraphraseTtsGenerationPayload.getEnable(ReviseAudioTypeEnum.COMBO)) {
            reviewAudioGenerationService.markGenerateFinish(paraphraseId, 0, ReviseAudioTypeEnum.COMBO);
            return;
        }
        WordReviewAudioDO wordReviewAudio;
        List<WordReviewAudioDO> list = reviewAudioService.list(paraphraseId, ReviseAudioTypeEnum.COMBO.getType());
        Boolean isReplace = paraphraseTtsGenerationPayload.getIsReplace(ReviseAudioTypeEnum.COMBO);
        if (CollectionUtils.isEmpty(list)) {
            wordReviewAudio = new WordReviewAudioDO();
        } else if (isReplace || list.size() > 1) {
            log.warn("Method replaceWordReviewAudioDO is invoking, paraphraseId={}, type={}", paraphraseId, ReviseAudioTypeEnum.COMBO.name());
            wordReviewAudio = replaceWordReviewAudioDO(list);
        } else {
            wordReviewAudio = list.get(0);
        }

        final ParaphraseDO paraphraseDO = paraphraseMapper.selectById(paraphraseId);
        if (paraphraseDO == null) {
            log.info("paraphraseDO is null, skipping generateComboFromParaphraseId, paraphraseId is {}", paraphraseId);
            return;
        }
        final CharacterDO characterDO = characterMapper.selectOne(
                Wrappers.<CharacterDO>lambdaQuery().eq(CharacterDO::getCharacterId, paraphraseDO.getCharacterId()));
        if (characterDO == null) {
            log.info("characterDO is null, skipping generateComboFromParaphraseId, characterDO is {}",
                    paraphraseDO.getCharacterId());
            return;
        }
        final List<ParaphraseExampleDO> paraphraseExamples = paraphraseExampleMapper.selectList(
                Wrappers.<ParaphraseExampleDO>lambdaQuery().eq(ParaphraseExampleDO::getParaphraseId, paraphraseId));
        int exampleIndex = 0;
        final List<byte[]> buffer = new ArrayList<>();
        for (ImmutablePair<ReviseAudioTypeEnum, Integer> counter : paraphraseTtsGenerationPayload.getCounters()) {
            ReviseAudioTypeEnum type = counter.getLeft();

            Set<Integer> sourceIds = buildSourceIds(paraphraseId, paraphraseDO, characterDO, paraphraseExamples, type);
            if (CollectionUtils.isEmpty(sourceIds)) {
                log.info("sourceIds is empty, type={},  skipping generate combo", type.name());
                continue;
            }
            sourceIds.forEach(sourceId -> {
                if (sourceId == null) {
                    log.info("sourceId is null, type={},  skipping generate combo", type.name());
                    return;
                }
                log.info("sourceId={}, type={}, starting generateWordReviewAudio()", sourceId, type.name());
                try {
                    WordReviewAudioDO perWordReviewAudio = findWordReviewAudio(sourceId, type.getType());
                    buffer.add(this.dfsService.downloadFile(perWordReviewAudio.getGroupName(),
                            perWordReviewAudio.getFilePath()));
                } catch (DfsOperateException | TtsException | DataCheckedException e) {
                    log.error(e.getMessage(), e);
                }
            });
        }

        byte[] mergedBytes = KiwiArrayUtils.mergeUseByteBuffer(buffer.toArray(new byte[buffer.size()][]));
        String uploadResult = null;
        try {
            uploadResult = this.dfsService.uploadFile(new ByteArrayInputStream(mergedBytes), mergedBytes.length,
                    ApiCrawlerConstants.EXT_MP3);
        } catch (DfsOperateException e) {
            log.error(e.getMessage(), e);
        }

        KiwiAssertUtils.assertNotEmpty(uploadResult, "uploadResult must not be empty");
        wordReviewAudio.setId(seqService.genCommonIntSequence());
        wordReviewAudio.setGroupName(DfsUtils.getGroupName(uploadResult));
        wordReviewAudio.setFilePath(DfsUtils.getUploadVoiceFilePath(uploadResult));
        wordReviewAudio.setSourceId(paraphraseId);
        wordReviewAudio.setType(ReviseAudioTypeEnum.COMBO.getType());
        wordReviewAudio.setIsDel(GlobalConstants.FLAG_DEL_NO);
        wordReviewAudio.setCreateTime(LocalDateTime.now());
        wordReviewAudio.setSourceUrl(TtsSourceEnum.COMBO.getSource());
        wordReviewAudio.setSourceText(StringUtils.defaultIfBlank(TtsSourceEnum.COMBO.name(), GlobalConstants.EMPTY));
        reviewAudioService.cleanAndInsert(wordReviewAudio);

        reviewAudioGenerationService.markGenerateFinish(paraphraseId, wordReviewAudio.getId(),
                ReviseAudioTypeEnum.COMBO);
    }

    private String acquireText(Integer sourceId, Integer type) throws DataCheckedException {
        if (ReviseAudioTypeEnum.isParaphrase(type)) {
            ParaphraseDO paraphraseDO = Optional.ofNullable(paraphraseMapper.selectById(sourceId)).orElseThrow(
                    () -> new ResourceNotFoundException(String.format("Paraphrase cannot be found, sourceId=%d, type=%d", sourceId, type)));
            if (ReviseAudioTypeEnum.isEnglish(type)) {
                return paraphraseDO.getParaphraseEnglish();
            } else if (ReviseAudioTypeEnum.isChinese(type)) {
                return StringUtils.defaultIfBlank(paraphraseDO.getMeaningChinese(),
                        RevisePermanentAudioEnum.WORD_PARAPHRASE_MISSING.getText());
            }
        } else if (ReviseAudioTypeEnum.isExample(type)) {
            ParaphraseExampleDO paraphraseExampleDO = Optional.ofNullable(paraphraseExampleMapper.selectById(sourceId))
                    .orElseThrow(() -> new ResourceNotFoundException("Example cannot be found, sourceId=%d, type=%d", sourceId, type));
            if (ReviseAudioTypeEnum.isEnglish(type)) {
                return paraphraseExampleDO.getExampleSentence();
            } else if (ReviseAudioTypeEnum.isChinese(type)) {
                return paraphraseExampleDO.getExampleTranslate();
            }
        } else if (ReviseAudioTypeEnum.isSpelling(type)) {
            WordMainDO wordMainDO = Optional.ofNullable(wordMainMapper.selectById(sourceId))
                    .orElseThrow(() -> new ResourceNotFoundException("Word cannot be found, sourceId=%d, type=%d", sourceId, type));
            StringBuilder sb = new StringBuilder();
            for (char alphabet : wordMainDO.getWordName().toCharArray()) {
                sb.append(StringUtils.upperCase(String.valueOf(alphabet))).append(GlobalConstants.SYMBOL_LF).append(GlobalConstants.SYMBOL_CH_PERIOD);
            }
            return sb.toString();
        } else if (ReviseAudioTypeEnum.isCharacter(type)) {
            if (RevisePermanentAudioEnum.isPermanent(sourceId)) {
                return RevisePermanentAudioEnum.fromSourceId(type).getText();
            }
            CharacterDO characterDO = Optional.ofNullable(characterMapper.selectById(sourceId))
                    .orElseThrow(() -> new ResourceNotFoundException("Character cannot be found, sourceId=%d, type=%d", sourceId, type));
            return RevisePermanentAudioEnum.WORD_CHARACTER.getText() + characterDO.getCharacterCode();
        } else if (ReviseAudioTypeEnum.isWord(type)) {
            WordMainDO wordMainDO = Optional.ofNullable(wordMainMapper.selectById(sourceId))
                    .orElseThrow(() -> new ResourceNotFoundException("Word cannot be found, sourceId=%d, type=%d", sourceId, type));
            return wordMainDO.getWordName();
        }
        throw new DataCheckedException("English text cannot be found, sourceId=%d, type=%d", sourceId, type);
    }

    private String acquireChineseText(Integer sourceId, Integer type) throws DataCheckedException {
        if (ReviseAudioTypeEnum.isParaphrase(type)) {
            return Optional.ofNullable(paraphraseMapper.selectById(sourceId))
                    .orElseThrow(() -> new ResourceNotFoundException("Paraphrase cannot be found!")).getMeaningChinese();
        } else if (ReviseAudioTypeEnum.isExample(type)) {
            return Optional.ofNullable(paraphraseExampleMapper.selectById(sourceId))
                    .orElseThrow(() -> new ResourceNotFoundException("Paraphrase example cannot be found!"))
                    .getExampleTranslate();
        }
        throw new DataCheckedException("Chinese text cannot be found, sourceId=%d, type=%d", sourceId, type);
    }

    private WordReviewDailyCounterDO findReviewCounterDO(int userId, int type) {
        LambdaQueryWrapper<WordReviewDailyCounterDO> wrapper = Wrappers.<WordReviewDailyCounterDO>lambdaQuery()
                .eq(WordReviewDailyCounterDO::getUserId, userId).eq(WordReviewDailyCounterDO::getType, type)
                .eq(WordReviewDailyCounterDO::getToday, LocalDateTime.now().toLocalDate());
        return reviewDailyCounterMapper.selectOne(wrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    private void createDO(int type, Integer userId) {
        if (Objects.nonNull(this.findReviewCounterDO(userId, type))) {
            return;
        }
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

    private static final Object BARRIER = new Object();
    private static final Object BARRIER_FOR_DAYS = new Object();

}
