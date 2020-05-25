/*
 *
 *   Copyright [2019~2025] [codingByFeng]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 *
 */

package me.fengorz.kiwi.word.biz.service.operate.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.HttpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import me.fengorz.kiwi.common.api.annotation.cache.KiwiCacheKey;
import me.fengorz.kiwi.common.api.annotation.cache.KiwiCacheKeyPrefix;
import me.fengorz.kiwi.common.api.constant.CacheConstants;
import me.fengorz.kiwi.common.api.constant.CommonConstants;
import me.fengorz.kiwi.common.api.exception.ResourceNotFoundException;
import me.fengorz.kiwi.common.api.exception.ServiceException;
import me.fengorz.kiwi.common.api.exception.dfs.DfsOperateDeleteException;
import me.fengorz.kiwi.common.api.exception.dfs.DfsOperateException;
import me.fengorz.kiwi.common.fastdfs.service.IDfsService;
import me.fengorz.kiwi.common.sdk.util.lang.string.KiwiStringUtils;
import me.fengorz.kiwi.common.sdk.util.validate.KiwiAssertUtils;
import me.fengorz.kiwi.word.api.common.WordConstants;
import me.fengorz.kiwi.word.api.common.WordCrawlerConstants;
import me.fengorz.kiwi.word.api.dto.fetch.FetchParaphraseDTO;
import me.fengorz.kiwi.word.api.dto.fetch.FetchWordCodeDTO;
import me.fengorz.kiwi.word.api.dto.fetch.FetchWordPronunciationDTO;
import me.fengorz.kiwi.word.api.dto.fetch.FetchWordResultDTO;
import me.fengorz.kiwi.word.api.entity.*;
import me.fengorz.kiwi.word.api.exception.WordResultStoreRuntimeException;
import me.fengorz.kiwi.word.api.factory.CrawlerEntityFactory;
import me.fengorz.kiwi.word.api.util.CrawlerUtils;
import me.fengorz.kiwi.word.api.vo.*;
import me.fengorz.kiwi.word.biz.service.*;
import me.fengorz.kiwi.word.biz.service.operate.IWordOperateService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @Description 单词相关业务的复杂逻辑解耦
 * @Author ZhanShiFeng
 * @Date 2019/11/25 3:13 PM
 */
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
    private final IDfsService dfsService;

    @Value("${me.fengorz.file.crawler.voice.tmpPath}")
    private String crawlerVoiceBasePath;

    @Override
    @Transactional(rollbackFor = Exception.class, noRollbackFor = DfsOperateDeleteException.class)
    public boolean removeWord(String wordName) throws DfsOperateDeleteException {
        WordMainDO wordMainDO = wordMainService.getOne(wordName);
        if (wordMainDO == null) {
            return false;
        }
        this.removeWordRelatedData(wordMainDO);
        wordMainService.removeById(wordMainDO.getWordId());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class, noRollbackFor = DfsOperateDeleteException.class)
    public boolean storeFetchWordResult(FetchWordResultDTO fetchWordResultDTO) throws WordResultStoreRuntimeException, DfsOperateException {
        final String wordName = fetchWordResultDTO.getWordName();

        WordMainDO wordMainDO = new WordMainDO();
        wordMainDO.setWordName(wordName);

        // If the word already exists, update the original word information
        WordMainDO existsWordMainDO = wordMainService.getOne(new QueryWrapper<>(wordMainDO));
        try {
            if (existsWordMainDO != null) {

                wordMainDO.setLastUpdateTime(LocalDateTime.now());
                wordMainDO.setIsDel(CommonConstants.FLAG_N);
                wordMainService.update(wordMainDO, new QueryWrapper<>(new WordMainDO().setWordName(wordMainDO.getWordName())));
                wordMainDO = wordMainService.getOne(new QueryWrapper<>(new WordMainDO().setWordName(wordMainDO.getWordName())));

                removeWordRelatedData(wordMainDO);
            } else {
                wordMainDO.setIsDel(CommonConstants.FLAG_N);
                wordMainService.save(wordMainDO);
            }
        } catch (DfsOperateDeleteException e) {
            throw e;
        } finally {
            subStoreFetchWordResult(fetchWordResultDTO, wordMainDO);
            this.evict(wordName);
        }

        return true;
    }

    private void subStoreFetchWordResult(FetchWordResultDTO fetchWordResultDTO, WordMainDO wordMainDO) throws DfsOperateException {
        Integer wordId = wordMainDO.getWordId();

        if (wordId == null) {
            throw new WordResultStoreRuntimeException("wordId is not null!");
        }

        List<FetchWordCodeDTO> fetchWordCodeDTOList = fetchWordResultDTO.getFetchWordCodeDTOList();
        if (CollUtil.isNotEmpty(fetchWordCodeDTOList)) {
            for (FetchWordCodeDTO fetchWordCodeDTO : fetchWordCodeDTOList) {
                WordCharacterDO wordCharacter = CrawlerEntityFactory.initWordCharacter(fetchWordCodeDTO.getCode(), fetchWordCodeDTO.getLabel(), wordId);
                wordCharacterService.save(wordCharacter);
                Integer characterId = wordCharacter.getCharacterId();

                List<FetchParaphraseDTO> fetchParaphraseDTOList = fetchWordCodeDTO.getFetchParaphraseDTOList();
                fetchParaphraseDTOList.forEach(fetchParaphraseDTO -> {

                    WordParaphraseDO wordParaphraseDO = CrawlerEntityFactory.initWordParaphrase(characterId, wordId, fetchParaphraseDTO.getMeaningChinese(), fetchParaphraseDTO.getParaphraseEnglish(), fetchParaphraseDTO.getTranslateLanguage());
                    wordParaphraseService.save(wordParaphraseDO);
                    Integer paraphraseId = wordParaphraseDO.getParaphraseId();

                    Optional.ofNullable(fetchParaphraseDTO.getFetchParaphraseExampleDTOList()).ifPresent(fetchParaphraseExampleDTOS -> fetchParaphraseExampleDTOS.forEach(fetchParaphraseExampleDTO -> {
                        WordParaphraseExampleDO wordParaphraseExampleDO = CrawlerEntityFactory.initWordParaphraseExample(paraphraseId, wordId, fetchParaphraseExampleDTO.getExampleSentence(), fetchParaphraseExampleDTO.getExampleTranslate(), fetchParaphraseExampleDTO.getTranslateLanguage());
                        wordParaphraseExampleService.save(wordParaphraseExampleDO);
                    }));
                });

                // save pronunciation and voice's file
                List<FetchWordPronunciationDTO> fetchWordPronunciationDTOList = fetchWordCodeDTO.getFetchWordPronunciationDTOList();
                if (CollUtil.isNotEmpty(fetchWordPronunciationDTOList)) {
                    for (FetchWordPronunciationDTO fetchWordPronunciationDTO : fetchWordPronunciationDTOList) {
                        String voiceFileUrl = WordCrawlerConstants.CAMBRIDGE_BASE_URL + fetchWordPronunciationDTO.getVoiceFileUrl();
                        long voiceSize = HttpUtil.downloadFile(URLUtil.decode(voiceFileUrl), FileUtil.file(crawlerVoiceBasePath));
                        String tempVoice = crawlerVoiceBasePath + CrawlerUtils.getVoiceFileName(voiceFileUrl);
                        String uploadResult = dfsService.uploadFile(FileUtil.getInputStream(tempVoice), voiceSize, WordCrawlerConstants.EXT_OGG);
                        WordPronunciationDO wordPronunciation = CrawlerEntityFactory.initWordPronunciation(wordId, characterId, uploadResult,
                                fetchWordPronunciationDTO.getSoundmark(), fetchWordPronunciationDTO.getSoundmarkType());
                        wordPronunciationService.save(wordPronunciation);
                    }
                }
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void dfsDeleteExceptionBackCall(String wordName) {
        WordFetchQueueDO wordFetchQueue = new WordFetchQueueDO();
        wordFetchQueue.setFetchStatus(WordCrawlerConstants.STATUS_ERROR_DFS_OPERATE_DELETE_FAILED).setFetchResult("delete pronunciation voice file error");
        wordFetchQueueService.update(wordFetchQueue, new QueryWrapper<>(new WordFetchQueueDO().setWordName(wordName)));

        // If you fail to delete the pronunciation file, leave the pronunciation file path blank to prevent the same exception from being thrown again
        wordPronunciationService.deleteByWordName(wordName);
    }

    /**
     * 封装返回给前端的整个单词的数据DTO
     *
     * @param wordName
     * @return
     */
    @Override
    @KiwiCacheKeyPrefix(WordConstants.CACHE_KEY_PREFIX_OPERATE.METHOD_WORD_NAME)
    @Cacheable(cacheNames = WordConstants.CACHE_NAMES, keyGenerator = CacheConstants.CACHE_KEY_GENERATOR_BEAN, unless = "#result == null")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public WordQueryVO queryWord(@KiwiCacheKey String wordName) {
        WordQueryVO wordQueryVO = new WordQueryVO();
        WordMainDO word = wordMainService.getOne(wordName);
        // if you can't find the result after the tense is determined, insert a record into the queue to be fetched
        if (word == null) {
            // TODO ZSF 查询单词变种表
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

    @KiwiCacheKeyPrefix(WordConstants.CACHE_KEY_PREFIX_OPERATE.METHOD_WORD_NAME)
    @CacheEvict(cacheNames = WordConstants.CACHE_NAMES, keyGenerator = CacheConstants.CACHE_KEY_GENERATOR_BEAN)
    private void evict(@KiwiCacheKey String wordName) {
    }

    private List<WordCharacterVO> assembleWordCharacterVOS(String wordName, Integer wordId) throws ServiceException {
        List<WordCharacterDO> wordCharacterList = wordCharacterService.list(new QueryWrapper<>(new WordCharacterDO().setWordId(wordId)));
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
        List<WordPronunciationDO> wordPronunciationList = wordPronunciationService.list(new QueryWrapper<>(new WordPronunciationDO().setCharacterId(wordCharacter.getCharacterId())));
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
        List<WordParaphraseDO> wordParaphraseDOList = wordParaphraseService.list(new QueryWrapper<>(new WordParaphraseDO().setCharacterId(characterId)));
        if (CollUtil.isEmpty(wordParaphraseDOList)) {
            return wordParaphraseVOList;
        }

        for (WordParaphraseDO wordParaphraseDO : wordParaphraseDOList) {
            WordParaphraseVO wordParaphraseVO = new WordParaphraseVO();
            BeanUtil.copyProperties(wordParaphraseDO, wordParaphraseVO);
            wordParaphraseVOList.add(wordParaphraseVO);

            List<WordParaphraseExampleVO> wordParaphraseExampleVOList = assembleWordParaphraseExampleVOS(wordParaphraseVO.getParaphraseId());
            if (wordParaphraseExampleVOList == null) {
                continue;
            }
            wordParaphraseVO.setWordParaphraseExampleVOList(wordParaphraseExampleVOList);
        }
        return wordParaphraseVOList;
    }

    private List<WordParaphraseExampleVO> assembleWordParaphraseExampleVOS(Integer paraphraseId) {
        List<WordParaphraseExampleVO> wordParaphraseExampleVOList = new ArrayList<>();
        List<WordParaphraseExampleDO> wordParaphraseExampleDOList = wordParaphraseExampleService.list(new QueryWrapper<>(new WordParaphraseExampleDO().setParaphraseId(paraphraseId)));
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
        LambdaQueryWrapper<WordStarRelDO> queryWrapper = new LambdaQueryWrapper<WordStarRelDO>().eq(WordStarRelDO::getListId, listId)
                .eq(WordStarRelDO::getWordId, wordId);
        int count = wordStarRelService.count(queryWrapper);
        KiwiAssertUtils.serviceEmpty(count, "wordStar is exists!");
        return wordStarRelService.save(
                new WordStarRelDO()
                        .setListId(listId)
                        .setWordId(wordId)
        );
    }

    @Override
    public boolean removeWordStarList(Integer wordId, Integer listId) {
        LambdaQueryWrapper<WordStarRelDO> queryWrapper = new LambdaQueryWrapper<WordStarRelDO>().eq(WordStarRelDO::getListId, listId)
                .eq(WordStarRelDO::getWordId, wordId);
        int count = wordStarRelService.count(queryWrapper);
        KiwiAssertUtils.serviceNotEmpty(count, "wordStar is not exists!");
        return wordStarRelService.remove(queryWrapper);
    }

    @Override
    public boolean putParaphraseIntoStarList(Integer paraphraseId, Integer listId) {
        KiwiAssertUtils.serviceNotEmpty(
                wordParaphraseService.countById(paraphraseId), "paraphrase is not exists!");
        KiwiAssertUtils.serviceNotEmpty(
                wordParaphraseStarListService.countById(listId), "paraphraseStarList is not exists!");
        LambdaQueryWrapper<WordParaphraseStarRelDO> wrapper = new LambdaQueryWrapper<WordParaphraseStarRelDO>().eq(WordParaphraseStarRelDO::getListId, listId).eq(WordParaphraseStarRelDO::getParaphraseId, paraphraseId);
        KiwiAssertUtils.serviceEmpty(wordParaphraseStarRelService.count(wrapper), "paraphrase already exists!");
        return wordParaphraseStarRelService.save(
                new WordParaphraseStarRelDO()
                        .setListId(listId)
                        .setParaphraseId(paraphraseId)
        );
    }

    @Override
    public boolean putExampleIntoStarList(Integer exampleId, Integer listId) {
        LambdaQueryWrapper<WordExampleStarRelDO> queryWrapper = new LambdaQueryWrapper<WordExampleStarRelDO>().eq(WordExampleStarRelDO::getListId, listId).eq(WordExampleStarRelDO::getExampleId, exampleId);
        KiwiAssertUtils.serviceEmpty(wordExampleStarRelService.count(queryWrapper), "example rel is already exists!");
        return wordExampleStarRelService.save(
                new WordExampleStarRelDO()
                        .setListId(listId)
                        .setExampleId(exampleId)
        );
    }

    @Override
    public WordCharacterVO getByParaphraseId(Integer paraphraseId) {
        WordParaphraseDO paraphrase = wordParaphraseService.getOne(
                new QueryWrapper<>(
                        new WordParaphraseDO()
                                .setParaphraseId(paraphraseId)
                )
        );
        KiwiAssertUtils.serviceNotNull(paraphrase, "paraphrase[{}] is not exists!", paraphraseId);

        final Integer characterId = paraphrase.getCharacterId();
        WordCharacterDO character = wordCharacterService.getOne(
                new QueryWrapper<>(
                        new WordCharacterDO()
                                .setCharacterId(characterId)
                )
        );
        KiwiAssertUtils.serviceNotNull(character, "character[{}] is not exists!", characterId);

        List<WordParaphraseVO> paraphraseVOList = new ArrayList<>();
        WordParaphraseVO wordParaphraseVO = new WordParaphraseVO();
        BeanUtil.copyProperties(paraphrase, wordParaphraseVO);
        paraphraseVOList.add(wordParaphraseVO);
        WordCharacterVO wordCharacterVO = new WordCharacterVO().setWordCharacter(character.getWordCharacter())
                .setWordLabel(character.getWordLabel())
                .setWordParaphraseVOList(paraphraseVOList);

        List<WordPronunciationDO> pronunciationList = wordPronunciationService.list(
                new QueryWrapper<>(
                        new WordPronunciationDO()
                                .setCharacterId(characterId)
                )
        );

        if (CollUtil.isNotEmpty(pronunciationList)) {
            List<WordPronunciationVO> pronunciationVOList = new ArrayList<>();
            pronunciationList.forEach(entity -> {
                WordPronunciationVO vo = new WordPronunciationVO();
                BeanUtil.copyProperties(entity, vo);
                pronunciationVOList.add(vo);
            });
            wordCharacterVO.setWordPronunciationVOList(pronunciationVOList);
        }

        return wordCharacterVO;
    }

    @Override
    public boolean removeExampleStar(Integer exampleId, Integer listId) {
        LambdaQueryWrapper<WordExampleStarRelDO> queryWrapper = new LambdaQueryWrapper<WordExampleStarRelDO>().eq(WordExampleStarRelDO::getListId, listId).eq(WordExampleStarRelDO::getExampleId, exampleId);
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
        //先判断变种是否存在，如果不存在再插入
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

    @Transactional(rollbackFor = Exception.class, noRollbackFor = DfsOperateDeleteException.class)
    private void removeWordRelatedData(WordMainDO wordMainDO) throws DfsOperateDeleteException {
        Integer wordId = wordMainDO.getWordId();

        wordMainVariantService.delByWordId(wordId);

        QueryWrapper<WordCharacterDO> wordCharacterQueryWrapper = new QueryWrapper<>(new WordCharacterDO().setWordId(wordId));
        List<WordCharacterDO> characterList = wordCharacterService.list(wordCharacterQueryWrapper);
        if (CollUtil.isNotEmpty(characterList)) {
            for (WordCharacterDO wordCharacter : characterList) {
                Integer characterId = wordCharacter.getCharacterId();
                QueryWrapper<WordParaphraseDO> wordParaphraseQueryWrapper = new QueryWrapper<>(new WordParaphraseDO().setCharacterId(characterId));
                List<WordParaphraseDO> paraphraseList = wordParaphraseService.list(wordParaphraseQueryWrapper);
                if (CollUtil.isNotEmpty(paraphraseList)) {
                    for (WordParaphraseDO wordParaphraseDO : paraphraseList) {
                        Integer paraphraseId = wordParaphraseDO.getParaphraseId();
                        wordParaphraseExampleService.remove(new QueryWrapper<>(new WordParaphraseExampleDO().setParaphraseId(paraphraseId)));
                    }
                }
                if (CollUtil.isNotEmpty(paraphraseList)) {
                    wordParaphraseService.remove(wordParaphraseQueryWrapper);
                }
            }
            wordCharacterService.remove(wordCharacterQueryWrapper);
        }

        // 删除分布式文件系统里面的文件
        QueryWrapper<WordPronunciationDO> wordPronunciationQueryWrapper = new QueryWrapper<>(new WordPronunciationDO().setWordId(wordId));
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


}
