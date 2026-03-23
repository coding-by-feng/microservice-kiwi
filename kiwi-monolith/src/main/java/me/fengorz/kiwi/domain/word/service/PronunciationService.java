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
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.constant.GlobalConstants;
import me.fengorz.kiwi.domain.word.entity.Pronunciation;
import me.fengorz.kiwi.domain.word.mapper.PronunciationMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Pronunciation Service - manages word pronunciation operations
 *
 * @author codingByFeng
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PronunciationService extends ServiceImpl<PronunciationMapper, Pronunciation> {

    private static final String CACHE_NAME = "pronunciation";

    private final WordMainService wordMainService;

    /**
     * Find pronunciation by ID
     */
    public Optional<Pronunciation> findById(Integer pronunciationId) {
        return Optional.ofNullable(getById(pronunciationId));
    }

    /**
     * Find pronunciations by word ID
     */
    @Cacheable(value = CACHE_NAME, key = "'word:' + #wordId", unless = "#result == null")
    public List<Pronunciation> findByWordId(Integer wordId) {
        return list(new LambdaQueryWrapper<Pronunciation>()
                .eq(Pronunciation::getWordId, wordId)
                .eq(Pronunciation::getIsDel, 0));
    }

    /**
     * Find pronunciations by character ID
     */
    public List<Pronunciation> findByCharacterId(Integer characterId) {
        return list(new LambdaQueryWrapper<Pronunciation>()
                .eq(Pronunciation::getCharacterId, characterId)
                .eq(Pronunciation::getIsDel, GlobalConstants.FLAG_N));
    }

    /**
     * Find UK pronunciation for a word
     */
    public Optional<Pronunciation> findUkPronunciation(Integer wordId) {
        return findByWordId(wordId).stream()
                .filter(Pronunciation::isUkPronunciation)
                .findFirst();
    }

    /**
     * Find US pronunciation for a word
     */
    public Optional<Pronunciation> findUsPronunciation(Integer wordId) {
        return findByWordId(wordId).stream()
                .filter(Pronunciation::isUsPronunciation)
                .findFirst();
    }

    /**
     * Save pronunciation
     */
    @Transactional
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public Pronunciation savePronunciation(Pronunciation pronunciation) {
        if (pronunciation.getIsDel() == null) {
            pronunciation.setIsDel(GlobalConstants.FLAG_N);
        }
        saveOrUpdate(pronunciation);
        return pronunciation;
    }

    /**
     * Clear voice file path for all pronunciations of a word
     */
    @Transactional
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public int blankVoiceByWordName(String wordName) {
        return wordMainService.findByWordName(wordName)
                .map(word -> {
                    List<Pronunciation> pronunciations = findByWordId(word.getWordId());
                    pronunciations.forEach(p -> p.setVoiceFilePath(null));
                    updateBatchById(pronunciations);
                    return pronunciations.size();
                })
                .orElse(0);
    }

    /**
     * Delete pronunciation by ID
     */
    @Transactional
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public void deleteById(Integer pronunciationId) {
        removeById(pronunciationId);
    }

    /**
     * Delete all pronunciations by word name
     */
    @Transactional
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public int deleteByWordName(String wordName) {
        return wordMainService.findByWordName(wordName)
                .map(word -> {
                    List<Pronunciation> pronunciations = findByWordId(word.getWordId());
                    removeBatchByIds(pronunciations.stream().map(Pronunciation::getPronunciationId).toList());
                    return pronunciations.size();
                })
                .orElse(0);
    }

    /**
     * Delete all pronunciations by word ID
     */
    @Transactional
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public void deleteByWordId(Integer wordId) {
        remove(new LambdaQueryWrapper<Pronunciation>()
                .eq(Pronunciation::getWordId, wordId));
    }

    /**
     * Evict cache for word
     */
    @CacheEvict(value = CACHE_NAME, key = "'word:' + #wordId")
    public void evictCacheByWordId(Integer wordId) {
        log.debug("Evicted pronunciation cache for word: {}", wordId);
    }
}
