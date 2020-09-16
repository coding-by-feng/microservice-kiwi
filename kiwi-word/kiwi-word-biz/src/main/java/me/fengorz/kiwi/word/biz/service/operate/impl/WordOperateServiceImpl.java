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
import java.util.Optional;
import java.util.stream.Collectors;

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

    private final IWordMainService mainService;
    private final IWordCharacterService characterService;
    private final IWordParaphraseService paraphraseService;
    private final IWordParaphraseExampleService exampleService;
    private final IWordPronunciationService pronunciationService;
    private final IWordFetchQueueService fetchQueueService;
    private final IWordStarListService wordStarListService;
    private final IWordParaphraseStarListService paraphraseStarListService;
    private final IWordExampleStarListService exampleStarListService;
    private final IWordStarRelService wordStarRelService;
    private final IWordParaphraseStarRelService paraphraseStarRelService;
    private final IWordExampleStarRelService exampleStarRelService;
    private final IWordMainVariantService mainVariantService;
    private final IWordParaphrasePhraseService phraseService;
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
        WordMainDO word = mainService.getOne(wordName);
        // if you can't find the result after the tense is determined, insert a record into the queue to be fetched
        if (word == null) {
            Integer sourceWordId = null;
            List<Integer> list = mainVariantService.getWordId(wordName);
            if (list != null) {
                KiwiAssertUtils.isTrue(list.size() == 1, "Multiple results for [{}]!", wordName);
                sourceWordId = list.get(0);
            }
            if (sourceWordId == null) {
                // 异步爬虫抓取，插入队列表
                fetchQueueService.flagStartFetchOnAsync(wordName);
            } else {
                final String name = mainService.getWordName(sourceWordId);
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
                characterService.list(new QueryWrapper<>(new WordCharacterDO().setWordId(wordId)));
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
        List<WordPronunciationDO> wordPronunciationList = pronunciationService
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
                paraphraseService.list(new QueryWrapper<>(new WordParaphraseDO().setCharacterId(characterId)));
        if (CollUtil.isEmpty(paraphraseDOList)) {
            return paraphraseVOList;
        }

        for (WordParaphraseDO paraphrase : paraphraseDOList) {
            WordParaphraseVO vo = new WordParaphraseVO();
            BeanUtil.copyProperties(paraphrase, vo);

            if (1 == paraphrase.getIsHavePhrase()) {
                List<String> phraseList = new ArrayList<>();
                List<WordParaphrasePhraseDO> phraseDOList =
                        phraseService.list(Wrappers.<WordParaphrasePhraseDO>lambdaQuery()
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
        List<WordParaphraseExampleDO> exampleDOList = exampleService
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
        KiwiAssertUtils.serviceNotEmpty(paraphraseService.countById(paraphraseId), "paraphrase is not exists!");
        KiwiAssertUtils.serviceNotEmpty(paraphraseStarListService.countById(listId),
                "paraphraseStarList is not exists!");
        LambdaQueryWrapper<WordParaphraseStarRelDO> wrapper = new LambdaQueryWrapper<WordParaphraseStarRelDO>()
                .eq(WordParaphraseStarRelDO::getListId, listId).eq(WordParaphraseStarRelDO::getParaphraseId, paraphraseId);
        KiwiAssertUtils.serviceEmpty(paraphraseStarRelService.count(wrapper), "paraphrase already exists!");
        return paraphraseStarRelService
                .save(new WordParaphraseStarRelDO().setListId(listId).setParaphraseId(paraphraseId));
    }

    @Override
    @KiwiCacheKeyPrefix(WordConstants.CACHE_KEY_PREFIX_OPERATE.METHOD_PARAPHRASE_ID)
    @Cacheable(cacheNames = WordConstants.CACHE_NAMES, keyGenerator = CacheConstants.CACHE_KEY_GENERATOR_BEAN,
            unless = "#result == null")
    public WordParaphraseVO findWordParaphraseVO(@KiwiCacheKey Integer paraphraseId) {
        WordParaphraseVO vo = new WordParaphraseVO();
        List<WordParaphraseExampleVO> exampleVOList = new ArrayList<>();
        WordParaphraseDO paraphrase = paraphraseService.getById(paraphraseId);
        BeanUtil.copyProperties(paraphrase, vo);
        List<WordParaphraseExampleDO> exampleDOList =
                exampleService.list(new LambdaQueryWrapper<WordParaphraseExampleDO>()
                        .eq(WordParaphraseExampleDO::getParaphraseId, paraphraseId));
        if (CollUtil.isNotEmpty(exampleDOList)) {
            exampleDOList.forEach(wordParaphraseExampleDO -> {
                WordParaphraseExampleVO exampleVO = new WordParaphraseExampleVO();
                BeanUtil.copyProperties(wordParaphraseExampleDO, exampleVO);
                exampleVOList.add(exampleVO);
            });
        }
        vo.setWordParaphraseExampleVOList(exampleVOList);
        vo.setWordName(mainService.getWordName(paraphrase.getWordId()));
        vo.setCodes(paraphrase.getCodes());

        WordCharacterVO characterVO = characterService.getFromCache(paraphrase.getCharacterId());
        vo.setWordCharacter(characterVO.getWordCharacter());
        vo.setWordLabel(characterVO.getWordLabel());

        List<WordParaphrasePhraseDO> phraseList = phraseService.list(Wrappers.<WordParaphrasePhraseDO>lambdaQuery().eq(WordParaphrasePhraseDO::getParaphraseId, paraphrase.getParaphraseId())
                .eq(WordParaphrasePhraseDO::getIsValid, CommonConstants.FLAG_YES));
        if (KiwiCollectionUtils.isNotEmpty(phraseList)) {
            vo.setPhraseList(phraseList.stream().map(WordParaphrasePhraseDO::getPhrase).collect(Collectors.toList()));
        }

        List<WordPronunciationDO> pronunciationList = pronunciationService
                .list(new QueryWrapper<>(new WordPronunciationDO().setCharacterId(characterVO.getCharacterId())));

        if (KiwiCollectionUtils.isNotEmpty(pronunciationList)) {
            List<WordPronunciationVO> pronunciationVOList = new ArrayList<>();
            pronunciationList.forEach(entity -> {
                WordPronunciationVO pronunciationVO = new WordPronunciationVO();
                BeanUtil.copyProperties(entity, pronunciationVO);
                pronunciationVOList.add(pronunciationVO);

                if (entity.getSoundmark().length() > WordConstants.SOUND_MARK_OVERLENGTH_THRESHOLD) {
                    vo.setIsOverlength(true);
                }
            });
            vo.setWordPronunciationVOList(pronunciationVOList);
        }

        return vo;
    }

    @Override
    public boolean putExampleIntoStarList(Integer exampleId, Integer listId) {
        LambdaQueryWrapper<WordExampleStarRelDO> queryWrapper = new LambdaQueryWrapper<WordExampleStarRelDO>()
                .eq(WordExampleStarRelDO::getListId, listId).eq(WordExampleStarRelDO::getExampleId, exampleId);
        KiwiAssertUtils.serviceEmpty(exampleStarRelService.count(queryWrapper), "example rel is already exists!");
        return exampleStarRelService.save(new WordExampleStarRelDO().setListId(listId).setExampleId(exampleId));
    }

    @Override
    public boolean removeExampleStar(Integer exampleId, Integer listId) {
        LambdaQueryWrapper<WordExampleStarRelDO> queryWrapper = new LambdaQueryWrapper<WordExampleStarRelDO>()
                .eq(WordExampleStarRelDO::getListId, listId).eq(WordExampleStarRelDO::getExampleId, exampleId);
        int count = exampleStarRelService.count(queryWrapper);
        KiwiAssertUtils.serviceNotEmpty(count, "example is not exists!");
        return exampleStarRelService.remove(queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean insertVariant(String inputWordName, String fetchWordName) {
        if (KiwiStringUtils.equals(inputWordName, fetchWordName)) {
            return false;
        }

        // 先判断变种是否存在，如果存在再插入
        WordMainVO mainVO = mainService.getOne(fetchWordName);
        if (mainVO == null) {
            throw new ResourceNotFoundException("word {} 不存在！", fetchWordName);
        }

        // 记录单词原型到队列表
        // fetchQueueService.saveDerivation(inputWordName, fetchWordName);

        final Integer wordId = mainVO.getWordId();
        if (mainVariantService.isExist(wordId, inputWordName)) {
            return false;
        }

        return mainVariantService.insertOne(wordId, inputWordName);
    }

    /* private methods beginning */

    @Override
    @KiwiCacheKeyPrefix(WordConstants.CACHE_KEY_PREFIX_OPERATE.METHOD_WORD_NAME)
    @CacheEvict(cacheNames = WordConstants.CACHE_NAMES, keyGenerator = CacheConstants.CACHE_KEY_GENERATOR_BEAN)
    public void evict(@KiwiCacheKey String wordName, WordMainDO one) {
        List<WordParaphraseDO> list = paraphraseService
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
    public FetchWordReplaceDTO getCacheReplace(@KiwiCacheKey String wordName) {
        // TODO ZSF 这里要设置超时时间，这块逻辑改成不用注解实现
        return new FetchWordReplaceDTO();
    }

    @Override
    @KiwiCacheKeyPrefix(WordConstants.CACHE_KEY_PREFIX_OPERATE.METHOD_FETCH_REPLACE)
    @CachePut(cacheNames = WordConstants.CACHE_NAMES, keyGenerator = CacheConstants.CACHE_KEY_GENERATOR_BEAN,
            unless = "#result == null")
    public FetchWordReplaceDTO cacheReplace(@KiwiCacheKey String wordName, FetchWordReplaceDTO dto) {
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
        FetchWordReplaceDTO replaceDTO = this.getCacheReplace(wordName);
        wordStarRelService.replaceFetchResult(replaceDTO.getOldRelWordId(), replaceDTO.getNewRelWordId());
        Optional.ofNullable(replaceDTO.getParaphraseBinderMap()).ifPresent(binderMap -> {
            binderMap.forEach((num, binder) -> {
                // TODO ZSF 这里需要校验为空，如果为空要终止队列，详细记录收藏数据
                paraphraseStarRelService.replaceFetchResult(binder.getOldId(), binder.getNewId());
            });
        });

        Optional.ofNullable(replaceDTO.getExampleBinderMap()).ifPresent(binderMap -> {
            binderMap.forEach((num, binder) -> {
                // TODO ZSF 这里需要校验为空，如果为空要终止队列，详细记录收藏数据
                exampleStarRelService.replaceFetchResult(binder.getOldId(), binder.getNewId());
            });
        });
        this.cacheEvictFetchReplace(wordName);
    }

}
