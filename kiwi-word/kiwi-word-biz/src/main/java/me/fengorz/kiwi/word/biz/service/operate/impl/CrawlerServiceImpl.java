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
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.HttpUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.bdf.core.service.SeqService;
import me.fengorz.kiwi.common.fastdfs.service.DfsService;
import me.fengorz.kiwi.common.sdk.annotation.log.LogMarker;
import me.fengorz.kiwi.common.sdk.constant.GlobalConstants;
import me.fengorz.kiwi.common.sdk.constant.MapperConstant;
import me.fengorz.kiwi.common.sdk.exception.DataCheckedException;
import me.fengorz.kiwi.common.sdk.exception.ResourceNotFoundException;
import me.fengorz.kiwi.common.sdk.exception.ServiceException;
import me.fengorz.kiwi.common.sdk.exception.dfs.DfsOperateDeleteException;
import me.fengorz.kiwi.common.sdk.exception.dfs.DfsOperateException;
import me.fengorz.kiwi.common.sdk.exception.tts.TtsException;
import me.fengorz.kiwi.common.sdk.util.bean.KiwiBeanUtils;
import me.fengorz.kiwi.common.sdk.util.lang.collection.KiwiCollectionUtils;
import me.fengorz.kiwi.common.sdk.util.lang.string.KiwiStringUtils;
import me.fengorz.kiwi.common.tts.service.TtsService;
import me.fengorz.kiwi.word.api.common.ApiCrawlerConstants;
import me.fengorz.kiwi.word.api.common.enumeration.ReviewAudioTypeEnum;
import me.fengorz.kiwi.word.api.dto.queue.result.*;
import me.fengorz.kiwi.word.api.entity.*;
import me.fengorz.kiwi.word.api.vo.ParaphraseExampleVO;
import me.fengorz.kiwi.word.biz.service.base.*;
import me.fengorz.kiwi.word.biz.service.operate.CrawlerService;
import me.fengorz.kiwi.word.biz.service.operate.OperateService;
import me.fengorz.kiwi.word.biz.service.operate.ReviewService;
import me.fengorz.kiwi.word.biz.util.WordBizUtils;
import me.fengorz.kiwi.word.biz.util.WordDfsUtils;

/**
 * @Description 爬虫服务 @Author zhanshifeng @Date 2020/7/28 8:03 PM
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CrawlerServiceImpl implements CrawlerService {

    private final WordMainService mainService;
    private final CharacterService characterService;
    private final ParaphraseService paraphraseService;
    private final ParaphraseExampleService exampleService;
    private final PronunciationService pronunciationService;
    private final WordFetchQueueService queueService;
    private final ParaphrasePhraseService phraseService;
    private final ParaphraseStarRelService paraphraseStarRelService;
    private final DfsService dfsService;
    private final SeqService seqService;
    private final OperateService operateService;
    private final TtsService ttsService;
    private final ReviewService reviewService;

    @Value("${me.fengorz.file.crawler.voice.tmpPath:'/wordTmp'}")
    private String crawlerVoiceBasePath;

    /**
     * @param dto
     * @return
     * @throws DfsOperateException
     * @throws DfsOperateDeleteException
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogMarker(isPrintParameter = true, isPrintExecutionTime = true, isPrintReturnValue = true)
    public boolean storeFetchWordResult(FetchWordResultDTO dto) {
        final String wordName = dto.getWordName();
        WordMainDO old = mainService.getOneAndCatch(wordName);
        if (old != null) {
            queueService.flagFetchBaseFinish(dto.getQueueId(), old.getWordId());
            return true;
        }

        WordMainDO wordMainDO = new WordMainDO().setWordName(wordName)
            .setWordId(seqService.genIntSequence(MapperConstant.T_INS_SEQUENCE)).setIsDel(GlobalConstants.FLAG_DEL_NO);
        mainService.save(wordMainDO);
        this.subStoreFetchWordResult(dto, wordMainDO);
        queueService.flagFetchBaseFinish(dto.getQueueId(), wordMainDO.getWordId());
        operateService.cacheReplace(wordName,
            operateService.getCacheReplace(wordName).setNewRelWordId(wordMainDO.getWordId()));
        operateService.fetchReplaceCallBack(wordName);
        return true;
    }

    private void subStoreFetchWordResult(FetchWordResultDTO fetchDTO, WordMainDO word) {
        final Integer wordId = word.getWordId();
        List<FetchWordCodeDTO> codeDTOList = fetchDTO.getFetchWordCodeDTOList();
        if (CollUtil.isNotEmpty(codeDTOList)) {
            for (FetchWordCodeDTO codeDTO : codeDTOList) {
                CharacterDO character = new CharacterDO();
                KiwiBeanUtils.copyProperties(codeDTO, character);
                character.setWordId(wordId);
                character.setCharacterId(seqService.genIntSequence(MapperConstant.T_INS_SEQUENCE));
                characterService.save(character);
                Integer characterId = character.getCharacterId();

                List<FetchParaphraseDTO> paraphraseDTOList = codeDTO.getFetchParaphraseDTOList();
                FetchWordReplaceDTO replaceDTO = operateService.getCacheReplace(word.getWordName());
                paraphraseDTOList.forEach(paraphraseDTO -> {
                    ParaphraseDO paraphrase = new ParaphraseDO();
                    KiwiBeanUtils.copyProperties(paraphraseDTO, paraphrase);
                    paraphrase.setCharacterId(characterId).setWordId(wordId);
                    paraphrase.setParaphraseId(seqService.genIntSequence(MapperConstant.T_INS_SEQUENCE));
                    paraphraseService.save(paraphrase);

                    Integer paraphraseId = paraphrase.getParaphraseId();
                    Optional.ofNullable(paraphrase.getSerialNumber()).filter(serialNumber -> serialNumber > 0)
                        .ifPresent(serialNumber -> {
                            FetchWordReplaceDTO.Binder binder = replaceDTO.getParaphraseBinderMap().get(serialNumber);
                            if (binder == null) {
                                return;
                            }
                            binder.setNewId(paraphraseId);
                            replaceDTO.getParaphraseBinderMap().put(serialNumber, binder);
                        });

                    List<FetchPhraseDTO> phraseDTOList = paraphraseDTO.getPhraseDTOList();
                    if (KiwiCollectionUtils.isNotEmpty(phraseDTOList)) {
                        for (FetchPhraseDTO fetchPhraseDTO : phraseDTOList) {
                            ParaphrasePhraseDO phrase = new ParaphrasePhraseDO();
                            phrase.setId(seqService.genIntSequence(MapperConstant.T_INS_SEQUENCE));
                            phrase.setParaphraseId(paraphraseId);
                            phrase.setPhrase(fetchPhraseDTO.getPhrase());
                            phrase.setIsValid(GlobalConstants.FLAG_YES);
                            phrase.setCreateTime(LocalDateTime.now());
                            phraseService.save(phrase);
                            paraphrase.setIsHavePhrase(GlobalConstants.FLAG_YES);
                            paraphraseService.updateById(paraphrase);
                        }
                    }

                    Optional.ofNullable(paraphraseDTO.getExampleDTOList())
                        .ifPresent(list -> list.forEach(exampleDTO -> {
                            ParaphraseExampleDO example = new ParaphraseExampleDO();
                            KiwiBeanUtils.copyProperties(exampleDTO, example);
                            example.setWordId(wordId);
                            example.setParaphraseId(paraphraseId);
                            example.setExampleId(seqService.genIntSequence(MapperConstant.T_INS_SEQUENCE));
                            exampleService.save(example);

                            Optional.ofNullable(example.getSerialNumber()).filter(serialNumber -> serialNumber > 0)
                                .ifPresent(serialNumber -> {
                                    FetchWordReplaceDTO.Binder binder =
                                        replaceDTO.getExampleBinderMap().get(serialNumber);
                                    if (binder == null) {
                                        return;
                                    }
                                    binder.setNewId(example.getExampleId());
                                    replaceDTO.getExampleBinderMap().put(serialNumber, binder);
                                });
                        }));
                });
                operateService.cacheReplace(word.getWordName(), replaceDTO);

                // save pronunciation and voice's file
                List<FetchWordPronunciationDTO> pronunciationDTOList = codeDTO.getFetchWordPronunciationDTOList();
                if (CollUtil.isNotEmpty(pronunciationDTOList)) {
                    for (FetchWordPronunciationDTO pronunciationDTO : pronunciationDTOList) {
                        PronunciationDO pronunciation =
                            WordBizUtils.initPronunciation(wordId, characterId, pronunciationDTO.getVoiceFileUrl(),
                                pronunciationDTO.getSoundmark(), pronunciationDTO.getSoundmarkType());
                        pronunciation.setPronunciationId(seqService.genIntSequence(MapperConstant.T_INS_SEQUENCE));
                        pronunciationService.save(pronunciation);
                    }
                }
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean fetchPronunciation(Integer wordId) {
        Objects
            .requireNonNull(pronunciationService
                .list(Wrappers.<PronunciationDO>lambdaQuery().eq(PronunciationDO::getWordId, wordId)))
            .forEach(this::fetchPronunciationVoice);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reFetchPronunciation(Integer pronunciationId) {
        PronunciationDO pronunciation = Optional.ofNullable(pronunciationService.getById(pronunciationId))
            .orElseThrow(ResourceNotFoundException::new);
        this.fetchPronunciation(pronunciation.getWordId());
    }

    @Override
    public boolean handlePhrasesFetchResult(FetchPhraseRunUpResultDTO dto) {
        for (String phrase : dto.getPhrases()) {
            // 词组是word本身跳过
            if (KiwiStringUtils.equals(phrase, dto.getWord())) {
                continue;
            }
            // 包含空格说明是词组
            if (KiwiStringUtils.containsBlank(phrase)) {
                queueService.startFetchPhrase(phrase, dto.getWord(), dto.getWordId());
            } else {
                // 单词队列表不存在插入新记录
                if (queueService.getOneAnyhow(phrase) == null) {
                    queueService.startFetch(phrase);
                }
            }
        }
        Optional.ofNullable(dto.getRelatedWords()).ifPresent(relatedWords -> {
            for (String relatedWord : relatedWords) {
                if (queueService.getOneAnyhow(relatedWord) == null) {
                    queueService.startForceFetchWord(relatedWord);
                }
            }
        });
        return true;
    }

    @Override
    public boolean storePhrasesFetchResult(FetchPhraseResultDTO dto) {
        // 把关联词组插入队列
        final Set<String> relatedWords = dto.getRelatedWords();
        if (KiwiCollectionUtils.isNotEmpty(relatedWords)) {
            for (String relatedWord : relatedWords) {
                queueService.startFetch(relatedWord);
            }
        }

        final List<FetchParaphraseDTO> paraphrases = dto.getFetchParaphraseDTOList();
        if (KiwiCollectionUtils.isEmpty(paraphrases)) {
            return false;
        }

        final String phrase = dto.getPhrase();
        WordMainDO wordMain = new WordMainDO().setWordId(seqService.genIntSequence(MapperConstant.T_INS_SEQUENCE))
            .setWordName(phrase).setInfoType(ApiCrawlerConstants.QUEUE_INFO_TYPE_PHRASE);
        mainService.save(wordMain);

        for (FetchParaphraseDTO paraphrase : paraphrases) {
            ParaphraseDO paraphraseDO = new ParaphraseDO();
            KiwiBeanUtils.copyProperties(paraphrase, paraphraseDO);
            paraphraseDO.setParaphraseId(seqService.genIntSequence(MapperConstant.T_INS_SEQUENCE));
            paraphraseDO.setWordId(wordMain.getWordId());
            paraphraseDO.setCharacterId(0);
            paraphraseDO.setSerialNumber(0);
            paraphraseService.save(paraphraseDO);
            Optional.ofNullable(paraphrase.getExampleDTOList()).ifPresent(examples -> {
                for (FetchParaphraseExampleDTO example : examples) {
                    ParaphraseExampleDO exampleDO = new ParaphraseExampleDO();
                    KiwiBeanUtils.copyProperties(example, exampleDO);
                    exampleDO.setExampleId(seqService.genIntSequence(MapperConstant.T_INS_SEQUENCE));
                    exampleDO.setParaphraseId(paraphraseDO.getParaphraseId());
                    exampleDO.setWordId(wordMain.getWordId());
                    exampleDO.setSerialNumber(0);
                    exampleService.save(exampleDO);
                }
            });
        }

        Optional.ofNullable(queueService.getOneAnyhow(dto.getQueueId())).ifPresent(queue -> {
            queue.setWordId(wordMain.getWordId());
            queueService.updateById(queue);
        });

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void generateTtsVoice() {
        try {
            log.info("Method generateTtsVoice is starting!");
            if (!ttsService.hasValidApiKey()) {
                log.info("There is not valid api key!");
                return;
            }
            if (GENERATE_VOICE_BARRIER.tryAcquire(1, 1, TimeUnit.SECONDS)) {
                List<Integer> notGeneratedParaphraseId = paraphraseStarRelService.listNotGeneratedVoice();
                if (CollectionUtils.isEmpty(notGeneratedParaphraseId)) {
                    log.info("There is not notGeneratedParaphraseId need to generate voice.");
                    notGeneratedParaphraseId = paraphraseService.listNotGeneratedAndNotCollectVoice();
                    if (CollectionUtils.isEmpty(notGeneratedParaphraseId)) {
                        log.info("There is not notGeneratedAndNotCollectVoice need to generate voice.");
                        GENERATE_VOICE_BARRIER.release();
                        return;
                    }
                }
                for (Integer id : notGeneratedParaphraseId) {
                    try {
                        log.info("Paraphrase id({}) generation is starting!", id);
                        ParaphraseDO paraphrase = paraphraseService.getById(id);
                        try {
                            reviewService.generateWordReviewAudio(true, id,
                                ReviewAudioTypeEnum.PARAPHRASE_EN.getType());
                            reviewService.generateWordReviewAudio(true, id,
                                ReviewAudioTypeEnum.PARAPHRASE_CH.getType());
                        } catch (DfsOperateException | TtsException | DataCheckedException e) {
                            log.error("generateWordReviewAudio exception, sourceId={}, {}", id, e.getMessage());
                            throw e;
                        }

                        List<ParaphraseExampleVO> examples = exampleService.listExamples(id);
                        if (CollectionUtils.isEmpty(examples)) {
                            continue;
                        }
                        for (ParaphraseExampleVO example : examples) {
                            try {
                                reviewService.generateWordReviewAudio(true, example.getExampleId(),
                                    ReviewAudioTypeEnum.EXAMPLE_EN.getType());
                                reviewService.generateWordReviewAudio(true, example.getExampleId(),
                                    ReviewAudioTypeEnum.EXAMPLE_CH.getType());
                            } catch (DfsOperateException | TtsException | DataCheckedException e) {
                                log.error("generateWordReviewAudio exception, sourceId={}, {}", example.getExampleId(),
                                    e.getMessage());
                                throw e;
                            }
                        }
                    } catch (Exception e) {
                        reviewService.cleanReviewVoiceByParaphraseId(id);
                        GENERATE_VOICE_BARRIER.release();
                        log.error(
                            "Paraphrase id({}) generation failed, Data has cleaned, GENERATE_VOICE_BARRIER has released",
                            id);
                        return;
                    }
                    log.info("Paraphrase id({}) generation is end!", id);
                }
            } else {
                log.info("Paraphrase is generating, GENERATE_VOICE_BARRIER tryAcquire false, skip work.");
            }
        } catch (InterruptedException e) {
            log.error("GENERATE_VOICE_BARRIER tryAcquire failed.");
            return;
        }
        GENERATE_VOICE_BARRIER.release();
    }

    private void fetchPronunciationVoice(PronunciationDO pronunciation) {
        String voiceUrl = pronunciation.getVoiceFilePath();
        // 如果音标资源链接为空，可能是爬虫没有抓到，那就放弃当前音标资源
        if (KiwiStringUtils.isBlank(voiceUrl)) {
            return;
        }
        String voiceFileUrl = ApiCrawlerConstants.URL_CAMBRIDGE_BASE + voiceUrl;
        log.info("Download {}.", voiceFileUrl);
        long voiceSize = HttpUtil.downloadFile(URLUtil.decode(voiceFileUrl), FileUtil.file(crawlerVoiceBasePath));
        String tempVoice = crawlerVoiceBasePath + WordDfsUtils.getVoiceFileName(voiceFileUrl);
        try {
            String uploadResult =
                dfsService.uploadFile(FileUtil.getInputStream(tempVoice), voiceSize, ApiCrawlerConstants.EXT_MP3);
            pronunciation.setGroupName(WordDfsUtils.getGroupName(uploadResult));
            pronunciation.setVoiceFilePath(WordDfsUtils.getUploadVoiceFilePath(uploadResult));
            pronunciationService.updateById(pronunciation);
            log.info("Pronunciation fetched success, uploadResult = {}.", uploadResult);
        } catch (DfsOperateException e) {
            throw new ServiceException(
                KiwiStringUtils.format("fetchPronunciationVoice error, pronunciation.url={}", voiceUrl));
        }
    }

    private static final Semaphore GENERATE_VOICE_BARRIER = new Semaphore(1);

}
