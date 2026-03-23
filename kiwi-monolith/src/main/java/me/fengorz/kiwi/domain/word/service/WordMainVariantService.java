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
import me.fengorz.kiwi.domain.word.entity.WordMainVariant;
import me.fengorz.kiwi.domain.word.mapper.WordMainVariantMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * WordMainVariant Service - manages word variant forms
 *
 * @author codingByFeng
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class WordMainVariantService extends ServiceImpl<WordMainVariantMapper, WordMainVariant> {

    private static final String CACHE_NAME = "variant";

    /**
     * Find variant by ID
     */
    public Optional<WordMainVariant> findById(Integer id) {
        return Optional.ofNullable(getById(id));
    }

    /**
     * Find variants by word ID
     */
    @Cacheable(value = CACHE_NAME, key = "'word:' + #wordId", unless = "#result == null")
    public List<WordMainVariant> findByWordId(Integer wordId) {
        return list(new LambdaQueryWrapper<WordMainVariant>()
                .eq(WordMainVariant::getWordId, wordId)
                .eq(WordMainVariant::getIsValid, 1));
    }

    /**
     * Find by variant name
     */
    @Cacheable(value = CACHE_NAME, key = "'name:' + #variantName", unless = "#result == null")
    public Optional<WordMainVariant> findByVariantName(String variantName) {
        return Optional.ofNullable(getOne(new LambdaQueryWrapper<WordMainVariant>()
                .eq(WordMainVariant::getVariantName, variantName)
                .eq(WordMainVariant::getIsValid, 1)));
    }

    /**
     * Find variants by type
     */
    public List<WordMainVariant> findByWordIdAndType(Integer wordId, Integer type) {
        return list(new LambdaQueryWrapper<WordMainVariant>()
                .eq(WordMainVariant::getWordId, wordId)
                .eq(WordMainVariant::getType, type));
    }

    /**
     * Check if variant exists
     */
    public boolean existsByVariantName(String variantName) {
        return count(new LambdaQueryWrapper<WordMainVariant>()
                .eq(WordMainVariant::getVariantName, variantName)) > 0;
    }

    /**
     * Count variants for a word
     */
    public long countByWordId(Integer wordId) {
        return count(new LambdaQueryWrapper<WordMainVariant>()
                .eq(WordMainVariant::getWordId, wordId));
    }

    /**
     * Get word ID from variant name (for looking up words by their variant forms)
     */
    public Optional<Integer> getWordIdByVariantName(String variantName) {
        return findByVariantName(variantName)
                .map(WordMainVariant::getWordId);
    }

    /**
     * Save variant
     */
    @Transactional
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public WordMainVariant saveVariant(WordMainVariant variant) {
        if (variant.getIsValid() == null) {
            variant.setIsValid(1);
        }
        saveOrUpdate(variant);
        return variant;
    }

    /**
     * Create a new variant
     */
    @Transactional
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public WordMainVariant create(Integer wordId, String variantName, Integer type) {
        WordMainVariant variant = WordMainVariant.builder()
                .wordId(wordId)
                .variantName(variantName)
                .type(type)
                .isValid(1)
                .build();
        save(variant);
        return variant;
    }

    /**
     * Delete variant by ID
     */
    @Transactional
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public void deleteById(Integer id) {
        removeById(id);
    }

    /**
     * Delete all variants by word ID
     */
    @Transactional
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public void deleteByWordId(Integer wordId) {
        remove(new LambdaQueryWrapper<WordMainVariant>()
                .eq(WordMainVariant::getWordId, wordId));
    }

    /**
     * Evict cache for word
     */
    @CacheEvict(value = CACHE_NAME, key = "'word:' + #wordId")
    public void evictCacheByWordId(Integer wordId) {
        log.debug("Evicted variant cache for word: {}", wordId);
    }
}
