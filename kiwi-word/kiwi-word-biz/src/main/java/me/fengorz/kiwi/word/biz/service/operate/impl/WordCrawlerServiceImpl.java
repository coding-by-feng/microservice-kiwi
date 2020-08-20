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
import me.fengorz.kiwi.common.sdk.util.lang.collection.KiwiCollectionUtils;
import me.fengorz.kiwi.common.sdk.util.lang.string.KiwiStringUtils;
import me.fengorz.kiwi.word.api.common.WordCrawlerConstants;
import me.fengorz.kiwi.word.api.dto.queue.fetch.*;
import me.fengorz.kiwi.word.api.entity.*;
import me.fengorz.kiwi.word.biz.service.base.*;
import me.fengorz.kiwi.word.biz.service.operate.IWordCrawlerService;
import me.fengorz.kiwi.word.biz.service.operate.IWordOperateService;
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
public class WordCrawlerServiceImpl implements IWordCrawlerService {

    private final IWordMainService wordMainService;
    private final IWordCharacterService wordCharacterService;
    private final IWordParaphraseService wordParaphraseService;
    private final IWordParaphraseExampleService wordParaphraseExampleService;
    private final IWordPronunciationService wordPronunciationService;
    private final IWordFetchQueueService wordFetchQueueService;
    private final IWordParaphrasePhraseService wordParaphrasePhraseService;
    private final IDfsService dfsService;
    private final ISeqService seqService;
    private final IWordOperateService wordOperateService;

    @Value("${me.fengorz.file.crawler.voice.tmpPath}")
    private String crawlerVoiceBasePath;

    /**
     * 存储单词数据逻辑不关注如果清除数据，如果有老数据未被清除，那么队列抓取记录暂时锁住，等到老数据清除干净再开始爬虫
     * 
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
        wordMainService.save(wordMainDO);
        this.subStoreFetchWordResult(dto, wordMainDO);
        // wordFetchQueueService.flagFetchBaseFinish(dto.getQueueId(), wordMainDO.getWordId());
        wordOperateService.cachePutFetchReplace(wordName,
            wordOperateService.cacheGetFetchReplace(wordName).setNewRelWordId(wordMainDO.getWordId()));
        wordOperateService.fetchReplaceCallBack(wordName);
        return true;
    }

    private void subStoreFetchWordResult(FetchWordResultDTO fetchWordResultDTO, WordMainDO wordMainDO) {
        final Integer wordId = wordMainDO.getWordId();
        List<FetchWordCodeDTO> fetchWordCodeDTOList = fetchWordResultDTO.getFetchWordCodeDTOList();
        if (CollUtil.isNotEmpty(fetchWordCodeDTOList)) {
            for (FetchWordCodeDTO fetchWordCodeDTO : fetchWordCodeDTOList) {
                WordCharacterDO wordCharacter =
                    WordBizUtils.initWordCharacter(fetchWordCodeDTO.getCode(), fetchWordCodeDTO.getLabel(), wordId);
                wordCharacter.setCharacterId(seqService.genIntSequence(MapperConstant.T_INS_SEQUENCE));
                wordCharacterService.save(wordCharacter);
                Integer characterId = wordCharacter.getCharacterId();

                List<FetchParaphraseDTO> fetchParaphraseDTOList = fetchWordCodeDTO.getFetchParaphraseDTOList();
                FetchWordReplaceDTO replaceDTO = wordOperateService.cacheGetFetchReplace(wordMainDO.getWordName());
                fetchParaphraseDTOList.forEach(fetchParaphraseDTO -> {

                    WordParaphraseDO wordParaphraseDO =
                        WordBizUtils.initWordParaphrase(characterId, wordId, fetchParaphraseDTO.getMeaningChinese(),
                            fetchParaphraseDTO.getParaphraseEnglish(), fetchParaphraseDTO.getTranslateLanguage());
                    wordParaphraseDO.setParaphraseId(seqService.genIntSequence(MapperConstant.T_INS_SEQUENCE));
                    wordParaphraseService.save(wordParaphraseDO);
                    Integer paraphraseId = wordParaphraseDO.getParaphraseId();
                    replaceDTO.getNewParaphraseIdMap().put(wordParaphraseDO.getParaphraseEnglish(), paraphraseId);
                    List<FetchPhraseDTO> fetchPhraseDTOList = fetchParaphraseDTO.getFetchPhraseDTOList();
                    if (KiwiCollectionUtils.isNotEmpty(fetchPhraseDTOList)) {
                        for (FetchPhraseDTO fetchPhraseDTO : fetchPhraseDTOList) {
                            WordParaphrasePhraseDO phraseDO = new WordParaphrasePhraseDO();
                            phraseDO.setId(seqService.genIntSequence(MapperConstant.T_INS_SEQUENCE));
                            phraseDO.setParaphraseId(paraphraseId);
                            phraseDO.setPhrase(fetchPhraseDTO.getPhrase());
                            phraseDO.setIsValid(CommonConstants.FLAG_YES);
                            phraseDO.setCreateTime(LocalDateTime.now());
                            wordParaphrasePhraseService.save(phraseDO);
                            wordParaphraseDO.setIsHavePhrase(CommonConstants.FLAG_YES);
                            wordParaphraseService.updateById(wordParaphraseDO);
                        }
                    }

                    Optional.ofNullable(fetchParaphraseDTO.getFetchParaphraseExampleDTOList())
                        .ifPresent(list -> list.forEach(fetchParaphraseExampleDTO -> {
                            WordParaphraseExampleDO wordParaphraseExampleDO = WordBizUtils.initWordParaphraseExample(
                                paraphraseId, wordId, fetchParaphraseExampleDTO.getExampleSentence(),
                                fetchParaphraseExampleDTO.getExampleTranslate(),
                                fetchParaphraseExampleDTO.getTranslateLanguage());
                            wordParaphraseExampleDO
                                .setExampleId(seqService.genIntSequence(MapperConstant.T_INS_SEQUENCE));
                            wordParaphraseExampleService.save(wordParaphraseExampleDO);
                            replaceDTO.getNewExampleIdMap().put(wordParaphraseExampleDO.getExampleSentence(),
                                wordParaphraseExampleDO.getExampleId());
                        }));
                });
                wordOperateService.cachePutFetchReplace(wordMainDO.getWordName(), replaceDTO);

                // save pronunciation and voice's file
                List<FetchWordPronunciationDTO> fetchWordPronunciationDTOList =
                    fetchWordCodeDTO.getFetchWordPronunciationDTOList();
                if (CollUtil.isNotEmpty(fetchWordPronunciationDTOList)) {
                    for (FetchWordPronunciationDTO fetchWordPronunciationDTO : fetchWordPronunciationDTOList) {
                        WordPronunciationDO wordPronunciation = WordBizUtils.initPronunciation(wordId, characterId,
                            fetchWordPronunciationDTO.getVoiceFileUrl(), fetchWordPronunciationDTO.getSoundmark(),
                            fetchWordPronunciationDTO.getSoundmarkType());
                        wordPronunciation.setPronunciationId(seqService.genIntSequence(MapperConstant.T_INS_SEQUENCE));
                        wordPronunciationService.save(wordPronunciation);
                    }
                }
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean fetchPronunciation(Integer wordId) {
        Objects
            .requireNonNull(wordPronunciationService
                .list(Wrappers.<WordPronunciationDO>lambdaQuery().eq(WordPronunciationDO::getWordId, wordId)))
            .forEach(this::fetchPronunciationVoice);
        return true;
    }

    private void fetchPronunciationVoice(WordPronunciationDO pronunciation) {
        String voiceUrl = pronunciation.getVoiceFilePath();
        String voiceFileUrl = WordCrawlerConstants.CAMBRIDGE_BASE_URL + voiceUrl;
        long voiceSize = HttpUtil.downloadFile(URLUtil.decode(voiceFileUrl), FileUtil.file(crawlerVoiceBasePath));
        String tempVoice = crawlerVoiceBasePath + WordDfsUtils.getVoiceFileName(voiceFileUrl);
        try {
            String uploadResult =
                dfsService.uploadFile(FileUtil.getInputStream(tempVoice), voiceSize, WordCrawlerConstants.EXT_OGG);
            pronunciation.setGroupName(WordDfsUtils.getGroupName(uploadResult));
            pronunciation.setVoiceFilePath(WordDfsUtils.getUploadVoiceFilePath(uploadResult));
            wordPronunciationService.updateById(pronunciation);
        } catch (DfsOperateException e) {
            throw new ServiceException(
                KiwiStringUtils.format("fetchPronunciationVoice error, pronunciation.url={}", voiceUrl));
        }
    }
}
