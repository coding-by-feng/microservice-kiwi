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
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @Description TODO
 * @Author zhanshifeng
 * @Date 2020/7/29 8:56 PM
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WordCleanerService implements IWordCleanerService {

    private final IWordOperateService operateService;
    private final IWordMainService wordMainService;
    private final IWordCharacterService wordCharacterService;
    private final IWordParaphraseService wordParaphraseService;
    private final IWordParaphraseExampleService wordParaphraseExampleService;
    private final IWordPronunciationService wordPronunciationService;
    private final IWordMainVariantService wordMainVariantService;
    private final IWordParaphrasePhraseService wordParaphrasePhraseService;
    private final IDfsService dfsService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<RemovePronunciatioinMqDTO> removeWord(String wordName, Integer queueId) {
        List<RemovePronunciatioinMqDTO> result = new ArrayList<>();
        List<WordMainDO> list =
                wordMainService.list(Wrappers.<WordMainDO>lambdaQuery().eq(WordMainDO::getWordName, wordName));
        if (KiwiCollectionUtils.isEmpty(list)) {
            Optional.ofNullable(wordMainVariantService.listWordMain(wordName)).ifPresent(list::addAll);
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

    private List<RemovePronunciatioinMqDTO> subRemoveWord(WordMainDO wordMainDO) {
        final String wordName = wordMainDO.getWordName();
        wordMainService.remove(Wrappers.<WordMainDO>lambdaQuery().eq(WordMainDO::getWordName, wordName));
        wordMainVariantService.remove(Wrappers.<WordMainVariantDO>lambdaQuery().eq(WordMainVariantDO::getVariantName, wordName));
        operateService.cachePutFetchReplace(wordName,
                operateService.cacheGetFetchReplace(wordName).setOldRelWordId(wordMainDO.getWordId()));
        return this.removeWordRelatedData(wordMainDO);
    }

    private List<RemovePronunciatioinMqDTO> removeWordRelatedData(WordMainDO wordMainDO) {
        Integer wordId = wordMainDO.getWordId();

        wordMainVariantService.delByWordId(wordId);
        QueryWrapper<WordCharacterDO> wordCharacterQueryWrapper =
                new QueryWrapper<>(new WordCharacterDO().setWordId(wordId));
        List<WordCharacterDO> characterList = wordCharacterService.list(wordCharacterQueryWrapper);
        if (CollUtil.isNotEmpty(characterList)) {
            for (WordCharacterDO wordCharacter : characterList) {
                Integer characterId = wordCharacter.getCharacterId();
                QueryWrapper<WordParaphraseDO> wordParaphraseQueryWrapper =
                        new QueryWrapper<>(new WordParaphraseDO().setCharacterId(characterId));
                List<WordParaphraseDO> paraphraseList = wordParaphraseService.list(wordParaphraseQueryWrapper);
                if (CollUtil.isNotEmpty(paraphraseList)) {
                    for (WordParaphraseDO wordParaphraseDO : paraphraseList) {
                        Integer paraphraseId = wordParaphraseDO.getParaphraseId();
                        LambdaQueryWrapper<WordParaphraseExampleDO> exampleDOLambdaQueryWrapper =
                                Wrappers.<WordParaphraseExampleDO>lambdaQuery().eq(WordParaphraseExampleDO::getParaphraseId,
                                        paraphraseId);
                        List<WordParaphraseExampleDO> exampleDOList =
                                wordParaphraseExampleService.list(exampleDOLambdaQueryWrapper);
                        if (KiwiCollectionUtils.isNotEmpty(exampleDOList)) {
                            for (WordParaphraseExampleDO wordParaphraseExampleDO : exampleDOList) {
                                // 将已删除的老的exampleId缓存起来，这样可以替换掉收藏本的关联id
                                FetchWordReplaceDTO replaceDTO =
                                        operateService.cacheGetFetchReplace(wordMainDO.getWordName());
                                Map<String, Integer> oldExampleIdMap = replaceDTO.getOldExampleIdMap();
                                oldExampleIdMap.put(wordParaphraseExampleDO.getExampleSentence(),
                                        wordParaphraseExampleDO.getExampleId());
                                operateService.cachePutFetchReplace(wordMainDO.getWordName(), replaceDTO);
                            }
                            wordParaphraseExampleService.remove(exampleDOLambdaQueryWrapper);
                        }

                        // 将已删除的老的paraphraseId缓存起来，这样可以替换掉收藏本的关联id
                        FetchWordReplaceDTO replaceDTO = operateService.cacheGetFetchReplace(wordMainDO.getWordName());
                        replaceDTO.getOldParaphraseIdMap().put(wordParaphraseDO.getParaphraseEnglish(), paraphraseId);
                        operateService.cachePutFetchReplace(wordMainDO.getWordName(), replaceDTO);

                        wordParaphrasePhraseService.remove(Wrappers.<WordParaphrasePhraseDO>lambdaQuery()
                                .eq(WordParaphrasePhraseDO::getParaphraseId, paraphraseId));
                    }
                }
                if (CollUtil.isNotEmpty(paraphraseList)) {
                    wordParaphraseService.remove(wordParaphraseQueryWrapper);
                }
                wordCharacterService.evict(characterId);
            }
            wordCharacterService.remove(wordCharacterQueryWrapper);
        }

        // 删除分布式文件系统里面的文件
        LambdaQueryWrapper<WordPronunciationDO> pronunciationWrapper =
                Wrappers.<WordPronunciationDO>lambdaQuery().eq(WordPronunciationDO::getWordId, wordId);
        List<WordPronunciationDO> wordPronunciationList = wordPronunciationService.list(pronunciationWrapper);
        wordPronunciationService.remove(pronunciationWrapper);

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
