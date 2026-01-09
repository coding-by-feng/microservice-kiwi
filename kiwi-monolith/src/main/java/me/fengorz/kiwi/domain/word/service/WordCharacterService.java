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
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.constant.GlobalConstants;
import me.fengorz.kiwi.domain.word.entity.WordCharacter;
import me.fengorz.kiwi.domain.word.mapper.WordCharacterMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * WordCharacter Service - manages word character (part of speech) operations
 *
 * @author codingByFeng
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class WordCharacterService extends ServiceImpl<WordCharacterMapper, WordCharacter> {

    private static final String CACHE_NAME = "character";

    /**
     * Find character by ID
     */
    public Optional<WordCharacter> findById(Integer characterId) {
        return Optional.ofNullable(getById(characterId));
    }

    /**
     * Find character by ID with paraphrases
     */
    @Cacheable(value = CACHE_NAME, key = "'id:' + #characterId")
    public Optional<WordCharacter> findByIdWithParaphrases(Integer characterId) {
        return findById(characterId);
    }

    /**
     * Find character by ID with pronunciations
     */
    public Optional<WordCharacter> findByIdWithPronunciations(Integer characterId) {
        return findById(characterId);
    }

    /**
     * Find characters by word ID
     */
    @Cacheable(value = CACHE_NAME, key = "'word:' + #wordId")
    public List<WordCharacter> findByWordId(Integer wordId) {
        return list(new LambdaQueryWrapper<WordCharacter>()
                .eq(WordCharacter::getWordId, wordId)
                .eq(WordCharacter::getIsDel, GlobalConstants.FLAG_N)
                .orderByAsc(WordCharacter::getCharacterId));
    }

    /**
     * Find character by word ID and character code
     */
    public Optional<WordCharacter> findByWordIdAndCode(Integer wordId, String characterCode) {
        return Optional.ofNullable(getOne(new LambdaQueryWrapper<WordCharacter>()
                .eq(WordCharacter::getWordId, wordId)
                .eq(WordCharacter::getCharacterCode, characterCode)
                .eq(WordCharacter::getIsDel, GlobalConstants.FLAG_N)));
    }

    /**
     * Count characters by word ID
     */
    public long countByWordId(Integer wordId) {
        return count(new LambdaQueryWrapper<WordCharacter>()
                .eq(WordCharacter::getWordId, wordId)
                .eq(WordCharacter::getIsDel, GlobalConstants.FLAG_N));
    }

    /**
     * Check if character exists
     */
    public boolean existsById(Integer characterId) {
        return getById(characterId) != null;
    }

    /**
     * Save character
     */
    @Transactional
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public WordCharacter saveCharacter(WordCharacter character) {
        if (character.getIsDel() == null) {
            character.setIsDel(GlobalConstants.FLAG_N);
        }
        saveOrUpdate(character);
        return character;
    }

    /**
     * Delete character by ID
     */
    @Transactional
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public void deleteById(Integer characterId) {
        removeById(characterId);
    }

    /**
     * Delete all characters by word ID
     */
    @Transactional
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public void deleteByWordId(Integer wordId) {
        remove(new LambdaQueryWrapper<WordCharacter>()
                .eq(WordCharacter::getWordId, wordId));
    }

    /**
     * Evict cache for word
     */
    @CacheEvict(value = CACHE_NAME, key = "'word:' + #wordId")
    public void evictCacheByWordId(Integer wordId) {
        log.debug("Evicted character cache for word: {}", wordId);
    }

    /**
     * Evict cache by character ID
     */
    @CacheEvict(value = CACHE_NAME, key = "'id:' + #characterId")
    public void evictCache(Integer characterId) {
        log.debug("Evicted character cache for ID: {}", characterId);
    }
}
