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
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.constant.GlobalConstants;
import me.fengorz.kiwi.domain.word.entity.WordMain;
import me.fengorz.kiwi.domain.word.mapper.WordMainMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * WordMain Service
 *
 * @author codingByFeng
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class WordMainService extends ServiceImpl<WordMainMapper, WordMain> {

    private static final String CACHE_NAME = "word";

    /**
     * Find word by ID
     */
    public Optional<WordMain> findById(Integer wordId) {
        return Optional.ofNullable(getById(wordId));
    }

    /**
     * Find word by name
     */
    @Cacheable(value = CACHE_NAME, key = "'name:' + #wordName", unless = "#result == null")
    public Optional<WordMain> findByWordName(String wordName) {
        return Optional.ofNullable(getOne(new LambdaQueryWrapper<WordMain>()
                .eq(WordMain::getWordName, wordName)
                .eq(WordMain::getIsDel, 0)));
    }

    /**
     * Find word by ID with all details
     */
    public Optional<WordMain> findByIdWithDetails(Integer wordId) {
        return findById(wordId);
    }

    /**
     * Find word by name with pronunciations
     */
    public Optional<WordMain> findByWordNameWithPronunciations(String wordName) {
        return findByWordName(wordName);
    }

    /**
     * Check if word exists
     */
    public boolean existsByWordName(String wordName) {
        return count(new LambdaQueryWrapper<WordMain>()
                .eq(WordMain::getWordName, wordName)
                .eq(WordMain::getIsDel, 0)) > 0;
    }

    /**
     * Find all words with pagination
     */
    public Page<WordMain> findAll(Page<WordMain> page) {
        return page(page, new LambdaQueryWrapper<WordMain>()
                .eq(WordMain::getIsDel, 0));
    }

    /**
     * Find words by name containing (simple search)
     */
    public Page<WordMain> findByWordNameContaining(String wordName, Page<WordMain> page) {
        return page(page, new LambdaQueryWrapper<WordMain>()
                .like(WordMain::getWordName, wordName)
                .eq(WordMain::getIsDel, 0));
    }

    /**
     * Find words starting with prefix
     */
    public List<WordMain> findByWordNameStartingWith(String prefix) {
        return list(new LambdaQueryWrapper<WordMain>()
                .likeRight(WordMain::getWordName, prefix)
                .eq(WordMain::getIsDel, 0));
    }

    /**
     * Fuzzy query with variant support
     */
    public Page<WordMain> fuzzyQuery(String query, Page<WordMain> page) {
        return page(page, new LambdaQueryWrapper<WordMain>()
                .like(WordMain::getWordName, query)
                .eq(WordMain::getIsDel, 0));
    }

    /**
     * Save word
     */
    @Transactional
    @CacheEvict(value = CACHE_NAME, key = "'name:' + #word.wordName")
    public WordMain saveWord(WordMain word) {
        if (word.getWordId() == null) {
            word.setInTime(LocalDateTime.now());
            word.setIsDel(GlobalConstants.FLAG_N);
        }
        word.setLastUpdateTime(LocalDateTime.now());
        saveOrUpdate(word);
        return word;
    }

    /**
     * Delete word by ID
     */
    @Transactional
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public void deleteWordById(Integer wordId) {
        removeById(wordId);
    }

    /**
     * Delete word by ID (alias for deleteWordById)
     */
    @Transactional
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public void deleteById(Integer wordId) {
        removeById(wordId);
    }

    /**
     * Evict cache for word
     */
    @CacheEvict(value = CACHE_NAME, key = "'name:' + #wordName")
    public void evictCache(String wordName) {
        log.debug("Evicted cache for word: {}", wordName);
    }

    /**
     * Evict cache by ID
     */
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public void evictCacheById(Integer wordId) {
        log.debug("Evicted cache for word ID: {}", wordId);
    }

    /**
     * Find all words by name
     */
    public List<WordMain> findAllByWordName(String wordName) {
        return list(new LambdaQueryWrapper<WordMain>()
                .eq(WordMain::getWordName, wordName)
                .eq(WordMain::getIsDel, 0));
    }

    /**
     * Find words by name and info type
     */
    public List<WordMain> findByWordNameAndInfoType(String wordName, Integer infoType) {
        return list(new LambdaQueryWrapper<WordMain>()
                .eq(WordMain::getWordName, wordName)
                .eq(WordMain::getInfoType, infoType)
                .eq(WordMain::getIsDel, 0));
    }

    /**
     * DTO for fuzzy query results
     */
    public record FuzzyQueryResult(String wordName, Integer wordId) {}
}
