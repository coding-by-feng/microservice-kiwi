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
import me.fengorz.kiwi.domain.word.entity.ExampleStarRel;
import me.fengorz.kiwi.domain.word.mapper.ExampleStarRelMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * ExampleStarRel Service - manages example-list relationships
 *
 * @author codingByFeng
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class ExampleStarRelService extends ServiceImpl<ExampleStarRelMapper, ExampleStarRel> {

    private static final String CACHE_NAME = "exampleStarRel";

    /**
     * Find relation by composite ID
     */
    public Optional<ExampleStarRel> findById(Integer listId, Integer exampleId) {
        return Optional.ofNullable(getOne(new LambdaQueryWrapper<ExampleStarRel>()
                .eq(ExampleStarRel::getListId, listId)
                .eq(ExampleStarRel::getExampleId, exampleId)));
    }

    /**
     * Find relations by list ID
     */
    @Cacheable(value = CACHE_NAME, key = "'list:' + #listId", unless = "#result == null")
    public List<ExampleStarRel> findByListId(Integer listId) {
        return list(new LambdaQueryWrapper<ExampleStarRel>()
                .eq(ExampleStarRel::getListId, listId));
    }

    /**
     * Find relations by list ID with pagination
     */
    public Page<ExampleStarRel> findByListId(Integer listId, Page<ExampleStarRel> page) {
        return page(page, new LambdaQueryWrapper<ExampleStarRel>()
                .eq(ExampleStarRel::getListId, listId));
    }

    /**
     * Find relations by example ID
     */
    public List<ExampleStarRel> findByExampleId(Integer exampleId) {
        return list(new LambdaQueryWrapper<ExampleStarRel>()
                .eq(ExampleStarRel::getExampleId, exampleId));
    }

    /**
     * Count examples in a list
     */
    public long countByListId(Integer listId) {
        return count(new LambdaQueryWrapper<ExampleStarRel>()
                .eq(ExampleStarRel::getListId, listId));
    }

    /**
     * Check if example is in list
     */
    public boolean existsByListIdAndExampleId(Integer listId, Integer exampleId) {
        return count(new LambdaQueryWrapper<ExampleStarRel>()
                .eq(ExampleStarRel::getListId, listId)
                .eq(ExampleStarRel::getExampleId, exampleId)) > 0;
    }

    /**
     * Add example to list
     */
    @Transactional
    @CacheEvict(value = CACHE_NAME, key = "'list:' + #listId")
    public ExampleStarRel addExampleToList(Integer listId, Integer exampleId) {
        if (existsByListIdAndExampleId(listId, exampleId)) {
            log.debug("Example {} already in list {}", exampleId, listId);
            return findById(listId, exampleId).orElse(null);
        }

        ExampleStarRel rel = ExampleStarRel.builder()
                .listId(listId)
                .exampleId(exampleId)
                .build();
        save(rel);
        return rel;
    }

    /**
     * Remove example from list
     */
    @Transactional
    @CacheEvict(value = CACHE_NAME, key = "'list:' + #listId")
    public void removeExampleFromList(Integer listId, Integer exampleId) {
        remove(new LambdaQueryWrapper<ExampleStarRel>()
                .eq(ExampleStarRel::getListId, listId)
                .eq(ExampleStarRel::getExampleId, exampleId));
    }

    /**
     * Delete all relations by list ID
     */
    @Transactional
    @CacheEvict(value = CACHE_NAME, key = "'list:' + #listId")
    public void deleteByListId(Integer listId) {
        remove(new LambdaQueryWrapper<ExampleStarRel>()
                .eq(ExampleStarRel::getListId, listId));
    }
}
