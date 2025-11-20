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
import me.fengorz.kiwi.common.dfs.DfsService;
import me.fengorz.kiwi.common.sdk.util.lang.collection.KiwiCollectionUtils;
import me.fengorz.kiwi.common.sdk.util.lang.string.KiwiStringUtils;
import me.fengorz.kiwi.word.api.common.enumeration.WordTypeEnum;
import me.fengorz.kiwi.word.api.dto.queue.RemovePronunciatioinMqDTO;
import me.fengorz.kiwi.word.api.dto.queue.result.FetchWordReplaceDTO;
import me.fengorz.kiwi.word.api.entity.*;
import me.fengorz.kiwi.word.biz.service.base.*;
import me.fengorz.kiwi.word.biz.service.operate.CleanerService;
import me.fengorz.kiwi.word.biz.service.operate.OperateService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author Kason Zhan @Date 2020/7/29 8:56 PM
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CleanerServiceImpl implements CleanerService {

    private final OperateService operateService;
    private final WordFetchQueueService queueService;
    private final WordMainService mainService;
    private final CharacterService characterService;
    private final ParaphraseService paraphraseService;
    private final ParaphraseExampleService exampleService;
    private final PronunciationService pronunciationService;
    private final WordMainVariantService variantService;
    private final ParaphrasePhraseService phraseService;
    private DfsService dfsService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<RemovePronunciatioinMqDTO> removeWord(String wordName, Integer queueId) {
        List<RemovePronunciatioinMqDTO> result = new ArrayList<>();
        List<WordMainDO> list =
            mainService.list(Wrappers.<WordMainDO>lambdaQuery().eq(WordMainDO::getWordName, wordName));
        if (KiwiCollectionUtils.isEmpty(list)) {
            Optional.ofNullable(variantService.listWordMain(wordName, queueId)).ifPresent(list::addAll);
        }

        if (KiwiCollectionUtils.isEmpty(list)) {
            return result;
        }

        for (WordMainDO wordMainDO : list) {
            this.evictAll(wordMainDO, wordName);
            List<RemovePronunciatioinMqDTO> dtoList = this.removeWordRelatedData(wordMainDO);
            dtoList.forEach(dto -> dto.setQueueId(queueId));
            KiwiCollectionUtils.addAllIfNotContains(result, dtoList);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<RemovePronunciatioinMqDTO> removeWord(Integer queueId) {
        List<RemovePronunciatioinMqDTO> result = new LinkedList<>();
        Optional.ofNullable(queueService.getOneAnyhow(queueId)).ifPresent(queue -> {
            String wordName = queue.getWordName();
            String derivation = queue.getDerivation();

            log.info("Remove word[{}] from queueId[{}], derivation[{}]", wordName, queueId, derivation);

            // if (KiwiStringUtils.equals(wordName, derivation)) {
            // list.addAll(mainService.list(wordName,
            // WordTypeEnum.WORD.getType()));
            // } else {
            // // 如果所查单词和单词的原型不同的话
            // list.addAll(mainService.list(derivation,
            // WordTypeEnum.WORD.getType()));
            // }
            // List<WordMainDO> list = new
            // LinkedList<>(mainService.listDirtyData(queue.getWordId()));

            List<WordMainDO> list = new ArrayList<>(operateService.collectDirtyData(queueId, wordName));

            log.info("Dirty data[wordName={}] list size: {}", wordName, list.size());

            if (KiwiCollectionUtils.isEmpty(list)) {
                return;
            }

            for (WordMainDO wordMainDO : list) {
                this.evictAll(wordMainDO, wordName);
                List<RemovePronunciatioinMqDTO> temps = this.removeWordRelatedData(wordMainDO);
                variantService
                    .remove(Wrappers.<WordMainVariantDO>lambdaQuery().eq(WordMainVariantDO::getVariantName, wordName));
                temps.forEach(dto -> dto.setQueueId(queueId));
                KiwiCollectionUtils.addAllIfNotContains(result, temps);
            }
        });
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removePhrase(Integer queueId) {
        Optional.ofNullable(queueService.getOneAnyhow(queueId)).ifPresent(queue -> {
            String wordName = queue.getWordName();
            List<WordMainDO> list =
                new LinkedList<>(mainService.list(wordName, WordTypeEnum.PHRASE.getType()));
            if (KiwiCollectionUtils.isEmpty(list)) {
                return;
            }
            for (WordMainDO wordMainDO : list) {
                Integer wordId = wordMainDO.getWordId();
                this.evictAll(wordMainDO, wordName);
                mainService.remove(Wrappers.<WordMainDO>lambdaQuery().eq(WordMainDO::getWordName, wordName)
                    .eq(WordMainDO::getInfoType, WordTypeEnum.PHRASE.getType()));
                variantService.delByWordId(wordId);
                paraphraseService.delByWordId(wordId);
            }
        });
        return true;
    }

    private List<RemovePronunciatioinMqDTO> removeWordRelatedData(WordMainDO wordMainDO) {
        final String wordName = wordMainDO.getWordName();
        mainService.removeById(wordMainDO.getWordId());
        operateService.cacheReplace(wordName,
            operateService.getCacheReplace(wordName).setOldRelWordId(wordMainDO.getWordId()));
        return this.removeRelatedData(wordMainDO);
    }

    private List<RemovePronunciatioinMqDTO> removeRelatedData(WordMainDO word) {
        Integer wordId = word.getWordId();

        variantService.delByWordId(wordId);
        List<CharacterDO> characterList =
            characterService.list(Wrappers.<CharacterDO>lambdaQuery().eq(CharacterDO::getWordId, wordId));
        if (KiwiCollectionUtils.isNotEmpty(characterList)) {
            for (CharacterDO character : characterList) {
                Integer characterId = character.getCharacterId();
                List<ParaphraseDO> paraphraseList = paraphraseService
                    .list(Wrappers.<ParaphraseDO>lambdaQuery().eq(ParaphraseDO::getCharacterId, characterId));
                if (CollUtil.isNotEmpty(paraphraseList)) {
                    for (ParaphraseDO paraphrase : paraphraseList) {
                        Integer paraphraseId = paraphrase.getParaphraseId();
                        LambdaQueryWrapper<ParaphraseExampleDO> exampleQueryWrapper = Wrappers
                            .<ParaphraseExampleDO>lambdaQuery().eq(ParaphraseExampleDO::getParaphraseId, paraphraseId);
                        List<ParaphraseExampleDO> exampleList = exampleService.list(exampleQueryWrapper);
                        if (KiwiCollectionUtils.isNotEmpty(exampleList)) {
                            for (ParaphraseExampleDO example : exampleList) {
                                // 将已删除的老的exampleId缓存起来，这样可以替换掉收藏本的关联id
                                Optional.ofNullable(example.getSerialNumber()).filter(serialNumber -> serialNumber > 0)
                                    .ifPresent(serialNumber -> {
                                        FetchWordReplaceDTO replaceDTO =
                                            operateService.getCacheReplace(word.getWordName());
                                        Map<Integer, FetchWordReplaceDTO.Binder> exampleBinderMap =
                                            replaceDTO.getExampleBinderMap();
                                        exampleBinderMap.put(serialNumber,
                                            new FetchWordReplaceDTO.Binder().setOldId(example.getExampleId()));
                                        operateService.cacheReplace(word.getWordName(), replaceDTO);
                                    });
                            }
                            exampleService.remove(exampleQueryWrapper);
                        }

                        // 将已删除的老的paraphraseId缓存起来，这样可以替换掉收藏本的关联id
                        Optional.ofNullable(paraphrase.getSerialNumber()).filter(serialNumber -> serialNumber > 0)
                            .ifPresent(serialNumber -> {
                                FetchWordReplaceDTO replaceDTO = operateService.getCacheReplace(word.getWordName());
                                Map<Integer, FetchWordReplaceDTO.Binder> paraphraseBinderMap =
                                    replaceDTO.getParaphraseBinderMap();
                                paraphraseBinderMap.put(serialNumber,
                                    new FetchWordReplaceDTO.Binder().setOldId(paraphraseId));
                                operateService.cacheReplace(word.getWordName(), replaceDTO);
                            });

                        phraseService.remove(Wrappers.<ParaphrasePhraseDO>lambdaQuery()
                            .eq(ParaphrasePhraseDO::getParaphraseId, paraphraseId));
                    }
                }
                if (CollUtil.isNotEmpty(paraphraseList)) {
                    paraphraseService
                        .remove(Wrappers.<ParaphraseDO>lambdaUpdate().eq(ParaphraseDO::getCharacterId, characterId));
                }
                characterService.evict(characterId);
            }
            characterService.remove(Wrappers.<CharacterDO>lambdaUpdate().eq(CharacterDO::getWordId, wordId));
        }

        // 删除分布式文件系统里面的文件
        LambdaQueryWrapper<PronunciationDO> pronunciationWrapper =
            Wrappers.<PronunciationDO>lambdaQuery().eq(PronunciationDO::getWordId, wordId);
        List<PronunciationDO> wordPronunciationList = pronunciationService.list(pronunciationWrapper);
        pronunciationService.remove(pronunciationWrapper);

        return wordPronunciationList.stream()
            .filter(pronunciationDO -> StrUtil.isNotBlank(pronunciationDO.getVoiceFilePath()))
            .map(pronunciationDO -> new RemovePronunciatioinMqDTO().setGroupName(pronunciationDO.getGroupName())
                .setVoiceFilePath(pronunciationDO.getVoiceFilePath()))
            .collect(Collectors.toList());
    }

    @Override
    public void evictAll(WordMainDO wordMainDO, String wordName) {
        // 这里缓存的删除要在Mysql的删除之前做
        if (KiwiStringUtils.isNotEquals(wordName, wordMainDO.getWordName())) {
            operateService.evict(wordName, wordMainDO);
            operateService.evict(wordMainDO.getWordName(), wordMainDO);
        } else {
            operateService.evict(wordMainDO.getWordName(), wordMainDO);
        }
        mainService.evictById(wordMainDO.getWordId());
    }
}
