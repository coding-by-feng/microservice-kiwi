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
import me.fengorz.kiwi.domain.word.entity.WordStarList;
import me.fengorz.kiwi.domain.word.mapper.WordStarListMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * WordStarList Service - manages user's word collection lists
 *
 * @author codingByFeng
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class WordStarListService extends ServiceImpl<WordStarListMapper, WordStarList> {

    private static final String CACHE_NAME = "wordStarList";

    /**
     * Find list by ID
     */
    public Optional<WordStarList> findById(Integer listId) {
        return Optional.ofNullable(getById(listId));
    }

    /**
     * Find list by ID with word relations
     */
    @Cacheable(value = CACHE_NAME, key = "'id:' + #listId", unless = "#result == null")
    public Optional<WordStarList> findByIdWithWordRelations(Integer listId) {
        return findById(listId);
    }

    /**
     * Find lists by owner
     */
    @Cacheable(value = CACHE_NAME, key = "'owner:' + #owner", unless = "#result == null")
    public List<WordStarList> findByOwner(Integer owner) {
        return list(new LambdaQueryWrapper<WordStarList>()
                .eq(WordStarList::getOwner, owner)
                .eq(WordStarList::getIsDel, GlobalConstants.FLAG_N)
                .orderByDesc(WordStarList::getSort));
    }

    /**
     * Find lists by owner with word relations
     */
    public List<WordStarList> findByOwnerWithWordRelations(Integer owner) {
        return findByOwner(owner);
    }

    /**
     * Create a new word star list
     */
    @Transactional
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public WordStarList create(String listName, String remark, Integer owner) {
        WordStarList list = WordStarList.builder()
                .listName(listName)
                .remark(remark)
                .owner(owner)
                .isDel(GlobalConstants.FLAG_N)
                .build();
        save(list);
        return list;
    }

    /**
     * Save word star list
     */
    @Transactional
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public WordStarList saveList(WordStarList list) {
        if (list.getIsDel() == null) {
            list.setIsDel(GlobalConstants.FLAG_N);
        }
        saveOrUpdate(list);
        return list;
    }

    /**
     * Update list name
     */
    @Transactional
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public boolean updateListName(Integer listId, String listName) {
        return findById(listId)
                .map(list -> {
                    list.setListName(listName);
                    updateById(list);
                    return true;
                })
                .orElse(false);
    }

    /**
     * Update list sort order
     */
    @Transactional
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public boolean updateSort(Integer listId, Integer sort) {
        return findById(listId)
                .map(list -> {
                    list.setSort(sort);
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
    public void deleteById(Integer listId) {
        removeById(listId);
    }

    /**
     * Evict cache for owner
     */
    @CacheEvict(value = CACHE_NAME, key = "'owner:' + #owner")
    public void evictCacheByOwner(Integer owner) {
        log.debug("Evicted word star list cache for owner: {}", owner);
    }
}
