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
import me.fengorz.kiwi.domain.word.entity.ParaphraseStarRel;
import me.fengorz.kiwi.domain.word.mapper.ParaphraseStarRelMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * ParaphraseStarRel Service - manages paraphrase star list relations
 *
 * @author codingByFeng
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ParaphraseStarRelService extends ServiceImpl<ParaphraseStarRelMapper, ParaphraseStarRel> {

    private static final String CACHE_NAME = "paraphraseStarRel";

    /**
     * Find relation by list ID and paraphrase ID
     */
    public Optional<ParaphraseStarRel> findByListIdAndParaphraseId(Integer listId, Integer paraphraseId) {
        return Optional.ofNullable(getOne(new LambdaQueryWrapper<ParaphraseStarRel>()
                .eq(ParaphraseStarRel::getListId, listId)
                .eq(ParaphraseStarRel::getParaphraseId, paraphraseId)));
    }

    /**
     * Find all relations by list ID
     */
    public List<ParaphraseStarRel> findByListId(Integer listId) {
        return list(new LambdaQueryWrapper<ParaphraseStarRel>()
                .eq(ParaphraseStarRel::getListId, listId)
                .orderByDesc(ParaphraseStarRel::getCreateTime));
    }

    /**
     * Find review items (not remembered yet)
     */
    public List<ParaphraseStarRel> findReviewItems(Integer listId) {
        return list(new LambdaQueryWrapper<ParaphraseStarRel>()
                .eq(ParaphraseStarRel::getListId, listId)
                .eq(ParaphraseStarRel::getIsRemember, 0)
                .orderByDesc(ParaphraseStarRel::getCreateTime));
    }

    /**
     * Find remember items (remembered but not kept in mind)
     */
    public List<ParaphraseStarRel> findRememberItems(Integer listId) {
        return list(new LambdaQueryWrapper<ParaphraseStarRel>()
                .eq(ParaphraseStarRel::getListId, listId)
                .eq(ParaphraseStarRel::getIsRemember, 1)
                .eq(ParaphraseStarRel::getIsKeepInMind, 0)
                .orderByDesc(ParaphraseStarRel::getRememberTime));
    }

    /**
     * Check if relation exists
     */
    public boolean exists(Integer listId, Integer paraphraseId) {
        return count(new LambdaQueryWrapper<ParaphraseStarRel>()
                .eq(ParaphraseStarRel::getListId, listId)
                .eq(ParaphraseStarRel::getParaphraseId, paraphraseId)) > 0;
    }

    /**
     * Put paraphrase into star list
     */
    @Transactional
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public boolean putIntoStarList(Integer paraphraseId, Integer listId) {
        if (exists(listId, paraphraseId)) {
            log.info("Paraphrase {} already in list {}", paraphraseId, listId);
            return false;
        }
        ParaphraseStarRel rel = ParaphraseStarRel.builder()
                .listId(listId)
                .paraphraseId(paraphraseId)
                .isRemember(0)
                .isKeepInMind(0)
                .build();
        return save(rel);
    }

    /**
     * Remove paraphrase from star list
     */
    @Transactional
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public boolean removeFromStarList(Integer paraphraseId, Integer listId) {
        return remove(new LambdaQueryWrapper<ParaphraseStarRel>()
                .eq(ParaphraseStarRel::getListId, listId)
                .eq(ParaphraseStarRel::getParaphraseId, paraphraseId));
    }

    /**
     * Mark paraphrase as remembered
     */
    @Transactional
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public boolean rememberOne(Integer paraphraseId, Integer listId) {
        return findByListIdAndParaphraseId(listId, paraphraseId)
                .map(rel -> {
                    rel.setIsRemember(1);
                    rel.setRememberTime(LocalDateTime.now());
                    return updateById(rel);
                })
                .orElse(false);
    }

    /**
     * Mark paraphrase as kept in mind
     */
    @Transactional
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public boolean keepInMind(Integer paraphraseId, Integer listId) {
        return findByListIdAndParaphraseId(listId, paraphraseId)
                .map(rel -> {
                    rel.setIsKeepInMind(1);
                    rel.setKeepInMindTime(LocalDateTime.now());
                    return updateById(rel);
                })
                .orElse(false);
    }

    /**
     * Mark paraphrase as forgotten
     */
    @Transactional
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public boolean forgetOne(Integer paraphraseId, Integer listId) {
        return findByListIdAndParaphraseId(listId, paraphraseId)
                .map(rel -> {
                    rel.setIsRemember(0);
                    rel.setIsKeepInMind(0);
                    return updateById(rel);
                })
                .orElse(false);
    }

    /**
     * Delete all relations by list ID
     */
    @Transactional
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public boolean deleteByListId(Integer listId) {
        return remove(new LambdaQueryWrapper<ParaphraseStarRel>()
                .eq(ParaphraseStarRel::getListId, listId));
    }
}
