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
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.fastdfs.service.IDfsService;
import me.fengorz.kiwi.common.sdk.util.lang.collection.KiwiCollectionUtils;
import me.fengorz.kiwi.common.sdk.util.lang.string.KiwiStringUtils;
import me.fengorz.kiwi.word.api.dto.queue.RemovePronunciatioinMqDTO;
import me.fengorz.kiwi.word.api.dto.queue.fetch.FetchWordReplaceDTO;
import me.fengorz.kiwi.word.api.entity.*;
import me.fengorz.kiwi.word.biz.service.base.*;
import me.fengorz.kiwi.word.biz.service.operate.IWordCleanerService;
import me.fengorz.kiwi.word.biz.service.operate.IWordOperateService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author zhanshifeng
 * @Date 2020/7/29 8:56 PM
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WordCleanerService implements IWordCleanerService {

    private final IWordOperateService operateService;
    private final IWordFetchQueueService queueService;
    private final IWordMainService wordMainService;
    private final IWordCharacterService characterService;
    private final IWordParaphraseService paraphraseService;
    private final IWordParaphraseExampleService exampleService;
    private final IWordPronunciationService pronunciationService;
    private final IWordMainVariantService variantService;
    private final IWordParaphrasePhraseService phraseService;
    private final IDfsService dfsService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<RemovePronunciatioinMqDTO> removeWord(String wordName, Integer queueId) {
        List<RemovePronunciatioinMqDTO> result = new ArrayList<>();
        List<WordMainDO> list =
                wordMainService.list(Wrappers.<WordMainDO>lambdaQuery().eq(WordMainDO::getWordName, wordName));
        if (KiwiCollectionUtils.isEmpty(list)) {
            Optional.ofNullable(variantService.listWordMain(wordName, queueId)).ifPresent(list::addAll);
        }

        if (KiwiCollectionUtils.isEmpty(list)) {
            return result;
        }

        for (WordMainDO wordMainDO : list) {
            this.evictAll(wordMainDO, wordName);
            List<RemovePronunciatioinMqDTO> dtoList = this.subRemoveWord(wordMainDO);
            dtoList.forEach(dto -> dto.setQueueId(queueId));
            KiwiCollectionUtils.addAllIfNotContains(result, dtoList);
        }
        return result;
    }

    @Override
    public List<RemovePronunciatioinMqDTO> removeWord(Integer queueId) {
        List<RemovePronunciatioinMqDTO> result = new LinkedList<>();
        Optional.ofNullable(queueService.getOneAnyhow(queueId)).ifPresent(queue -> {
            String wordName = queue.getWordName();
            String derivation = queue.getDerivation();
            List<WordMainDO> list = new LinkedList<>();
            if (KiwiStringUtils.equals(wordName, derivation)) {
                list.addAll(wordMainService.list(wordName));
            } else {
                // 如果所查单词和单词的原型不同的话
                list.addAll(wordMainService.list(derivation));
            }
            if (KiwiCollectionUtils.isEmpty(list)) {
                return;
            }

            for (WordMainDO wordMainDO : list) {
                this.evictAll(wordMainDO, wordName);
                List<RemovePronunciatioinMqDTO> temps = this.subRemoveWord(wordMainDO);
                temps.forEach(dto -> dto.setQueueId(queueId));
                KiwiCollectionUtils.addAllIfNotContains(result, temps);
            }
        });
        return result;
    }

    private List<RemovePronunciatioinMqDTO> subRemoveWord(WordMainDO wordMainDO) {
        final String wordName = wordMainDO.getWordName();
        wordMainService.remove(Wrappers.<WordMainDO>lambdaQuery().eq(WordMainDO::getWordName, wordName));
        variantService.remove(Wrappers.<WordMainVariantDO>lambdaQuery().eq(WordMainVariantDO::getVariantName, wordName));
        operateService.cacheReplace(wordName,
                operateService.cacheReplace(wordName).setOldRelWordId(wordMainDO.getWordId()));
        return this.removeRelatedData(wordMainDO);
    }

    private List<RemovePronunciatioinMqDTO> removeRelatedData(WordMainDO word) {
        Integer wordId = word.getWordId();

        variantService.delByWordId(wordId);
        List<WordCharacterDO> characterList = characterService.list(Wrappers.<WordCharacterDO>lambdaQuery().eq(WordCharacterDO::getWordId, wordId));
        if (KiwiCollectionUtils.isNotEmpty(characterList)) {
            for (WordCharacterDO character : characterList) {
                Integer characterId = character.getCharacterId();
                List<WordParaphraseDO> paraphraseList = paraphraseService.list(Wrappers.<WordParaphraseDO>lambdaQuery().eq(WordParaphraseDO::getCharacterId, characterId));
                if (CollUtil.isNotEmpty(paraphraseList)) {
                    for (WordParaphraseDO wordParaphraseDO : paraphraseList) {
                        Integer paraphraseId = wordParaphraseDO.getParaphraseId();
                        LambdaQueryWrapper<WordParaphraseExampleDO> exampleQueryWrapper =
                                Wrappers.<WordParaphraseExampleDO>lambdaQuery().eq(WordParaphraseExampleDO::getParaphraseId,
                                        paraphraseId);
                        List<WordParaphraseExampleDO> exampleList =
                                exampleService.list(exampleQueryWrapper);
                        if (KiwiCollectionUtils.isNotEmpty(exampleList)) {
                            for (WordParaphraseExampleDO example : exampleList) {
                                // 将已删除的老的exampleId缓存起来，这样可以替换掉收藏本的关联id
                                FetchWordReplaceDTO replaceDTO =
                                        operateService.cacheReplace(word.getWordName());
                                Map<String, Integer> oldExampleIdMap = replaceDTO.getOldExampleIdMap();
                                oldExampleIdMap.put(example.getExampleSentence(),
                                        example.getExampleId());
                                operateService.cacheReplace(word.getWordName(), replaceDTO);
                            }
                            exampleService.remove(exampleQueryWrapper);
                        }

                        // 将已删除的老的paraphraseId缓存起来，这样可以替换掉收藏本的关联id
                        FetchWordReplaceDTO replaceDTO = operateService.cacheReplace(word.getWordName());
                        replaceDTO.getOldParaphraseIdMap().put(wordParaphraseDO.getParaphraseEnglish(), paraphraseId);
                        operateService.cacheReplace(word.getWordName(), replaceDTO);

                        phraseService.remove(Wrappers.<WordParaphrasePhraseDO>lambdaQuery()
                                .eq(WordParaphrasePhraseDO::getParaphraseId, paraphraseId));
                    }
                }
                if (CollUtil.isNotEmpty(paraphraseList)) {
                    paraphraseService.remove(Wrappers.<WordParaphraseDO>lambdaUpdate().eq(WordParaphraseDO::getCharacterId, characterId));
                }
                characterService.evict(characterId);
            }
            characterService.remove(Wrappers.<WordCharacterDO>lambdaUpdate().eq(WordCharacterDO::getWordId, wordId));
        }

        // 删除分布式文件系统里面的文件
        LambdaQueryWrapper<WordPronunciationDO> pronunciationWrapper =
                Wrappers.<WordPronunciationDO>lambdaQuery().eq(WordPronunciationDO::getWordId, wordId);
        List<WordPronunciationDO> wordPronunciationList = pronunciationService.list(pronunciationWrapper);
        pronunciationService.remove(pronunciationWrapper);

        return wordPronunciationList.stream()
                .filter(pronunciationDO -> StrUtil.isNotBlank(pronunciationDO.getVoiceFilePath()))
                .map(pronunciationDO -> new RemovePronunciatioinMqDTO().setGroupName(pronunciationDO.getGroupName())
                        .setVoiceFilePath(pronunciationDO.getVoiceFilePath()))
                .collect(Collectors.toList());
    }

    private void evictAll(WordMainDO wordMainDO, String wordName) {
        // 这里缓存的删除要在Mysql的删除之前做
        if (KiwiStringUtils.isNotEquals(wordName, wordMainDO.getWordName())) {
            operateService.evict(wordName, wordMainDO);
        } else {
            operateService.evict(wordMainDO.getWordName(), wordMainDO);
        }
        wordMainService.evictById(wordMainDO.getWordId());
    }

}
