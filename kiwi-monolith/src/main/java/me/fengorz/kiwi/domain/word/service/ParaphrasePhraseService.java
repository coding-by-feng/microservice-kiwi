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
import me.fengorz.kiwi.domain.word.entity.ParaphrasePhrase;
import me.fengorz.kiwi.domain.word.mapper.ParaphrasePhraseMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * ParaphrasePhrase Service - manages phrases related to paraphrases
 *
 * @author codingByFeng
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class ParaphrasePhraseService extends ServiceImpl<ParaphrasePhraseMapper, ParaphrasePhrase> {

    private static final String CACHE_NAME = "phrase";
    private static final int VALID = 1;

    /**
     * Find phrase by ID
     */
    public Optional<ParaphrasePhrase> findById(Integer id) {
        return Optional.ofNullable(getById(id));
    }

    /**
     * Find phrases by paraphrase ID
     */
    @Cacheable(value = CACHE_NAME, key = "'paraphrase:' + #paraphraseId", unless = "#result == null")
    public List<ParaphrasePhrase> findByParaphraseId(Integer paraphraseId) {
        return list(new LambdaQueryWrapper<ParaphrasePhrase>()
                .eq(ParaphrasePhrase::getParaphraseId, paraphraseId)
                .eq(ParaphrasePhrase::getIsValid, VALID));
    }

    /**
     * Count phrases for a paraphrase
     */
    public long countByParaphraseId(Integer paraphraseId) {
        return count(new LambdaQueryWrapper<ParaphrasePhrase>()
                .eq(ParaphrasePhrase::getParaphraseId, paraphraseId)
                .eq(ParaphrasePhrase::getIsValid, VALID));
    }

    /**
     * Save phrase
     */
    @Transactional
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public ParaphrasePhrase savePhrase(ParaphrasePhrase phrase) {
        if (phrase.getIsValid() == null) {
            phrase.setIsValid(VALID);
        }
        saveOrUpdate(phrase);
        return phrase;
    }

    /**
     * Save all phrases
     */
    @Transactional
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public List<ParaphrasePhrase> saveAllPhrases(List<ParaphrasePhrase> phrases) {
        phrases.forEach(p -> {
            if (p.getIsValid() == null) {
                p.setIsValid(VALID);
            }
        });
        saveBatch(phrases);
        return phrases;
    }

    /**
     * Delete phrase by ID
     */
    @Transactional
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public void deleteById(Integer id) {
        removeById(id);
    }

    /**
     * Delete all phrases by paraphrase ID
     */
    @Transactional
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public void deleteByParaphraseId(Integer paraphraseId) {
        remove(new LambdaQueryWrapper<ParaphrasePhrase>()
                .eq(ParaphrasePhrase::getParaphraseId, paraphraseId));
    }

    /**
     * Evict cache for paraphrase
     */
    @CacheEvict(value = CACHE_NAME, key = "'paraphrase:' + #paraphraseId")
    public void evictCacheByParaphraseId(Integer paraphraseId) {
        log.debug("Evicted phrase cache for paraphrase: {}", paraphraseId);
    }
}
