/*
 * Copyright [2019~2025] [codingByFeng]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.fengorz.kiwi.domain.word.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.domain.word.entity.*;
import me.fengorz.kiwi.domain.word.vo.*;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Word Query Service - handles complex word query operations
 * Migrated from OperateService
 *
 * @author codingByFeng
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WordQueryService {

    private static final String CACHE_NAME = "wordQuery";
    private static final String PHRASE_CODE = "phrase";

    private final WordMainService wordMainService;
    private final WordCharacterService characterService;
    private final ParaphraseService paraphraseService;
    private final ParaphraseExampleService exampleService;
    private final PronunciationService pronunciationService;
    private final WordMainVariantService variantService;
    private final ParaphrasePhraseService phraseService;
    private final FetchQueueService fetchQueueService;

    /**
     * Query word with full details
     */
    @Cacheable(value = CACHE_NAME, key = "'word:' + #wordName", unless = "#result == null")
    public Optional<WordQueryVO> queryWord(String wordName) {
        // First try to find the word directly
        Optional<WordMain> wordOpt = wordMainService.findByWordName(wordName);

        // If not found, try to find by variant
        if (wordOpt.isEmpty()) {
            Optional<WordMainVariant> variantOpt = variantService.findByVariantName(wordName);
            if (variantOpt.isPresent()) {
                wordOpt = wordMainService.findById(variantOpt.get().getWordId());
            }
        }

        // If still not found, add to fetch queue and return empty
        if (wordOpt.isEmpty()) {
            fetchQueueService.addToQueue(wordName, 100);
            return Optional.empty();
        }

        WordMain word = wordOpt.get();
        return Optional.of(assembleWordQueryVO(word));
    }

    /**
     * Query word by ID with full details
     */
    @Cacheable(value = CACHE_NAME, key = "'wordId:' + #wordId", unless = "#result == null")
    public Optional<WordQueryVO> queryWordById(Integer wordId) {
        return wordMainService.findById(wordId)
                .map(this::assembleWordQueryVO);
    }

    /**
     * Query paraphrase with full details
     */
    @Cacheable(value = CACHE_NAME, key = "'paraphrase:' + #paraphraseId", unless = "#result == null")
    public Optional<ParaphraseVO> findParaphraseVO(Integer paraphraseId) {
        return paraphraseService.findById(paraphraseId)
                .map(this::assembleParaphraseVO);
    }

    /**
     * Search words by Chinese meaning
     */
    public Page<WordQueryVO> searchByChineseMeaning(String chineseMeaning, Page<Paraphrase> page) {
        // Use database-level filtering with pagination
        Page<Paraphrase> paraphrasePage = paraphraseService.page(page, new LambdaQueryWrapper<Paraphrase>()
                .like(Paraphrase::getMeaningChinese, chineseMeaning)
                .eq(Paraphrase::getIsDel, 0));

        Set<Integer> wordIds = paraphrasePage.getRecords().stream()
                .map(Paraphrase::getWordId)
                .collect(Collectors.toSet());

        List<WordQueryVO> results = wordIds.stream()
                .map(id -> wordMainService.findById(id))
                .filter(Optional::isPresent)
                .map(opt -> assembleWordQueryVO(opt.get()))
                .toList();

        Page<WordQueryVO> resultPage = new Page<>(page.getCurrent(), page.getSize(), paraphrasePage.getTotal());
        resultPage.setRecords(results);
        return resultPage;
    }

    /**
     * Update paraphrase Chinese meaning
     */
    @Transactional
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public boolean updateMeaningChinese(Integer paraphraseId, String meaningChinese) {
        return paraphraseService.updateMeaningChinese(paraphraseId, meaningChinese);
    }

    /**
     * Insert word variant
     */
    @Transactional
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public boolean insertVariant(String inputWordName, String fetchWordName) {
        if (inputWordName.equals(fetchWordName)) {
            return false;
        }

        Optional<WordMain> wordOpt = wordMainService.findByWordName(fetchWordName);
        if (wordOpt.isEmpty()) {
            log.warn("Word {} not found for variant insertion", fetchWordName);
            return false;
        }

        Integer wordId = wordOpt.get().getWordId();

        // Check if variant already exists
        if (variantService.existsByVariantName(inputWordName)) {
            return false;
        }

        variantService.create(wordId, inputWordName, null);
        return true;
    }

    /**
     * Evict cache for a word
     */
    @CacheEvict(value = CACHE_NAME, key = "'word:' + #wordName")
    public void evictWordCache(String wordName) {
        log.debug("Evicted word cache for: {}", wordName);
    }

    // ==================== Private Assembly Methods ====================

    private WordQueryVO assembleWordQueryVO(WordMain word) {
        WordQueryVO vo = new WordQueryVO();
        vo.setWordId(word.getWordId());
        vo.setWordName(word.getWordName());
        vo.setIsCollect("N");

        // Handle phrase type words differently
        if (word.isPhrase()) {
            vo.setCharacterVOList(assemblePhraseCharacters(word.getWordId()));
        } else {
            vo.setCharacterVOList(assembleCharacters(word.getWordId()));
        }

        return vo;
    }

    private List<CharacterVO> assemblePhraseCharacters(Integer wordId) {
        CharacterVO characterVO = new CharacterVO();
        characterVO.setCharacterId(0);
        characterVO.setCharacterCode(PHRASE_CODE);
        characterVO.setParaphraseVOList(new ArrayList<>());
        characterVO.setPronunciationVOList(new ArrayList<>());

        List<Paraphrase> paraphrases = paraphraseService.findByWordId(wordId);
        for (Paraphrase paraphrase : paraphrases) {
            ParaphraseVO paraphraseVO = assembleParaphraseVO(paraphrase);
            characterVO.getParaphraseVOList().add(paraphraseVO);
        }

        return Collections.singletonList(characterVO);
    }

    private List<CharacterVO> assembleCharacters(Integer wordId) {
        List<CharacterVO> characterVOList = new ArrayList<>();
        List<WordCharacter> characters = characterService.findByWordId(wordId);

        for (WordCharacter character : characters) {
            CharacterVO characterVO = new CharacterVO();
            characterVO.setCharacterId(character.getCharacterId());
            characterVO.setCharacterCode(character.getCharacterCode());
            characterVO.setTag(character.getTag());

            // Assemble paraphrases
            List<Paraphrase> paraphrases = paraphraseService.findByCharacterId(character.getCharacterId());
            List<ParaphraseVO> paraphraseVOList = paraphrases.stream()
                    .map(this::assembleParaphraseVO)
                    .collect(Collectors.toList());
            characterVO.setParaphraseVOList(paraphraseVOList);

            // Assemble pronunciations
            List<Pronunciation> pronunciations = pronunciationService.findByCharacterId(character.getCharacterId());
            List<PronunciationVO> pronunciationVOList = pronunciations.stream()
                    .map(this::assemblePronunciationVO)
                    .collect(Collectors.toList());
            characterVO.setPronunciationVOList(pronunciationVOList);

            characterVOList.add(characterVO);
        }

        return characterVOList;
    }

    private ParaphraseVO assembleParaphraseVO(Paraphrase paraphrase) {
        ParaphraseVO vo = new ParaphraseVO();
        vo.setParaphraseId(paraphrase.getParaphraseId());
        vo.setWordId(paraphrase.getWordId());
        vo.setCharacterId(paraphrase.getCharacterId());
        vo.setCodes(paraphrase.getCodes());
        vo.setParaphraseEnglish(paraphrase.getParaphraseEnglish());
        vo.setParaphraseEnglishTranslate(paraphrase.getParaphraseEnglishTranslate());
        vo.setMeaningChinese(paraphrase.getMeaningChinese());
        vo.setIsCollect("N");

        // Get word name
        wordMainService.findById(paraphrase.getWordId())
                .ifPresent(word -> vo.setWordName(word.getWordName()));

        // Get character info if available
        if (paraphrase.getCharacterId() != null) {
            characterService.findById(paraphrase.getCharacterId())
                    .ifPresent(character -> {
                        vo.setWordCharacter(character.getCharacterCode());
                        vo.setWordLabel(character.getTag());
                    });
        } else {
            vo.setWordCharacter(PHRASE_CODE);
        }

        // Assemble examples
        List<ParaphraseExample> examples = exampleService.findByParaphraseId(paraphrase.getParaphraseId());
        List<ParaphraseExampleVO> exampleVOList = examples.stream()
                .map(this::assembleExampleVO)
                .collect(Collectors.toList());
        vo.setExampleVOList(exampleVOList);

        // Assemble phrases if applicable
        if (paraphrase.hasPhrase()) {
            List<ParaphrasePhrase> phrases = phraseService.findByParaphraseId(paraphrase.getParaphraseId());
            List<String> phraseList = phrases.stream()
                    .map(ParaphrasePhrase::getPhrase)
                    .collect(Collectors.toList());
            vo.setPhraseList(phraseList);
        }

        // Assemble pronunciations by characterId
        if (paraphrase.getCharacterId() != null) {
            List<Pronunciation> pronunciations = pronunciationService.findByCharacterId(paraphrase.getCharacterId());
            List<PronunciationVO> pronunciationVOList = pronunciations.stream()
                    .map(this::assemblePronunciationVO)
                    .collect(Collectors.toList());
            vo.setPronunciationVOList(pronunciationVOList);
        }

        return vo;
    }

    private ParaphraseExampleVO assembleExampleVO(ParaphraseExample example) {
        ParaphraseExampleVO vo = new ParaphraseExampleVO();
        vo.setExampleId(example.getExampleId());
        vo.setWordId(example.getWordId());
        vo.setParaphraseId(example.getParaphraseId());
        vo.setExampleSentence(example.getExampleSentence());
        vo.setExampleTranslate(example.getExampleTranslate());
        vo.setTranslateLanguage(example.getTranslateLanguage());
        vo.setSerialNumber(example.getSerialNumber());
        vo.setIsCollect("N");
        return vo;
    }

    private PronunciationVO assemblePronunciationVO(Pronunciation pronunciation) {
        PronunciationVO vo = new PronunciationVO();
        vo.setPronunciationId(pronunciation.getPronunciationId());
        vo.setSoundmark(pronunciation.getSoundmark());
        vo.setSoundmarkType(pronunciation.getSoundmarkType());
        vo.setSourceUrl(pronunciation.getSourceUrl());
        return vo;
    }
}
