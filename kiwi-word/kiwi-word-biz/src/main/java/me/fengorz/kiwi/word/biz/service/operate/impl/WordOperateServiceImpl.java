/*
 *
 * Copyright [2019~2025] [zhanshifeng]
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

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.bdf.core.service.ISeqService;
import me.fengorz.kiwi.common.api.annotation.cache.KiwiCacheKey;
import me.fengorz.kiwi.common.api.annotation.cache.KiwiCacheKeyPrefix;
import me.fengorz.kiwi.common.api.constant.CacheConstants;
import me.fengorz.kiwi.common.api.constant.CommonConstants;
import me.fengorz.kiwi.common.api.exception.ResourceNotFoundException;
import me.fengorz.kiwi.common.api.exception.ServiceException;
import me.fengorz.kiwi.common.fastdfs.service.IDfsService;
import me.fengorz.kiwi.common.sdk.util.lang.collection.KiwiCollectionUtils;
import me.fengorz.kiwi.common.sdk.util.lang.string.KiwiStringUtils;
import me.fengorz.kiwi.common.sdk.util.validate.KiwiAssertUtils;
import me.fengorz.kiwi.word.api.common.WordConstants;
import me.fengorz.kiwi.word.api.dto.queue.fetch.FetchWordReplaceDTO;
import me.fengorz.kiwi.word.api.entity.*;
import me.fengorz.kiwi.word.api.vo.WordMainVO;
import me.fengorz.kiwi.word.api.vo.WordParaphraseExampleVO;
import me.fengorz.kiwi.word.api.vo.detail.WordCharacterVO;
import me.fengorz.kiwi.word.api.vo.detail.WordParaphraseVO;
import me.fengorz.kiwi.word.api.vo.detail.WordPronunciationVO;
import me.fengorz.kiwi.word.api.vo.detail.WordQueryVO;
import me.fengorz.kiwi.word.biz.service.base.*;
import me.fengorz.kiwi.word.biz.service.operate.IWordOperateService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @Description 单词相关业务的复杂逻辑解耦
 * @Author zhanshifeng
 * @Date 2019/11/25 3:13 PM
 */
@Slf4j
@Service
@RequiredArgsConstructor
@KiwiCacheKeyPrefix(WordConstants.CACHE_KEY_PREFIX_OPERATE.CLASS)
public class WordOperateServiceImpl implements IWordOperateService {

    private final IWordMainService wordMainService;
    private final IWordCharacterService wordCharacterService;
    private final IWordParaphraseService wordParaphraseService;
    private final IWordParaphraseExampleService wordParaphraseExampleService;
    private final IWordPronunciationService wordPronunciationService;
    private final IWordFetchQueueService wordFetchQueueService;
    private final IWordStarListService wordStarListService;
    private final IWordParaphraseStarListService wordParaphraseStarListService;
    private final IWordExampleStarListService wordExampleStarListService;
    private final IWordStarRelService wordStarRelService;
    private final IWordParaphraseStarRelService wordParaphraseStarRelService;
    private final IWordExampleStarRelService wordExampleStarRelService;
    private final IWordMainVariantService wordMainVariantService;
    private final IWordParaphrasePhraseService wordParaphrasePhraseService;
    private final IDfsService dfsService;
    private final ISeqService seqService;

    /**
     * 封装返回给前端的整个单词的数据DTO
     *
     * @param wordName
     * @return
     */
    @Override
    @KiwiCacheKeyPrefix(WordConstants.CACHE_KEY_PREFIX_OPERATE.METHOD_WORD_NAME)
    @Cacheable(cacheNames = WordConstants.CACHE_NAMES, keyGenerator = CacheConstants.CACHE_KEY_GENERATOR_BEAN,
            unless = "#result == null")
    public WordQueryVO queryWord(@KiwiCacheKey String wordName) {
        WordQueryVO vo = new WordQueryVO();
        WordMainDO word = wordMainService.getOne(wordName);
        // if you can't find the result after the tense is determined, insert a record into the queue to be fetched
        if (word == null) {
            Integer sourceWordId = null;
            List<Integer> list = wordMainVariantService.getWordId(wordName);
            if (list != null) {
                KiwiAssertUtils.isTrue(list.size() == 1, "Multiple results for [{}]!", wordName);
                sourceWordId = list.get(0);
            }
            if (sourceWordId == null) {
                // 异步爬虫抓取，插入队列表
                wordFetchQueueService.flagStartFetchOnAsync(wordName);
            } else {
                final String name = wordMainService.getWordName(sourceWordId);
                if (KiwiStringUtils.isNotBlank(name)) {
                    return this.queryWord(name);
                }
            }
        }

        KiwiAssertUtils.resourceNotNull(word, "No results for [{}]!", wordName);

        vo.setWordId(word.getWordId());
        vo.setWordName(word.getWordName());
        vo.setIsCollect(CommonConstants.FLAG_N);

        Integer wordId = word.getWordId();
        vo.setWordCharacterVOList(assembleWordQueryVO(wordName, wordId));
        return vo;
    }

    private List<WordCharacterVO> assembleWordQueryVO(String wordName, Integer wordId) throws ServiceException {
        List<WordCharacterDO> characterList =
                wordCharacterService.list(new QueryWrapper<>(new WordCharacterDO().setWordId(wordId)));
        KiwiAssertUtils.serviceNotEmpty(characterList, "No character for [{}]!", wordName);

        List<WordCharacterVO> characterVOList = new ArrayList<>();
        for (WordCharacterDO character : characterList) {
            WordCharacterVO characterVO = new WordCharacterVO();
            BeanUtil.copyProperties(character, characterVO);
            characterVOList.add(characterVO);

            List<WordParaphraseVO> paraphraseVOList = assembleParaphraseVOList(character.getCharacterId());
            if (KiwiCollectionUtils.isEmpty(paraphraseVOList)) {
                continue;
            }
            characterVO.setWordParaphraseVOList(paraphraseVOList);

            List<WordPronunciationVO> pronunciationVOList = assemblePronunciationVOList(character);
            if (KiwiCollectionUtils.isEmpty(pronunciationVOList)) {
                continue;
            }
            characterVO.setWordPronunciationVOList(pronunciationVOList);
        }
        return characterVOList;
    }

    private List<WordPronunciationVO> assemblePronunciationVOList(WordCharacterDO wordCharacter) {
        List<WordPronunciationDO> wordPronunciationList = wordPronunciationService
                .list(new QueryWrapper<>(new WordPronunciationDO().setCharacterId(wordCharacter.getCharacterId())));
        // 单词发音音频文件传给pronunciationId给前端，让前端再次调用接口下载文件流
        if (CollUtil.isEmpty(wordPronunciationList)) {
            return null;
        }

        List<WordPronunciationVO> wordPronunciationVOList = new ArrayList<>();

        for (WordPronunciationDO wordPronunciation : wordPronunciationList) {
            WordPronunciationVO wordPronunciationVO = new WordPronunciationVO();
            BeanUtil.copyProperties(wordPronunciation, wordPronunciationVO);
            wordPronunciationVOList.add(wordPronunciationVO);
        }
        return wordPronunciationVOList;
    }

    private List<WordParaphraseVO> assembleParaphraseVOList(Integer characterId) {
        List<WordParaphraseVO> paraphraseVOList = new ArrayList<>();
        List<WordParaphraseDO> paraphraseDOList =
                wordParaphraseService.list(new QueryWrapper<>(new WordParaphraseDO().setCharacterId(characterId)));
        if (CollUtil.isEmpty(paraphraseDOList)) {
            return paraphraseVOList;
        }

        for (WordParaphraseDO paraphrase : paraphraseDOList) {
            WordParaphraseVO vo = new WordParaphraseVO();
            BeanUtil.copyProperties(paraphrase, vo);

            if (1 == paraphrase.getIsHavePhrase()) {
                List<String> phraseList = new ArrayList<>();
                List<WordParaphrasePhraseDO> phraseDOList =
                        wordParaphrasePhraseService.list(Wrappers.<WordParaphrasePhraseDO>lambdaQuery()
                                .eq(WordParaphrasePhraseDO::getParaphraseId, paraphrase.getParaphraseId())
                                .eq(WordParaphrasePhraseDO::getIsValid, CommonConstants.FLAG_YES));
                if (KiwiCollectionUtils.isNotEmpty(phraseDOList)) {
                    for (WordParaphrasePhraseDO phraseDO : phraseDOList) {
                        phraseList.add(phraseDO.getPhrase());
                    }
                }
                vo.setPhraseList(phraseList);
            }

            paraphraseVOList.add(vo);

            List<WordParaphraseExampleVO> exampleVOList =
                    assembleExampleVOList(vo.getParaphraseId());
            if (exampleVOList == null) {
                continue;
            }
            vo.setWordParaphraseExampleVOList(exampleVOList);
        }
        return paraphraseVOList;
    }

    private List<WordParaphraseExampleVO> assembleExampleVOList(Integer paraphraseId) {
        List<WordParaphraseExampleVO> ExampleVOList = new ArrayList<>();
        List<WordParaphraseExampleDO> exampleDOList = wordParaphraseExampleService
                .list(new QueryWrapper<>(new WordParaphraseExampleDO().setParaphraseId(paraphraseId)));
        if (CollUtil.isEmpty(exampleDOList)) {
            return null;
        }
        for (WordParaphraseExampleDO example : exampleDOList) {
            WordParaphraseExampleVO vo = new WordParaphraseExampleVO();
            BeanUtil.copyProperties(example, vo);
            ExampleVOList.add(vo);
        }
        return ExampleVOList;
    }

    @Override
    public boolean putWordIntoStarList(Integer wordId, Integer listId) {
        LambdaQueryWrapper<WordStarRelDO> queryWrapper = new LambdaQueryWrapper<WordStarRelDO>()
                .eq(WordStarRelDO::getListId, listId).eq(WordStarRelDO::getWordId, wordId);
        int count = wordStarRelService.count(queryWrapper);
        if (count > 0) {
            return false;
        }
        return wordStarRelService.save(new WordStarRelDO().setListId(listId).setWordId(wordId));
    }

    @Override
    public boolean removeWordStarList(Integer wordId, Integer listId) {
        LambdaQueryWrapper<WordStarRelDO> queryWrapper = new LambdaQueryWrapper<WordStarRelDO>()
                .eq(WordStarRelDO::getListId, listId).eq(WordStarRelDO::getWordId, wordId);
        int count = wordStarRelService.count(queryWrapper);
        KiwiAssertUtils.serviceNotEmpty(count, "wordStar is not exists!");
        return wordStarRelService.remove(queryWrapper);
    }

    @Override
    public boolean putParaphraseIntoStarList(Integer paraphraseId, Integer listId) {
        KiwiAssertUtils.serviceNotEmpty(wordParaphraseService.countById(paraphraseId), "paraphrase is not exists!");
        KiwiAssertUtils.serviceNotEmpty(wordParaphraseStarListService.countById(listId),
                "paraphraseStarList is not exists!");
        LambdaQueryWrapper<WordParaphraseStarRelDO> wrapper = new LambdaQueryWrapper<WordParaphraseStarRelDO>()
                .eq(WordParaphraseStarRelDO::getListId, listId).eq(WordParaphraseStarRelDO::getParaphraseId, paraphraseId);
        KiwiAssertUtils.serviceEmpty(wordParaphraseStarRelService.count(wrapper), "paraphrase already exists!");
        return wordParaphraseStarRelService
                .save(new WordParaphraseStarRelDO().setListId(listId).setParaphraseId(paraphraseId));
    }

    @Override
    @KiwiCacheKeyPrefix(WordConstants.CACHE_KEY_PREFIX_OPERATE.METHOD_PARAPHRASE_ID)
    @Cacheable(cacheNames = WordConstants.CACHE_NAMES, keyGenerator = CacheConstants.CACHE_KEY_GENERATOR_BEAN,
            unless = "#result == null")
    public WordParaphraseVO findWordParaphraseVO(@KiwiCacheKey Integer paraphraseId) {
        WordParaphraseVO paraphraseVO = new WordParaphraseVO();
        List<WordParaphraseExampleVO> exampleVOList = new ArrayList<>();
        WordParaphraseDO paraphrase = wordParaphraseService.getById(paraphraseId);
        BeanUtil.copyProperties(paraphrase, paraphraseVO);
        List<WordParaphraseExampleDO> exampleDOList =
                wordParaphraseExampleService.list(new LambdaQueryWrapper<WordParaphraseExampleDO>()
                        .eq(WordParaphraseExampleDO::getParaphraseId, paraphraseId));
        if (CollUtil.isNotEmpty(exampleDOList)) {
            exampleDOList.forEach(wordParaphraseExampleDO -> {
                WordParaphraseExampleVO exampleVO = new WordParaphraseExampleVO();
                BeanUtil.copyProperties(wordParaphraseExampleDO, exampleVO);
                exampleVOList.add(exampleVO);
            });
        }
        paraphraseVO.setWordParaphraseExampleVOList(exampleVOList);
        paraphraseVO.setWordName(wordMainService.getWordName(paraphrase.getWordId()));
        paraphraseVO.setCodes(paraphrase.getCodes());

        WordCharacterVO characterVO = wordCharacterService.getFromCache(paraphrase.getCharacterId());
        paraphraseVO.setWordCharacter(characterVO.getWordCharacter());
        paraphraseVO.setWordLabel(characterVO.getWordLabel());

        List<WordPronunciationDO> pronunciationList = wordPronunciationService
                .list(new QueryWrapper<>(new WordPronunciationDO().setCharacterId(characterVO.getCharacterId())));

        if (KiwiCollectionUtils.isNotEmpty(pronunciationList)) {
            List<WordPronunciationVO> pronunciationVOList = new ArrayList<>();
            pronunciationList.forEach(entity -> {
                WordPronunciationVO vo = new WordPronunciationVO();
                BeanUtil.copyProperties(entity, vo);
                pronunciationVOList.add(vo);
            });
            paraphraseVO.setWordPronunciationVOList(pronunciationVOList);
        }

        return paraphraseVO;
    }

    @Override
    public boolean putExampleIntoStarList(Integer exampleId, Integer listId) {
        LambdaQueryWrapper<WordExampleStarRelDO> queryWrapper = new LambdaQueryWrapper<WordExampleStarRelDO>()
                .eq(WordExampleStarRelDO::getListId, listId).eq(WordExampleStarRelDO::getExampleId, exampleId);
        KiwiAssertUtils.serviceEmpty(wordExampleStarRelService.count(queryWrapper), "example rel is already exists!");
        return wordExampleStarRelService.save(new WordExampleStarRelDO().setListId(listId).setExampleId(exampleId));
    }

    @Override
    public boolean removeExampleStar(Integer exampleId, Integer listId) {
        LambdaQueryWrapper<WordExampleStarRelDO> queryWrapper = new LambdaQueryWrapper<WordExampleStarRelDO>()
                .eq(WordExampleStarRelDO::getListId, listId).eq(WordExampleStarRelDO::getExampleId, exampleId);
        int count = wordExampleStarRelService.count(queryWrapper);
        KiwiAssertUtils.serviceNotEmpty(count, "example is not exists!");
        return wordExampleStarRelService.remove(queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean insertVariant(String inputWordName, String fetchWordName) {
        if (KiwiStringUtils.equals(inputWordName, fetchWordName)) {
            return false;
        }

        // 先判断变种是否存在，如果不存在再插入
        WordMainVO mainVO = wordMainService.getOne(fetchWordName);
        if (mainVO == null) {
            throw new ResourceNotFoundException("word {} 不存在！", fetchWordName);
        }

        final Integer wordId = mainVO.getWordId();

        if (wordMainVariantService.isExist(wordId, inputWordName)) {
            return false;
        }

        return wordMainVariantService.insertOne(wordId, inputWordName);
    }

    /* private methods beginning */

    @Override
    @KiwiCacheKeyPrefix(WordConstants.CACHE_KEY_PREFIX_OPERATE.METHOD_WORD_NAME)
    @CacheEvict(cacheNames = WordConstants.CACHE_NAMES, keyGenerator = CacheConstants.CACHE_KEY_GENERATOR_BEAN)
    public void evict(@KiwiCacheKey String wordName, WordMainDO one) {
        List<WordParaphraseDO> list = wordParaphraseService
                .list(Wrappers.<WordParaphraseDO>lambdaQuery().eq(WordParaphraseDO::getWordId, one.getWordId())
                        .eq(WordParaphraseDO::getIsDel, CommonConstants.FLAG_DEL_NO));
        if (KiwiCollectionUtils.isNotEmpty(list)) {
            for (WordParaphraseDO paraphraseDO : list) {
                this.evictParaphrase(paraphraseDO.getParaphraseId());
            }
        }
    }

    @KiwiCacheKeyPrefix(WordConstants.CACHE_KEY_PREFIX_OPERATE.METHOD_PARAPHRASE_ID)
    @CacheEvict(cacheNames = WordConstants.CACHE_NAMES, keyGenerator = CacheConstants.CACHE_KEY_GENERATOR_BEAN)
    private void evictParaphrase(@KiwiCacheKey Integer paraphraseId) {
    }

    /**
     * 获取DTO之后，要立马调用cachePutFetchReplace更新
     *
     * @param wordName
     * @return
     */
    @Override
    @KiwiCacheKeyPrefix(WordConstants.CACHE_KEY_PREFIX_OPERATE.METHOD_FETCH_REPLACE)
    @Cacheable(cacheNames = WordConstants.CACHE_NAMES, keyGenerator = CacheConstants.CACHE_KEY_GENERATOR_BEAN,
            unless = "#result == null")
    public FetchWordReplaceDTO cacheGetFetchReplace(@KiwiCacheKey String wordName) {
        // TODO ZSF 这里要设置超时时间，这块逻辑改成不用注解实现
        return new FetchWordReplaceDTO();
    }

    @Override
    @KiwiCacheKeyPrefix(WordConstants.CACHE_KEY_PREFIX_OPERATE.METHOD_FETCH_REPLACE)
    @CachePut(cacheNames = WordConstants.CACHE_NAMES, keyGenerator = CacheConstants.CACHE_KEY_GENERATOR_BEAN,
            unless = "#result == null")
    public FetchWordReplaceDTO cachePutFetchReplace(@KiwiCacheKey String wordName, FetchWordReplaceDTO dto) {
        if (dto == null) {
            return new FetchWordReplaceDTO();
        } else {
            return dto;
        }
    }

    @KiwiCacheKeyPrefix(WordConstants.CACHE_KEY_PREFIX_OPERATE.METHOD_FETCH_REPLACE)
    @CacheEvict(cacheNames = WordConstants.CACHE_NAMES, keyGenerator = CacheConstants.CACHE_KEY_GENERATOR_BEAN)
    private void cacheEvictFetchReplace(@KiwiCacheKey String wordName) {
    }

    @Override
    public void fetchReplaceCallBack(String wordName) {
        FetchWordReplaceDTO replaceDTO = this.cacheGetFetchReplace(wordName);
        wordStarRelService.replaceFetchResult(replaceDTO.getOldRelWordId(), replaceDTO.getNewRelWordId());
        Map<String, Integer> newParaphraseIdMap = replaceDTO.getNewParaphraseIdMap();
        Optional.ofNullable(replaceDTO.getOldParaphraseIdMap()).ifPresent(oldParaphraseIdMap -> {
            oldParaphraseIdMap.forEach((text, id) -> {
                if (newParaphraseIdMap.containsKey(text)) {
                    wordParaphraseStarRelService.replaceFetchResult(id, newParaphraseIdMap.get(text));
                }
            });
        });

        Map<String, Integer> newExampleIdMap = replaceDTO.getNewExampleIdMap();
        Optional.ofNullable(replaceDTO.getOldExampleIdMap()).ifPresent(oldExampleIdMap -> {
            oldExampleIdMap.forEach((text, id) -> {
                if (newExampleIdMap.containsKey(text)) {
                    wordExampleStarRelService.replaceFetchResult(id, newExampleIdMap.get(text));
                }
            });
        });
        this.cacheEvictFetchReplace(wordName);
    }

}
