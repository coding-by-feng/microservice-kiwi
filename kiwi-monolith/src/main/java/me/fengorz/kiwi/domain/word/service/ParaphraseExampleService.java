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
import me.fengorz.kiwi.domain.word.entity.ParaphraseExample;
import me.fengorz.kiwi.domain.word.mapper.ParaphraseExampleMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * ParaphraseExample Service - manages paraphrase examples
 *
 * @author codingByFeng
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class ParaphraseExampleService extends ServiceImpl<ParaphraseExampleMapper, ParaphraseExample> {

    private static final String CACHE_NAME = "example";

    /**
     * Find example by ID
     */
    public Optional<ParaphraseExample> findById(Integer exampleId) {
        return Optional.ofNullable(getById(exampleId));
    }

    /**
     * Find examples by paraphrase ID
     */
    @Cacheable(value = CACHE_NAME, key = "'paraphrase:' + #paraphraseId")
    public List<ParaphraseExample> findByParaphraseId(Integer paraphraseId) {
        return list(new LambdaQueryWrapper<ParaphraseExample>()
                .eq(ParaphraseExample::getParaphraseId, paraphraseId)
                .eq(ParaphraseExample::getIsDel, GlobalConstants.FLAG_N)
                .orderByAsc(ParaphraseExample::getSerialNumber));
    }

    /**
     * Count examples by paraphrase ID
     */
    public long countByParaphraseId(Integer paraphraseId) {
        return count(new LambdaQueryWrapper<ParaphraseExample>()
                .eq(ParaphraseExample::getParaphraseId, paraphraseId)
                .eq(ParaphraseExample::getIsDel, GlobalConstants.FLAG_N));
    }

    /**
     * Check if example exists
     */
    public boolean existsById(Integer exampleId) {
        return getById(exampleId) != null;
    }

    /**
     * Save example
     */
    @Transactional
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public ParaphraseExample saveExample(ParaphraseExample example) {
        if (example.getIsDel() == null) {
            example.setIsDel(GlobalConstants.FLAG_N);
        }
        saveOrUpdate(example);
        return example;
    }

    /**
     * Save all examples
     */
    @Transactional
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public List<ParaphraseExample> saveAllExamples(List<ParaphraseExample> examples) {
        examples.forEach(e -> {
            if (e.getIsDel() == null) {
                e.setIsDel(GlobalConstants.FLAG_N);
            }
        });
        saveBatch(examples);
        return examples;
    }

    /**
     * Delete example by ID
     */
    @Transactional
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public void deleteById(Integer exampleId) {
        removeById(exampleId);
    }

    /**
     * Delete all examples by paraphrase ID
     */
    @Transactional
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public void deleteByParaphraseId(Integer paraphraseId) {
        remove(new LambdaQueryWrapper<ParaphraseExample>()
                .eq(ParaphraseExample::getParaphraseId, paraphraseId));
    }

    /**
     * Evict cache for paraphrase
     */
    @CacheEvict(value = CACHE_NAME, key = "'paraphrase:' + #paraphraseId")
    public void evictCacheByParaphraseId(Integer paraphraseId) {
        log.debug("Evicted example cache for paraphrase: {}", paraphraseId);
    }
}
