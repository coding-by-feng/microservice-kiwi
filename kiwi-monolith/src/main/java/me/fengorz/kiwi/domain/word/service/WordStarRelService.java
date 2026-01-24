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
import me.fengorz.kiwi.domain.word.entity.WordStarRel;
import me.fengorz.kiwi.domain.word.mapper.WordStarRelMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * WordStarRel Service - manages word-list relationships
 *
 * @author codingByFeng
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class WordStarRelService extends ServiceImpl<WordStarRelMapper, WordStarRel> {

    private static final String CACHE_NAME = "wordStarRel";

    /**
     * Find relation by composite ID
     */
    public Optional<WordStarRel> findById(Integer listId, Integer wordId) {
        return Optional.ofNullable(getOne(new LambdaQueryWrapper<WordStarRel>()
                .eq(WordStarRel::getListId, listId)
                .eq(WordStarRel::getWordId, wordId)));
    }

    /**
     * Find relations by list ID
     */
    public List<WordStarRel> findByListId(Integer listId) {
        return list(new LambdaQueryWrapper<WordStarRel>()
                .eq(WordStarRel::getListId, listId));
    }

    /**
     * Find relations by list ID with pagination
     */
    public Page<WordStarRel> findByListId(Integer listId, Page<WordStarRel> page) {
        return page(page, new LambdaQueryWrapper<WordStarRel>()
                .eq(WordStarRel::getListId, listId));
    }

    /**
     * Find relations by word ID
     */
    public List<WordStarRel> findByWordId(Integer wordId) {
        return list(new LambdaQueryWrapper<WordStarRel>()
                .eq(WordStarRel::getWordId, wordId));
    }

    /**
     * Find remembered words in a list
     */
    public List<WordStarRel> findRememberedByListId(Integer listId) {
        return list(new LambdaQueryWrapper<WordStarRel>()
                .eq(WordStarRel::getListId, listId)
                .eq(WordStarRel::getIsRemember, 1));
    }

    /**
     * Find not remembered words in a list
     */
    public List<WordStarRel> findNotRememberedByListId(Integer listId) {
        return list(new LambdaQueryWrapper<WordStarRel>()
                .eq(WordStarRel::getListId, listId)
                .eq(WordStarRel::getIsRemember, 0));
    }

    /**
     * Count words in a list
     */
    public long countByListId(Integer listId) {
        return count(new LambdaQueryWrapper<WordStarRel>()
                .eq(WordStarRel::getListId, listId));
    }

    /**
     * Count remembered words in a list
     */
    public long countRememberedByListId(Integer listId) {
        return count(new LambdaQueryWrapper<WordStarRel>()
                .eq(WordStarRel::getListId, listId)
                .eq(WordStarRel::getIsRemember, 1));
    }

    /**
     * Check if word is in list
     */
    public boolean existsByListIdAndWordId(Integer listId, Integer wordId) {
        return count(new LambdaQueryWrapper<WordStarRel>()
                .eq(WordStarRel::getListId, listId)
                .eq(WordStarRel::getWordId, wordId)) > 0;
    }

    /**
     * Add word to list
     */
    @Transactional
    @CacheEvict(value = CACHE_NAME, key = "'list:' + #listId")
    public WordStarRel addWordToList(Integer listId, Integer wordId) {
        if (existsByListIdAndWordId(listId, wordId)) {
            log.debug("Word {} already in list {}", wordId, listId);
            return findById(listId, wordId).orElse(null);
        }

        WordStarRel rel = WordStarRel.builder()
                .listId(listId)
                .wordId(wordId)
                .build();
        save(rel);
        return rel;
    }

    /**
     * Remove word from list
     */
    @Transactional
    @CacheEvict(value = CACHE_NAME, key = "'list:' + #listId")
    public void removeWordFromList(Integer listId, Integer wordId) {
        remove(new LambdaQueryWrapper<WordStarRel>()
                .eq(WordStarRel::getListId, listId)
                .eq(WordStarRel::getWordId, wordId));
    }

    /**
     * Mark word as remembered
     */
    @Transactional
    @CacheEvict(value = CACHE_NAME, key = "'list:' + #listId")
    public boolean markRemembered(Integer listId, Integer wordId) {
        return findById(listId, wordId)
                .map(rel -> {
                    rel.markRemembered();
                    updateById(rel);
                    return true;
                })
                .orElse(false);
    }

    /**
     * Mark word as forgotten
     */
    @Transactional
    @CacheEvict(value = CACHE_NAME, key = "'list:' + #listId")
    public boolean markForgotten(Integer listId, Integer wordId) {
        return findById(listId, wordId)
                .map(rel -> {
                    rel.markForgotten();
                    updateById(rel);
                    return true;
                })
                .orElse(false);
    }

    /**
     * Delete all relations by list ID
     */
    @Transactional
    @CacheEvict(value = CACHE_NAME, key = "'list:' + #listId")
    public void deleteByListId(Integer listId) {
        remove(new LambdaQueryWrapper<WordStarRel>()
                .eq(WordStarRel::getListId, listId));
    }
}
