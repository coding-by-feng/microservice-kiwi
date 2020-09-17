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

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.HttpUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.bdf.core.service.ISeqService;
import me.fengorz.kiwi.common.api.constant.CommonConstants;
import me.fengorz.kiwi.common.api.constant.MapperConstant;
import me.fengorz.kiwi.common.api.exception.ServiceException;
import me.fengorz.kiwi.common.api.exception.dfs.DfsOperateDeleteException;
import me.fengorz.kiwi.common.api.exception.dfs.DfsOperateException;
import me.fengorz.kiwi.common.fastdfs.service.IDfsService;
import me.fengorz.kiwi.common.sdk.util.bean.KiwiBeanUtils;
import me.fengorz.kiwi.common.sdk.util.lang.collection.KiwiCollectionUtils;
import me.fengorz.kiwi.common.sdk.util.lang.string.KiwiStringUtils;
import me.fengorz.kiwi.word.api.common.WordCrawlerConstants;
import me.fengorz.kiwi.word.api.dto.queue.fetch.*;
import me.fengorz.kiwi.word.api.entity.*;
import me.fengorz.kiwi.word.biz.service.base.*;
import me.fengorz.kiwi.word.biz.service.operate.ICrawlerService;
import me.fengorz.kiwi.word.biz.service.operate.IOperateService;
import me.fengorz.kiwi.word.biz.util.WordBizUtils;
import me.fengorz.kiwi.word.biz.util.WordDfsUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * @Description 爬虫服务
 * @Author zhanshifeng
 * @Date 2020/7/28 8:03 PM
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CrawlerServiceImpl implements ICrawlerService {

    private final IWordMainService mainService;
    private final ICharacterService characterService;
    private final IParaphraseService wordParaphraseService;
    private final IParaphraseExampleService exampleService;
    private final IPronunciationService pronunciationService;
    private final IWordFetchQueueService queueService;
    private final IParaphrasePhraseService phraseService;
    private final IDfsService dfsService;
    private final ISeqService seqService;
    private final IOperateService operateService;

    @Value("${me.fengorz.file.crawler.voice.tmpPath}")
    private String crawlerVoiceBasePath;

    /**
     * @param dto
     * @return
     * @throws DfsOperateException
     * @throws DfsOperateDeleteException
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean storeFetchWordResult(FetchWordResultDTO dto) {
        final String wordName = dto.getWordName();
        WordMainDO wordMainDO = new WordMainDO().setWordName(wordName)
                .setWordId(seqService.genIntSequence(MapperConstant.T_INS_SEQUENCE)).setIsDel(CommonConstants.FLAG_DEL_NO);
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
                    wordParaphraseService.save(paraphrase);

                    Integer paraphraseId = paraphrase.getParaphraseId();
                    Optional.ofNullable(paraphrase.getSerialNumber()).filter(serialNumber -> serialNumber > 0).ifPresent(serialNumber -> {
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
                            phrase.setIsValid(CommonConstants.FLAG_YES);
                            phrase.setCreateTime(LocalDateTime.now());
                            phraseService.save(phrase);
                            paraphrase.setIsHavePhrase(CommonConstants.FLAG_YES);
                            wordParaphraseService.updateById(paraphrase);
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

                                Optional.ofNullable(example.getSerialNumber()).filter(serialNumber -> serialNumber > 0).ifPresent(serialNumber -> {
                                    FetchWordReplaceDTO.Binder binder = replaceDTO.getExampleBinderMap().get(serialNumber);
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
                List<FetchWordPronunciationDTO> pronunciationDTOList =
                        codeDTO.getFetchWordPronunciationDTOList();
                if (CollUtil.isNotEmpty(pronunciationDTOList)) {
                    for (FetchWordPronunciationDTO pronunciationDTO : pronunciationDTOList) {
                        PronunciationDO pronunciation = WordBizUtils.initPronunciation(wordId, characterId,
                                pronunciationDTO.getVoiceFileUrl(), pronunciationDTO.getSoundmark(),
                                pronunciationDTO.getSoundmarkType());
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

    private void fetchPronunciationVoice(PronunciationDO pronunciation) {
        String voiceUrl = pronunciation.getVoiceFilePath();
        // 如果音标资源链接为空，可能是爬虫没有抓到，那就放弃当前音标资源
        if (KiwiStringUtils.isBlank(voiceUrl)) {
            return;
        }
        String voiceFileUrl = WordCrawlerConstants.URL_CAMBRIDGE_BASE + voiceUrl;
        long voiceSize = HttpUtil.downloadFile(URLUtil.decode(voiceFileUrl), FileUtil.file(crawlerVoiceBasePath));
        String tempVoice = crawlerVoiceBasePath + WordDfsUtils.getVoiceFileName(voiceFileUrl);
        try {
            String uploadResult =
                    dfsService.uploadFile(FileUtil.getInputStream(tempVoice), voiceSize, WordCrawlerConstants.EXT_OGG);
            pronunciation.setGroupName(WordDfsUtils.getGroupName(uploadResult));
            pronunciation.setVoiceFilePath(WordDfsUtils.getUploadVoiceFilePath(uploadResult));
            pronunciationService.updateById(pronunciation);
        } catch (DfsOperateException e) {
            throw new ServiceException(
                    KiwiStringUtils.format("fetchPronunciationVoice error, pronunciation.url={}", voiceUrl));
        }
    }
}
