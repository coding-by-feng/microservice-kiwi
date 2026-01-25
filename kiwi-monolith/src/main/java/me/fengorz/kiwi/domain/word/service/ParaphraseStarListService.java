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
import me.fengorz.kiwi.domain.tools.service.SequenceService;
import me.fengorz.kiwi.domain.word.entity.ParaphraseStarList;
import me.fengorz.kiwi.domain.word.mapper.ParaphraseStarListMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * ParaphraseStarList Service - manages user's paraphrase collection lists
 *
 * @author codingByFeng
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ParaphraseStarListService extends ServiceImpl<ParaphraseStarListMapper, ParaphraseStarList> {

    private static final String CACHE_NAME = "paraphraseStarList";

    private final SequenceService sequenceService;

    /**
     * Find list by ID
     */
    public Optional<ParaphraseStarList> findById(Integer listId) {
        return Optional.ofNullable(getById(listId));
    }

    /**
     * Find lists by owner
     */
    @Cacheable(value = CACHE_NAME, key = "'owner:' + #owner")
    public List<ParaphraseStarList> findByOwner(Integer owner) {
        return list(new LambdaQueryWrapper<ParaphraseStarList>()
                .eq(ParaphraseStarList::getOwner, owner)
                .eq(ParaphraseStarList::getIsDel, GlobalConstants.FLAG_N)
                .orderByDesc(ParaphraseStarList::getSort));
    }

    /**
     * Create a new paraphrase star list
     */
    @Transactional
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public ParaphraseStarList create(String listName, String remark, Integer owner) {
        ParaphraseStarList list = ParaphraseStarList.builder()
                .id(sequenceService.generateSequence())
                .listName(listName)
                .remark(remark)
                .owner(owner)
                .isDel(GlobalConstants.FLAG_N)
                .sort(1)
                .build();
        save(list);
        return list;
    }

    /**
     * Save paraphrase star list
     */
    @Transactional
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public ParaphraseStarList saveList(ParaphraseStarList list) {
        if (list.getId() == null) {
            list.setId(sequenceService.generateSequence());
        }
        if (list.getIsDel() == null) {
            list.setIsDel(GlobalConstants.FLAG_N);
        }
        if (list.getSort() == null) {
            list.setSort(1);
        }
        saveOrUpdate(list);
        return list;
    }

    /**
     * Update list
     */
    @Transactional
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public boolean updateList(Integer listId, String listName, String remark) {
        return findById(listId)
                .map(list -> {
                    if (listName != null) {
                        list.setListName(listName);
                    }
                    if (remark != null) {
                        list.setRemark(remark);
                    }
                    updateById(list);
                    return true;
                })
                .orElse(false);
    }

    /**
     * Delete list by ID
     */
    @Transactional
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public boolean deleteById(Integer listId) {
        return removeById(listId);
    }

    /**
     * Evict cache for owner
     */
    @CacheEvict(value = CACHE_NAME, key = "'owner:' + #owner")
    public void evictCacheByOwner(Integer owner) {
        log.debug("Evicted paraphrase star list cache for owner: {}", owner);
    }
}
