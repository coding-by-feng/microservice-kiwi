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
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import me.fengorz.kiwi.word.api.common.WordCrawlerConstants;
import me.fengorz.kiwi.word.api.dto.queue.result.FetchWordReplaceDTO;
import me.fengorz.kiwi.word.api.entity.*;
import me.fengorz.kiwi.word.api.request.ParaphraseRequest;
import me.fengorz.kiwi.word.api.vo.ParaphraseExampleVO;
import me.fengorz.kiwi.word.api.vo.WordMainVO;
import me.fengorz.kiwi.word.api.vo.detail.CharacterVO;
import me.fengorz.kiwi.word.api.vo.detail.ParaphraseVO;
import me.fengorz.kiwi.word.api.vo.detail.PronunciationVO;
import me.fengorz.kiwi.word.api.vo.detail.WordQueryVO;
import me.fengorz.kiwi.word.biz.service.base.*;
import me.fengorz.kiwi.word.biz.service.operate.IOperateService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.DocumentOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.SearchOperations;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static org.elasticsearch.index.query.QueryBuilders.idsQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchPhraseQuery;

/**
 * @Description 单词相关业务的复杂逻辑解耦
 * @Author zhanshifeng
 * @Date 2019/11/25 3:13 PM
 */
@Slf4j
@Service
@RequiredArgsConstructor
@KiwiCacheKeyPrefix(WordConstants.CACHE_KEY_PREFIX_OPERATE.CLASS)
public class OperateServiceImpl implements IOperateService {

    private final IWordMainService mainService;
    private final ICharacterService characterService;
    private final IParaphraseService paraphraseService;
    private final IParaphraseExampleService exampleService;
    private final IPronunciationService pronunciationService;
    private final IWordFetchQueueService fetchQueueService;
    private final IWordStarListService wordStarListService;
    private final IParaphraseStarListService paraphraseStarListService;
    private final IExampleStarListService exampleStarListService;
    private final IWordStarRelService wordStarRelService;
    private final IParaphraseStarRelService paraphraseStarRelService;
    private final IWordExampleStarRelService exampleStarRelService;
    private final IWordMainVariantService mainVariantService;
    private final IParaphrasePhraseService phraseService;
    private final IWordReviewService reviewService;
    private final IDfsService dfsService;
    private final SearchOperations searchOperations;
    private final DocumentOperations documentOperations;

    private final static Object barrier = new Object();

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
    public WordQueryVO queryWord(@KiwiCacheKey String wordName, Integer... infoType) {
        WordQueryVO vo = new WordQueryVO();
        WordMainDO word = mainService.getOneAndCatch(wordName, infoType);
        // if you can't find the result after the tense is determined, insert a record into the queue to be fetched
        if (word == null) {
            Integer sourceWordId = mainVariantService.getWordId(wordName);
            if (sourceWordId == null) {
                // 异步爬虫抓取，插入队列表
                fetchQueueService.startFetchOnAsync(wordName);
            } else {
                WordMainDO source = mainService.getById(sourceWordId);
                return this.queryWord(source.getWordName(), source.getInfoType());
            }
        }

        KiwiAssertUtils.resourceNotNull(word, "No results for [{}]!", wordName);

        // 如果是词组的话
        if (word.getInfoType() == WordCrawlerConstants.QUEUE_INFO_TYPE_PHRASE) {
            List<CharacterVO> characterVOList = new LinkedList<>();
            characterVOList.add(new CharacterVO().setCharacterCode(WordConstants.PHRASE_CODE).setCharacterId(0)
                    .setParaphraseVOList(new LinkedList<>()).setPronunciationVOList(new LinkedList<>()));
            Optional.ofNullable(paraphraseService.listPhrase(word.getWordId())).ifPresent(paraphraseVOList -> {
                for (ParaphraseVO paraphraseVO : paraphraseVOList) {
                    paraphraseVO.setExampleVOList(exampleService.listExamples(paraphraseVO.getParaphraseId()));
                    characterVOList.get(0).getParaphraseVOList().add(paraphraseVO);
                }
            });
            vo.setCharacterVOList(characterVOList);
            return vo.setWordName(wordName).setWordId(word.getWordId());
        }

        vo.setWordId(word.getWordId());
        vo.setWordName(word.getWordName());
        vo.setIsCollect(CommonConstants.FLAG_N);

        Integer wordId = word.getWordId();
        vo.setCharacterVOList(assembleWordQueryVO(wordName, wordId));
        this.saveVo2Es(vo);
        return vo;
    }

    private void saveVo2Es(WordQueryVO vo) {
        synchronized (barrier) {
            NativeSearchQuery query = new NativeSearchQueryBuilder()
                    // .withIds() 这个API有坑，查询不生效的，慎用！
                    .withQuery(idsQuery().addIds(vo.getWordId().toString()))
                    .build();
            long count = searchOperations.count(query, WordQueryVO.class);
            if (count == 1) {
                return;
            } else if (count > 1) {
                documentOperations.delete(vo);
            }
            documentOperations.save(vo);
        }
    }

    @Override
    public IPage<WordQueryVO> queryWordByCh(String chineseParaphrase, int current, int size) {
        IPage<WordQueryVO> page = this.queryES(chineseParaphrase, current, size, WordConstants.VO_PATH_MEANING_CHINESE);
        if (KiwiCollectionUtils.isEmpty(page.getRecords())) {
            page = this.queryES(chineseParaphrase, current, size, WordConstants.VO_PATH_EXAMPLE_CHINESE);
        }
        return page;
    }

    private IPage<WordQueryVO> queryES(String chinese, int current, int size, String path) {
        IPage<WordQueryVO> page = new Page<>(current, size);
        NativeSearchQuery query = new NativeSearchQueryBuilder()
                .withQuery(matchPhraseQuery(path, chinese))
                .withPageable(PageRequest.of(current, size))
                .build();
        SearchHits<WordQueryVO> result = searchOperations.search(query, WordQueryVO.class);
        long totalHits = result.getTotalHits();
        page.setTotal(totalHits);
        page.setPages(totalHits % size == 0 ? totalHits / size : (totalHits / size) + 1);
        if (!result.isEmpty()) {
            page.setRecords(result.getSearchHits().stream().map(SearchHit::getContent).collect(Collectors.toList()));
            return page;
        }
        return page;
    }

    private List<CharacterVO> assembleWordQueryVO(String wordName, Integer wordId) throws ServiceException {
        List<CharacterDO> characterList =
                characterService.list(new QueryWrapper<>(new CharacterDO().setWordId(wordId)));
        KiwiAssertUtils.serviceNotEmpty(characterList, "No character for [{}]!", wordName);

        List<CharacterVO> characterVOList = new ArrayList<>();
        for (CharacterDO character : characterList) {
            CharacterVO characterVO = new CharacterVO();
            BeanUtil.copyProperties(character, characterVO);
            characterVOList.add(characterVO);

            List<ParaphraseVO> paraphraseVOList = assembleParaphraseVOList(character.getCharacterId());
            if (KiwiCollectionUtils.isEmpty(paraphraseVOList)) {
                continue;
            }
            characterVO.setParaphraseVOList(paraphraseVOList);

            List<PronunciationVO> pronunciationVOList = assemblePronunciationVOList(character);
            if (KiwiCollectionUtils.isEmpty(pronunciationVOList)) {
                continue;
            }
            characterVO.setPronunciationVOList(pronunciationVOList);
        }
        return characterVOList;
    }

    private List<PronunciationVO> assemblePronunciationVOList(CharacterDO wordCharacter) {
        List<PronunciationDO> wordPronunciationList = pronunciationService
                .list(new QueryWrapper<>(new PronunciationDO().setCharacterId(wordCharacter.getCharacterId())));
        // 单词发音音频文件传给pronunciationId给前端，让前端再次调用接口下载文件流
        if (CollUtil.isEmpty(wordPronunciationList)) {
            return null;
        }

        List<PronunciationVO> pronunciationVOList = new ArrayList<>();

        for (PronunciationDO wordPronunciation : wordPronunciationList) {
            PronunciationVO pronunciationVO = new PronunciationVO();
            BeanUtil.copyProperties(wordPronunciation, pronunciationVO);
            pronunciationVOList.add(pronunciationVO);
        }
        return pronunciationVOList;
    }

    private List<ParaphraseVO> assembleParaphraseVOList(Integer characterId) {
        List<ParaphraseVO> paraphraseVOList = new ArrayList<>();
        List<ParaphraseDO> paraphraseDOList =
                paraphraseService.list(new QueryWrapper<>(new ParaphraseDO().setCharacterId(characterId)));
        if (CollUtil.isEmpty(paraphraseDOList)) {
            return paraphraseVOList;
        }

        for (ParaphraseDO paraphrase : paraphraseDOList) {
            ParaphraseVO vo = new ParaphraseVO();
            BeanUtil.copyProperties(paraphrase, vo);

            if (1 == paraphrase.getIsHavePhrase()) {
                List<String> phraseList = new ArrayList<>();
                List<ParaphrasePhraseDO> phraseDOList =
                        phraseService.list(Wrappers.<ParaphrasePhraseDO>lambdaQuery()
                                .eq(ParaphrasePhraseDO::getParaphraseId, paraphrase.getParaphraseId())
                                .eq(ParaphrasePhraseDO::getIsValid, CommonConstants.FLAG_YES));
                if (KiwiCollectionUtils.isNotEmpty(phraseDOList)) {
                    for (ParaphrasePhraseDO phraseDO : phraseDOList) {
                        phraseList.add(phraseDO.getPhrase());
                    }
                }
                vo.setPhraseList(phraseList);
            }

            paraphraseVOList.add(vo);

            List<ParaphraseExampleVO> exampleVOList =
                    assembleExampleVOList(vo.getParaphraseId());
            if (exampleVOList == null) {
                continue;
            }
            vo.setExampleVOList(exampleVOList);
        }
        return paraphraseVOList;
    }

    private List<ParaphraseExampleVO> assembleExampleVOList(Integer paraphraseId) {
        List<ParaphraseExampleVO> ExampleVOList = new ArrayList<>();
        List<ParaphraseExampleDO> exampleDOList = exampleService
                .list(new QueryWrapper<>(new ParaphraseExampleDO().setParaphraseId(paraphraseId)));
        if (CollUtil.isEmpty(exampleDOList)) {
            return null;
        }
        for (ParaphraseExampleDO example : exampleDOList) {
            ParaphraseExampleVO vo = new ParaphraseExampleVO();
            BeanUtil.copyProperties(example, vo);
            ExampleVOList.add(vo);
        }
        return ExampleVOList;
    }

    @Override
    @KiwiCacheKeyPrefix(WordConstants.CACHE_KEY_PREFIX_OPERATE.METHOD_PARAPHRASE_ID)
    @Cacheable(cacheNames = WordConstants.CACHE_NAMES, keyGenerator = CacheConstants.CACHE_KEY_GENERATOR_BEAN,
            unless = "#result == null")
    public ParaphraseVO findParaphraseVO(@KiwiCacheKey Integer paraphraseId) {
        ParaphraseVO vo = new ParaphraseVO();
        List<ParaphraseExampleVO> exampleVOList = new ArrayList<>();
        ParaphraseDO paraphrase = paraphraseService.getById(paraphraseId);
        BeanUtil.copyProperties(paraphrase, vo);
        List<ParaphraseExampleDO> exampleDOList =
                exampleService.list(new LambdaQueryWrapper<ParaphraseExampleDO>()
                        .eq(ParaphraseExampleDO::getParaphraseId, paraphraseId));
        if (CollUtil.isNotEmpty(exampleDOList)) {
            exampleDOList.forEach(wordParaphraseExampleDO -> {
                ParaphraseExampleVO exampleVO = new ParaphraseExampleVO();
                BeanUtil.copyProperties(wordParaphraseExampleDO, exampleVO);
                exampleVOList.add(exampleVO);
            });
        }
        vo.setExampleVOList(exampleVOList);
        vo.setWordName(mainService.getWordName(paraphrase.getWordId()));
        vo.setCodes(paraphrase.getCodes());

        CharacterVO characterVO = characterService.get(paraphrase.getCharacterId());
        if (characterVO != null) {
            vo.setWordCharacter(characterVO.getCharacterCode());
            vo.setWordLabel(characterVO.getTag());
        } else {
            vo.setWordCharacter(WordConstants.PHRASE_CODE);
        }

        List<ParaphrasePhraseDO> phraseList = phraseService.list(Wrappers.<ParaphrasePhraseDO>lambdaQuery().eq(ParaphrasePhraseDO::getParaphraseId, paraphrase.getParaphraseId())
                .eq(ParaphrasePhraseDO::getIsValid, CommonConstants.FLAG_YES));
        if (KiwiCollectionUtils.isNotEmpty(phraseList)) {
            vo.setPhraseList(phraseList.stream().map(ParaphrasePhraseDO::getPhrase).collect(Collectors.toList()));
        }

        if (characterVO == null) {
            return vo.setPronunciationVOList(new LinkedList<>());
        }
        List<PronunciationDO> pronunciationList = pronunciationService
                .list(new QueryWrapper<>(new PronunciationDO().setCharacterId(characterVO.getCharacterId())));

        if (KiwiCollectionUtils.isNotEmpty(pronunciationList)) {
            List<PronunciationVO> pronunciationVOList = new ArrayList<>();
            pronunciationList.forEach(entity -> {
                PronunciationVO pronunciationVO = new PronunciationVO();
                BeanUtil.copyProperties(entity, pronunciationVO);
                pronunciationVOList.add(pronunciationVO);

                if (entity.getSoundmark().length() > WordConstants.SOUND_MARK_OVERLENGTH_THRESHOLD) {
                    vo.setIsOverlength(true);
                }
            });
            vo.setPronunciationVOList(pronunciationVOList);
        }

        return vo;
    }

    @Override
    public boolean modifyMeaningChinese(ParaphraseRequest request) {
        evictParaphrase(request.getParaphraseId());
        paraphraseService.modifyMeaningChinese(request);
        return false;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean insertVariant(String inputWordName, String fetchWordName) {
        if (KiwiStringUtils.equals(inputWordName, fetchWordName)) {
            return false;
        }

        // 先判断变种是否存在，如果存在再插入
        WordMainVO mainVO = mainService.getOneAndCatch(fetchWordName);
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
        List<ParaphraseDO> list = paraphraseService
                .list(Wrappers.<ParaphraseDO>lambdaQuery().eq(ParaphraseDO::getWordId, one.getWordId())
                        .eq(ParaphraseDO::getIsDel, CommonConstants.FLAG_DEL_NO));
        if (KiwiCollectionUtils.isNotEmpty(list)) {
            for (ParaphraseDO paraphraseDO : list) {
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

    @Override
    public Set<WordMainDO> collectDirtyData(Integer queueId, String wordName) {
        Set<WordMainDO> set = new HashSet<>(mainService.list(Wrappers.<WordMainDO>lambdaQuery().eq(WordMainDO::getWordName, wordName)));
        if (KiwiCollectionUtils.isEmpty(set)) {
            Optional.ofNullable(mainService.listDirtyData(fetchQueueService.getOneAnyhow(queueId).getWordId())).ifPresent(set::addAll);
            // 防止有脏数据的队列表wordId是0
            // 如果是单词变种的情况
            List<Integer> wordIds = mainVariantService.list(Wrappers.<WordMainVariantDO>lambdaQuery().eq(WordMainVariantDO::getVariantName, wordName)).stream().map(WordMainVariantDO::getWordId).collect(Collectors.toList());
            if (KiwiCollectionUtils.isNotEmpty(wordIds)) {
                set.addAll(mainService.list(Wrappers.<WordMainDO>lambdaQuery().in(WordMainDO::getWordId, wordIds)));
            }
        }
        return set;
    }

    @Override
    public Integer getReviewBreakpointPageNumber(Integer listId) {
        List<WordBreakpointReviewDO> list = reviewService.listBreakpointReview(listId);
        if (KiwiCollectionUtils.isEmpty(list)) {
            return 0;
        } else {
            return list.get(0).getLastPage();
        }
    }

}
