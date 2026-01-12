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
import me.fengorz.kiwi.domain.word.entity.Paraphrase;
import me.fengorz.kiwi.domain.word.mapper.ParaphraseMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Paraphrase Service - manages word paraphrase/meaning operations
 *
 * @author codingByFeng
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class ParaphraseService extends ServiceImpl<ParaphraseMapper, Paraphrase> {

    private static final String CACHE_NAME = "paraphrase";

    /**
     * Find paraphrase by ID
     */
    public Optional<Paraphrase> findById(Integer paraphraseId) {
        return Optional.ofNullable(getById(paraphraseId));
    }

    /**
     * Find paraphrase by ID with examples
     */
    @Cacheable(value = CACHE_NAME, key = "'id:' + #paraphraseId")
    public Optional<Paraphrase> findByIdWithExamples(Integer paraphraseId) {
        return findById(paraphraseId);
    }

    /**
     * Find paraphrases by word ID
     */
    public List<Paraphrase> findByWordId(Integer wordId) {
        return list(new LambdaQueryWrapper<Paraphrase>()
                .eq(Paraphrase::getWordId, wordId)
                .eq(Paraphrase::getIsDel, 0)
                .orderByAsc(Paraphrase::getSerialNumber));
    }

    /**
     * Find paraphrases by word ID with examples
     */
    @Cacheable(value = CACHE_NAME, key = "'word:' + #wordId")
    public List<Paraphrase> findByWordIdWithExamples(Integer wordId) {
        return findByWordId(wordId);
    }

    /**
     * Find paraphrases by character ID
     */
    public List<Paraphrase> findByCharacterId(Integer characterId) {
        return list(new LambdaQueryWrapper<Paraphrase>()
                .eq(Paraphrase::getCharacterId, characterId)
                .eq(Paraphrase::getIsDel, 0)
                .orderByAsc(Paraphrase::getSerialNumber));
    }

    /**
     * Count paraphrases by word ID
     */
    public long countByWordId(Integer wordId) {
        return count(new LambdaQueryWrapper<Paraphrase>()
                .eq(Paraphrase::getWordId, wordId)
                .eq(Paraphrase::getIsDel, 0));
    }

    /**
     * Count paraphrase by ID (check existence)
     */
    public boolean existsById(Integer paraphraseId) {
        return getById(paraphraseId) != null;
    }

    /**
     * Save paraphrase
     */
    @Transactional
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public Paraphrase saveParaphrase(Paraphrase paraphrase) {
        if (paraphrase.getIsDel() == null) {
            paraphrase.setIsDel(GlobalConstants.FLAG_N);
        }
        saveOrUpdate(paraphrase);
        return paraphrase;
    }

    /**
     * Update meaning Chinese translation
     */
    @Transactional
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public boolean updateMeaningChinese(Integer paraphraseId, String meaningChinese) {
        return findById(paraphraseId)
                .map(paraphrase -> {
                    paraphrase.setMeaningChinese(meaningChinese);
                    updateById(paraphrase);
                    return true;
                })
                .orElse(false);
    }

    /**
     * Delete paraphrase by ID
     */
    @Transactional
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public void deleteById(Integer paraphraseId) {
        removeById(paraphraseId);
    }

    /**
     * Delete all paraphrases by word ID
     */
    @Transactional
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public void deleteByWordId(Integer wordId) {
        remove(new LambdaQueryWrapper<Paraphrase>()
                .eq(Paraphrase::getWordId, wordId));
    }

    /**
     * Evict cache for word
     */
    @CacheEvict(value = CACHE_NAME, key = "'word:' + #wordId")
    public void evictCacheByWordId(Integer wordId) {
        log.debug("Evicted paraphrase cache for word: {}", wordId);
    }

    /**
     * Delete all paraphrases by character ID
     */
    @Transactional
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public void deleteByCharacterId(Integer characterId) {
        remove(new LambdaQueryWrapper<Paraphrase>()
                .eq(Paraphrase::getCharacterId, characterId));
    }
}
