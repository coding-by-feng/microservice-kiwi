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

package me.fengorz.kiwi.word.biz.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.HttpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.AllArgsConstructor;
import me.fengorz.kiwi.common.api.constant.CommonConstants;
import me.fengorz.kiwi.common.api.exception.ServiceException;
import me.fengorz.kiwi.common.fastdfs.component.DfsService;
import me.fengorz.kiwi.common.fastdfs.exception.DfsOperateDeleteException;
import me.fengorz.kiwi.common.fastdfs.exception.DfsOperateException;
import me.fengorz.kiwi.common.sdk.CommonUtils;
import me.fengorz.kiwi.common.sdk.web.security.SecurityUtils;
import me.fengorz.kiwi.common.sdk.util.validate.EnhancedAssertUtils;
import me.fengorz.kiwi.word.api.common.CrawlerConstants;
import me.fengorz.kiwi.word.api.dto.fetch.FetchParaphraseDTO;
import me.fengorz.kiwi.word.api.dto.fetch.FetchWordCodeDTO;
import me.fengorz.kiwi.word.api.dto.fetch.FetchWordPronunciationDTO;
import me.fengorz.kiwi.word.api.dto.fetch.FetchWordResultDTO;
import me.fengorz.kiwi.word.api.entity.*;
import me.fengorz.kiwi.word.api.exception.WordResultStoreException;
import me.fengorz.kiwi.word.api.factory.CrawlerEntityFactory;
import me.fengorz.kiwi.word.api.util.CrawlerUtils;
import me.fengorz.kiwi.word.api.vo.*;
import me.fengorz.kiwi.word.biz.service.*;
import me.fengorz.kiwi.word.biz.service.operate.IWordOperateService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @Description 单词相关业务的复杂逻辑解耦
 * @Author zhanshifeng
 * @Date 2019/11/25 3:13 PM
 */
@Service
@AllArgsConstructor
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
    private final DfsService dfsService;

    @Override
    @Transactional(rollbackFor = Exception.class, noRollbackFor = DfsOperateDeleteException.class)
    public boolean storeFetchWordResult(FetchWordResultDTO fetchWordResultDTO) throws WordResultStoreException, DfsOperateException, DfsOperateDeleteException {
        final String wordName = fetchWordResultDTO.getWordName();

        WordMainDO wordMainDO = new WordMainDO();
        wordMainDO.setWordName(wordName);

        // If the word already exists, update the original word information
        WordMainDO existsWordMainDO = wordMainService.getOne(new QueryWrapper<>(wordMainDO));
        if (existsWordMainDO != null) {

            wordMainDO.setLastUpdateTime(LocalDateTime.now());
            wordMainDO.setIsDel(CommonConstants.FALSE);
            wordMainService.update(wordMainDO, new QueryWrapper<>(new WordMainDO().setWordName(wordMainDO.getWordName())));
            wordMainDO = wordMainService.getOne(new QueryWrapper<>(new WordMainDO().setWordName(wordMainDO.getWordName())));

            removeWordRelatedData(wordMainDO);
        } else {
            wordMainDO.setIsDel(CommonConstants.FALSE);
            wordMainService.save(wordMainDO);
        }

        Integer wordId = wordMainDO.getWordId();

        if (wordId == null) {
            throw new WordResultStoreException("wordId is not null!");
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
                        String voiceFileUrl = CrawlerConstants.CAMBRIDGE_BASE_URL + fetchWordPronunciationDTO.getVoiceFileUrl();
                        long voiceSize = HttpUtil.downloadFile(URLUtil.decode(voiceFileUrl), FileUtil.file(CrawlerConstants.CRAWLER_VOICE_BASE_PATH));
                        String tempVoice = CrawlerConstants.CRAWLER_VOICE_BASE_PATH + CrawlerUtils.getVoiceFileName(voiceFileUrl);
                        String uploadResult = dfsService.uploadFile(FileUtil.getInputStream(tempVoice), voiceSize, CrawlerConstants.EXT_OGG);
                        WordPronunciationDO wordPronunciation = CrawlerEntityFactory.initWordPronunciation(wordId, characterId, uploadResult,
                                fetchWordPronunciationDTO.getSoundmark(), fetchWordPronunciationDTO.getSoundmarkType());
                        wordPronunciationService.save(wordPronunciation);
                    }
                }
            }
        }

        return true;
    }

    @Override
    @Transactional(noRollbackFor = DfsOperateDeleteException.class)
    public void removeWordRelatedData(WordMainDO wordMainDO) throws DfsOperateDeleteException {
        Integer wordId = wordMainDO.getWordId();
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void dfsDeleteExceptionBackCall(String wordName) {
        WordFetchQueueDO wordFetchQueue = new WordFetchQueueDO();
        wordFetchQueue.setFetchStatus(CrawlerConstants.STATUS_ERROR_DFS_OPERATE_DELETE_FAILED).setFetchResult("delete pronunciation voice file error");
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
    public WordQueryVO queryWord(String wordName) throws ServiceException {
        WordQueryVO wordQueryVO = new WordQueryVO();
        WordMainDO word = this.wordMainService.getOne(new QueryWrapper<>(new WordMainDO().setWordName(wordName)));
        // TODO: 2020/1/7 Here we have to decide if it's a tense-changing word

        // if you can't find the result after the tense is determined, insert a record into the queue to be fetched
        if (word == null) {
            // 先抓取到时态变化的其他读个不同wordName，然后每个都fetchNewWord一下
        }

        EnhancedAssertUtils.serviceNotNull(word, "No results for [{}]!", wordName);

        wordQueryVO.setWordId(word.getWordId());
        wordQueryVO.setWordName(word.getWordName());
        wordQueryVO.setIsCollect(CommonConstants.FALSE);

        Integer wordId = word.getWordId();
        // Visitor users do not need to query isCollect
        boolean isQueryCollect = false;
        final Integer currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId != null) {
            isQueryCollect = true;
        }
        wordQueryVO.setIsLogin(CommonUtils.translateBooleanToStr(isQueryCollect));

        // Check to see if words have been collected
        if (isQueryCollect) {
            boolean countWordIsCollect = this.wordStarListService.countWordIsCollect(wordId, currentUserId);
            if (countWordIsCollect) {
                wordQueryVO.setIsCollect(CommonConstants.TRUE);
            }
        }

        wordQueryVO.setWordCharacterVOList(assembleWordCharacterVOS(wordName, wordId, isQueryCollect, currentUserId));
        return wordQueryVO;
    }

    private List<WordCharacterVO> assembleWordCharacterVOS(String wordName, Integer wordId, boolean isQueryCollect, Integer currentUserId) throws ServiceException {
        List<WordCharacterDO> wordCharacterList = this.wordCharacterService.list(new QueryWrapper<>(new WordCharacterDO().setWordId(wordId)));
        EnhancedAssertUtils.serviceNotEmpty(wordCharacterList, "No character for [{}]!", wordName);

        List<WordCharacterVO> wordCharacterVOList = new ArrayList<>();
        for (WordCharacterDO wordCharacter : wordCharacterList) {
            WordCharacterVO wordCharacterVO = new WordCharacterVO();
            BeanUtil.copyProperties(wordCharacter, wordCharacterVO);
            wordCharacterVOList.add(wordCharacterVO);

            List<WordParaphraseVO> wordParaphraseVOList = assembleWordParaphraseVOS(wordCharacter.getCharacterId(), isQueryCollect, currentUserId);
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
        List<WordPronunciationDO> wordPronunciationList = this.wordPronunciationService.list(new QueryWrapper<>(new WordPronunciationDO().setCharacterId(wordCharacter.getCharacterId())));
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

    private List<WordParaphraseVO> assembleWordParaphraseVOS(Integer characterId, boolean isQueryCollect, Integer currentUserId) {
        List<WordParaphraseVO> wordParaphraseVOList = new ArrayList<>();
        if (isQueryCollect) {
            wordParaphraseVOList = this.wordParaphraseService.selectParaphraseAndIsCollect(characterId, currentUserId);
            if (CollUtil.isEmpty(wordParaphraseVOList)) {
                return wordParaphraseVOList;
            }

            for (WordParaphraseVO wordParaphraseVO : wordParaphraseVOList) {
                List<WordParaphraseExampleVO> wordParaphraseExampleVOList = assembleWordParaphraseExampleVOS(wordParaphraseVO.getParaphraseId(), isQueryCollect, currentUserId);
                if (wordParaphraseExampleVOList == null) {
                    continue;
                }
                wordParaphraseVO.setWordParaphraseExampleVOList(wordParaphraseExampleVOList);
            }
        } else {
            List<WordParaphraseDO> wordParaphraseDOList = this.wordParaphraseService.list(new QueryWrapper<>(new WordParaphraseDO().setCharacterId(characterId)));
            if (CollUtil.isEmpty(wordParaphraseDOList)) {
                return wordParaphraseVOList;
            }

            for (WordParaphraseDO wordParaphraseDO : wordParaphraseDOList) {
                WordParaphraseVO wordParaphraseVO = new WordParaphraseVO();
                BeanUtil.copyProperties(wordParaphraseDO, wordParaphraseVO);
                wordParaphraseVOList.add(wordParaphraseVO);

                List<WordParaphraseExampleVO> wordParaphraseExampleVOList = assembleWordParaphraseExampleVOS(wordParaphraseVO.getParaphraseId(), isQueryCollect, currentUserId);
                if (wordParaphraseExampleVOList == null) {
                    continue;
                }
                wordParaphraseVO.setWordParaphraseExampleVOList(wordParaphraseExampleVOList);
            }
        }
        return wordParaphraseVOList;
    }

    private List<WordParaphraseExampleVO> assembleWordParaphraseExampleVOS(Integer paraphraseId, boolean isQueryCollect, Integer currentUserId) {
        List<WordParaphraseExampleVO> wordParaphraseExampleVOList = new ArrayList<>();
        if (isQueryCollect) {
            wordParaphraseExampleVOList = this.wordParaphraseExampleService.selectExampleAndIsCollect(currentUserId, paraphraseId);
        } else {
            List<WordParaphraseExampleDO> wordParaphraseExampleDOList = this.wordParaphraseExampleService.list(new QueryWrapper<>(new WordParaphraseExampleDO().setParaphraseId(paraphraseId)));
            if (CollUtil.isEmpty(wordParaphraseExampleDOList)) {
                return null;
            }
            for (WordParaphraseExampleDO wordParaphraseExampleDO : wordParaphraseExampleDOList) {
                WordParaphraseExampleVO wordParaphraseExampleVO = new WordParaphraseExampleVO();
                BeanUtil.copyProperties(wordParaphraseExampleDO, wordParaphraseExampleVO);
                wordParaphraseExampleVOList.add(wordParaphraseExampleVO);
            }
        }
        return wordParaphraseExampleVOList;
    }

    @Override
    public boolean putWordIntoStarList(Integer wordId, Integer listId) throws ServiceException {
        LambdaQueryWrapper<WordStarRelDO> queryWrapper = new LambdaQueryWrapper<WordStarRelDO>().eq(WordStarRelDO::getListId, listId)
                .eq(WordStarRelDO::getWordId, wordId);
        int count = wordStarRelService.count(queryWrapper);
        EnhancedAssertUtils.serviceEmpty(count, "wordStar is exists!");
        return this.wordStarRelService.save(
                new WordStarRelDO()
                        .setListId(listId)
                        .setWordId(wordId)
        );
    }

    @Override
    public boolean removeWordStarList(Integer wordId, Integer listId) throws ServiceException {
        LambdaQueryWrapper<WordStarRelDO> queryWrapper = new LambdaQueryWrapper<WordStarRelDO>().eq(WordStarRelDO::getListId, listId)
                .eq(WordStarRelDO::getWordId, wordId);
        int count = wordStarRelService.count(queryWrapper);
        EnhancedAssertUtils.serviceNotEmpty(count, "wordStar is not exists!");
        return this.wordStarRelService.remove(queryWrapper);
    }

    @Override
    public boolean putParaphraseIntoStarList(Integer paraphraseId, Integer listId) throws ServiceException {
        EnhancedAssertUtils.serviceNotEmpty(
                this.wordParaphraseService.countById(paraphraseId), "paraphrase is not exists!");
        EnhancedAssertUtils.serviceNotEmpty(
                this.wordParaphraseStarListService.countById(listId), "paraphraseStarList is not exists!");
        LambdaQueryWrapper<WordParaphraseStarRelDO> wrapper = new LambdaQueryWrapper<WordParaphraseStarRelDO>().eq(WordParaphraseStarRelDO::getListId, listId).eq(WordParaphraseStarRelDO::getParaphraseId, paraphraseId);
        EnhancedAssertUtils.serviceEmpty(this.wordParaphraseStarRelService.count(wrapper), "paraphrase already exists!");
        return this.wordParaphraseStarRelService.save(
                new WordParaphraseStarRelDO()
                        .setListId(listId)
                        .setParaphraseId(paraphraseId)
        );
    }

    @Override
    public boolean putExampleIntoStarList(Integer exampleId, Integer listId) throws ServiceException {
        LambdaQueryWrapper<WordExampleStarRelDO> queryWrapper = new LambdaQueryWrapper<WordExampleStarRelDO>().eq(WordExampleStarRelDO::getListId, listId).eq(WordExampleStarRelDO::getExampleId, exampleId);
        EnhancedAssertUtils.serviceEmpty(this.wordExampleStarRelService.count(queryWrapper), "example rel is already exists!");
        return this.wordExampleStarRelService.save(
                new WordExampleStarRelDO()
                        .setListId(listId)
                        .setExampleId(exampleId)
        );
    }

    @Override
    public WordCharacterVO getByParaphraseId(Integer paraphraseId) throws ServiceException {
        WordParaphraseDO paraphrase = this.wordParaphraseService.getOne(
                new QueryWrapper<>(
                        new WordParaphraseDO()
                                .setParaphraseId(paraphraseId)
                )
        );
        EnhancedAssertUtils.serviceNotNull(paraphrase, "paraphrase[{}] is not exists!", paraphraseId);

        final Integer characterId = paraphrase.getCharacterId();
        WordCharacterDO character = this.wordCharacterService.getOne(
                new QueryWrapper<>(
                        new WordCharacterDO()
                                .setCharacterId(characterId)
                )
        );
        EnhancedAssertUtils.serviceNotNull(character, "character[{}] is not exists!", characterId);

        List<WordParaphraseVO> paraphraseVOList = new ArrayList<>();
        WordParaphraseVO wordParaphraseVO = new WordParaphraseVO();
        BeanUtil.copyProperties(paraphrase, wordParaphraseVO);
        paraphraseVOList.add(wordParaphraseVO);
        WordCharacterVO wordCharacterVO = new WordCharacterVO().setWordCharacter(character.getWordCharacter())
                .setWordLabel(character.getWordLabel())
                .setWordParaphraseVOList(paraphraseVOList);

        List<WordPronunciationDO> pronunciationList = this.wordPronunciationService.list(
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
        int count = this.wordExampleStarRelService.count(queryWrapper);
        EnhancedAssertUtils.serviceNotEmpty(count, "example is not exists!");
        return this.wordExampleStarRelService.remove(queryWrapper);
    }

}
