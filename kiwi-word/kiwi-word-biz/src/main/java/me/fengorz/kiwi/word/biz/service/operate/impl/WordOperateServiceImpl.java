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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.bdf.core.service.ISeqService;
import me.fengorz.kiwi.common.api.annotation.cache.KiwiCacheKey;
import me.fengorz.kiwi.common.api.annotation.cache.KiwiCacheKeyPrefix;
import me.fengorz.kiwi.common.api.constant.CacheConstants;
import me.fengorz.kiwi.common.api.constant.CommonConstants;
import me.fengorz.kiwi.common.api.exception.ResourceNotFoundException;
import me.fengorz.kiwi.common.api.exception.ServiceException;
import me.fengorz.kiwi.common.api.exception.dfs.DfsOperateDeleteException;
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
import me.fengorz.kiwi.word.biz.exception.WordGetOneException;
import me.fengorz.kiwi.word.biz.service.base.*;
import me.fengorz.kiwi.word.biz.service.operate.IWordOperateService;

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

    @Override
    @Transactional(rollbackFor = Exception.class, noRollbackFor = DfsOperateDeleteException.class)
    public boolean removeWord(@KiwiCacheKey String wordName) throws DfsOperateDeleteException {
        List<WordMainDO> list =
            wordMainService.list(Wrappers.<WordMainDO>lambdaQuery().eq(WordMainDO::getWordName, wordName));
        if (KiwiCollectionUtils.isEmpty(list)) {
            Integer wordId = wordMainVariantService.getWordId(wordName);
            if (wordId != null) {
                WordMainDO wordMainDO = wordMainService.getById(wordId);
                list.add(wordMainDO);
            }
        }

        if (KiwiCollectionUtils.isEmpty(list)) {
            return false;
        }

        for (WordMainDO wordMainDO : list) {
            if (wordMainDO == null) {
                return false;
            }
            subRemoveWord(wordMainDO);
        }
        return true;
    }

    @Transactional(rollbackFor = Exception.class, noRollbackFor = DfsOperateDeleteException.class,
        propagation = Propagation.REQUIRES_NEW)
    private void subRemoveWord(WordMainDO wordMainDO) throws DfsOperateDeleteException {
        final String wordName = wordMainDO.getWordName();
        wordMainService.remove(Wrappers.<WordMainDO>lambdaQuery().eq(WordMainDO::getWordName, wordName));
        this.cachePutFetchReplace(wordName,
            this.cacheGetFetchReplace(wordName).setOldRelWordId(wordMainDO.getWordId()));
        this.evict(wordName);
        wordMainService.evictByName(wordName);
        wordMainService.evictById(wordMainDO.getWordId());
        // 这里缓存的删除要在Mysql的删除之前做
        this.evict(wordName);
        this.removeWordRelatedData(wordMainDO);
    }

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
    public WordQueryVO queryWord(@KiwiCacheKey String wordName) throws DfsOperateDeleteException {
        WordQueryVO wordQueryVO = new WordQueryVO();
        WordMainDO word = null;
        try {
            word = wordMainService.getOne(wordName);
        } catch (WordGetOneException e) {
            this.removeWord(wordName);
        }
        // if you can't find the result after the tense is determined, insert a record into the queue to be fetched
        if (word == null) {
            Integer sourceWordId = wordMainVariantService.getWordId(wordName);
            if (sourceWordId == null) {
                // 异步爬虫抓取，插入队列表
                wordFetchQueueService.asyncFetchNewWord(wordName);
            } else {
                final String name = wordMainService.getWordName(sourceWordId);
                if (KiwiStringUtils.isNotBlank(name)) {
                    return this.queryWord(name);
                }
            }
        }

        KiwiAssertUtils.resourceNotNull(word, "No results for [{}]!", wordName);

        wordQueryVO.setWordId(word.getWordId());
        wordQueryVO.setWordName(word.getWordName());
        wordQueryVO.setIsCollect(CommonConstants.FLAG_N);

        Integer wordId = word.getWordId();
        wordQueryVO.setWordCharacterVOList(assembleWordCharacterVOS(wordName, wordId));
        return wordQueryVO;
    }

    private List<WordCharacterVO> assembleWordCharacterVOS(String wordName, Integer wordId) throws ServiceException {
        List<WordCharacterDO> wordCharacterList =
            wordCharacterService.list(new QueryWrapper<>(new WordCharacterDO().setWordId(wordId)));
        KiwiAssertUtils.serviceNotEmpty(wordCharacterList, "No character for [{}]!", wordName);

        List<WordCharacterVO> wordCharacterVOList = new ArrayList<>();
        for (WordCharacterDO wordCharacter : wordCharacterList) {
            WordCharacterVO wordCharacterVO = new WordCharacterVO();
            BeanUtil.copyProperties(wordCharacter, wordCharacterVO);
            wordCharacterVOList.add(wordCharacterVO);

            List<WordParaphraseVO> wordParaphraseVOList = assembleWordParaphraseVOS(wordCharacter.getCharacterId());
            if (wordParaphraseVOList == null) {
                continue;
            }
            wordCharacterVO.setWordParaphraseVOList(wordParaphraseVOList);

            List<WordPronunciationVO> wordPronunciationVOList = assembleWordPronunciationVOS(wordCharacter);
            if (wordPronunciationVOList == null) {
                continue;
            }
            wordCharacterVO.setWordPronunciationVOList(wordPronunciationVOList);
        }
        return wordCharacterVOList;
    }

    private List<WordPronunciationVO> assembleWordPronunciationVOS(WordCharacterDO wordCharacter) {
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

    private List<WordParaphraseVO> assembleWordParaphraseVOS(Integer characterId) {
        List<WordParaphraseVO> wordParaphraseVOList = new ArrayList<>();
        List<WordParaphraseDO> wordParaphraseDOList =
            wordParaphraseService.list(new QueryWrapper<>(new WordParaphraseDO().setCharacterId(characterId)));
        if (CollUtil.isEmpty(wordParaphraseDOList)) {
            return wordParaphraseVOList;
        }

        for (WordParaphraseDO wordParaphraseDO : wordParaphraseDOList) {
            WordParaphraseVO wordParaphraseVO = new WordParaphraseVO();
            BeanUtil.copyProperties(wordParaphraseDO, wordParaphraseVO);

            if (1 == wordParaphraseDO.getIsHavePhrase()) {
                List<String> phraseList = new ArrayList<>();
                List<WordParaphrasePhraseDO> phraseDOList =
                    wordParaphrasePhraseService.list(Wrappers.<WordParaphrasePhraseDO>lambdaQuery()
                        .eq(WordParaphrasePhraseDO::getParaphraseId, wordParaphraseDO.getParaphraseId())
                        .eq(WordParaphrasePhraseDO::getIsValid, CommonConstants.FLAG_YES));
                if (KiwiCollectionUtils.isNotEmpty(phraseDOList)) {
                    for (WordParaphrasePhraseDO phraseDO : phraseDOList) {
                        phraseList.add(phraseDO.getPhrase());
                    }
                }
                wordParaphraseVO.setPhraseList(phraseList);
            }

            wordParaphraseVOList.add(wordParaphraseVO);

            List<WordParaphraseExampleVO> wordParaphraseExampleVOList =
                assembleWordParaphraseExampleVOS(wordParaphraseVO.getParaphraseId());
            if (wordParaphraseExampleVOList == null) {
                continue;
            }
            wordParaphraseVO.setWordParaphraseExampleVOList(wordParaphraseExampleVOList);
        }
        return wordParaphraseVOList;
    }

    private List<WordParaphraseExampleVO> assembleWordParaphraseExampleVOS(Integer paraphraseId) {
        List<WordParaphraseExampleVO> wordParaphraseExampleVOList = new ArrayList<>();
        List<WordParaphraseExampleDO> wordParaphraseExampleDOList = wordParaphraseExampleService
            .list(new QueryWrapper<>(new WordParaphraseExampleDO().setParaphraseId(paraphraseId)));
        if (CollUtil.isEmpty(wordParaphraseExampleDOList)) {
            return null;
        }
        for (WordParaphraseExampleDO wordParaphraseExampleDO : wordParaphraseExampleDOList) {
            WordParaphraseExampleVO wordParaphraseExampleVO = new WordParaphraseExampleVO();
            BeanUtil.copyProperties(wordParaphraseExampleDO, wordParaphraseExampleVO);
            wordParaphraseExampleVOList.add(wordParaphraseExampleVO);
        }
        return wordParaphraseExampleVOList;
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
        WordParaphraseVO wordParaphraseVO = new WordParaphraseVO();
        List<WordParaphraseExampleVO> wordParaphraseExampleVOList = new ArrayList<>();
        WordParaphraseDO wordParaphraseDO = wordParaphraseService.getById(paraphraseId);
        BeanUtil.copyProperties(wordParaphraseDO, wordParaphraseVO);
        List<WordParaphraseExampleDO> exampleDOS =
            wordParaphraseExampleService.list(new LambdaQueryWrapper<WordParaphraseExampleDO>()
                .eq(WordParaphraseExampleDO::getParaphraseId, paraphraseId));
        if (CollUtil.isNotEmpty(exampleDOS)) {
            exampleDOS.forEach(wordParaphraseExampleDO -> {
                WordParaphraseExampleVO exampleVO = new WordParaphraseExampleVO();
                BeanUtil.copyProperties(wordParaphraseExampleDO, exampleVO);
                wordParaphraseExampleVOList.add(exampleVO);
            });
        }
        wordParaphraseVO.setWordParaphraseExampleVOList(wordParaphraseExampleVOList);
        wordParaphraseVO.setWordName(wordMainService.getWordName(wordParaphraseDO.getWordId()));

        WordCharacterVO characterVO = wordCharacterService.getFromCache(wordParaphraseDO.getCharacterId());
        wordParaphraseVO.setWordCharacter(characterVO.getWordCharacter());
        wordParaphraseVO.setWordLabel(characterVO.getWordLabel());

        List<WordPronunciationDO> pronunciationList = wordPronunciationService
            .list(new QueryWrapper<>(new WordPronunciationDO().setCharacterId(characterVO.getCharacterId())));

        if (KiwiCollectionUtils.isNotEmpty(pronunciationList)) {
            List<WordPronunciationVO> pronunciationVOList = new ArrayList<>();
            pronunciationList.forEach(entity -> {
                WordPronunciationVO vo = new WordPronunciationVO();
                BeanUtil.copyProperties(entity, vo);
                pronunciationVOList.add(vo);
            });
            wordParaphraseVO.setWordPronunciationVOList(pronunciationVOList);
        }

        return wordParaphraseVO;
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
    public boolean insertVariant(String inputWordName, String fetchWordName) throws DfsOperateDeleteException {
        if (KiwiStringUtils.equals(inputWordName, fetchWordName)) {
            return false;
        }

        // 先判断变种是否存在，如果不存在再插入
        WordMainVO mainVO = null;
        try {
            mainVO = wordMainService.getOne(fetchWordName);
        } catch (WordGetOneException e) {
            this.removeWord(fetchWordName);
        }
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

    @Transactional(rollbackFor = Exception.class, noRollbackFor = DfsOperateDeleteException.class,
        propagation = Propagation.REQUIRES_NEW)
    private void removeWordRelatedData(WordMainDO wordMainDO) throws DfsOperateDeleteException {
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
                                FetchWordReplaceDTO replaceDTO = this.cacheGetFetchReplace(wordMainDO.getWordName());
                                Map<String, Integer> oldExampleIdMap = replaceDTO.getOldExampleIdMap();
                                oldExampleIdMap.put(wordParaphraseExampleDO.getExampleSentence(),
                                    wordParaphraseExampleDO.getExampleId());
                                this.cachePutFetchReplace(wordMainDO.getWordName(), replaceDTO);
                            }
                            wordParaphraseExampleService.remove(exampleDOLambdaQueryWrapper);
                        }

                        // 将已删除的老的paraphraseId缓存起来，这样可以替换掉收藏本的关联id
                        FetchWordReplaceDTO replaceDTO = this.cacheGetFetchReplace(wordMainDO.getWordName());
                        replaceDTO.getOldParaphraseIdMap().put(wordParaphraseDO.getParaphraseEnglish(), paraphraseId);
                        this.cachePutFetchReplace(wordMainDO.getWordName(), replaceDTO);

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
        QueryWrapper<WordPronunciationDO> wordPronunciationQueryWrapper =
            new QueryWrapper<>(new WordPronunciationDO().setWordId(wordId));
        List<WordPronunciationDO> wordPronunciationList = wordPronunciationService.list(wordPronunciationQueryWrapper);
        wordPronunciationService.remove(wordPronunciationQueryWrapper);

        if (CollUtil.isNotEmpty(wordPronunciationList)) {
            for (WordPronunciationDO wordPronunciation : wordPronunciationList) {
                if (StrUtil.isBlank(wordPronunciation.getVoiceFilePath())) {
                    continue;
                }
                dfsService.deleteFile(wordPronunciation.getGroupName(), wordPronunciation.getVoiceFilePath());
            }
        }
    }

    @Override
    @KiwiCacheKeyPrefix(WordConstants.CACHE_KEY_PREFIX_OPERATE.METHOD_WORD_NAME)
    @CacheEvict(cacheNames = WordConstants.CACHE_NAMES, keyGenerator = CacheConstants.CACHE_KEY_GENERATOR_BEAN)
    public void evict(String wordName) throws DfsOperateDeleteException {
        WordMainVO one = null;
        try {
            one = wordMainService.getOne(wordName);
        } catch (WordGetOneException e) {
            this.removeWord(wordName);
        }
        if (one == null) {
            return;
        }
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
    private void evictParaphrase(@KiwiCacheKey Integer paraphraseId) {}

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
    private void cacheEvictFetchReplace(@KiwiCacheKey String wordName) {}

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
