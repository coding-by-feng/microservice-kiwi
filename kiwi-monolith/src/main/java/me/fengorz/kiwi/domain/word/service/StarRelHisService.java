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
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.domain.word.entity.StarRelHis;
import me.fengorz.kiwi.domain.word.mapper.StarRelHisMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * StarRelHis Service - manages star relation history records
 *
 * @author codingByFeng
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class StarRelHisService extends ServiceImpl<StarRelHisMapper, StarRelHis> {

    /**
     * Find by ID
     */
    public Optional<StarRelHis> findById(Integer id) {
        return Optional.ofNullable(getById(id));
    }

    /**
     * Find by word name and user
     */
    public List<StarRelHis> findByWordNameAndUserId(String wordName, Integer userId) {
        return list(new LambdaQueryWrapper<StarRelHis>()
                .eq(StarRelHis::getWordName, wordName)
                .eq(StarRelHis::getUserId, userId)
                .eq(StarRelHis::getIsValid, 1));
    }

    /**
     * Find specific archive record
     */
    public Optional<StarRelHis> findByWordNameAndUserIdAndListIdAndType(String wordName, Integer userId, Integer listId, Integer type) {
        return Optional.ofNullable(getOne(new LambdaQueryWrapper<StarRelHis>()
                .eq(StarRelHis::getWordName, wordName)
                .eq(StarRelHis::getUserId, userId)
                .eq(StarRelHis::getListId, listId)
                .eq(StarRelHis::getType, type)
                .eq(StarRelHis::getIsValid, 1)));
    }

    /**
     * Find by user and type
     */
    public List<StarRelHis> findByUserIdAndType(Integer userId, Integer type) {
        return list(new LambdaQueryWrapper<StarRelHis>()
                .eq(StarRelHis::getUserId, userId)
                .eq(StarRelHis::getType, type)
                .eq(StarRelHis::getIsValid, 1));
    }

    /**
     * Find by list
     */
    public List<StarRelHis> findByListId(Integer listId) {
        return list(new LambdaQueryWrapper<StarRelHis>()
                .eq(StarRelHis::getListId, listId)
                .eq(StarRelHis::getIsValid, 1));
    }

    /**
     * Save archive record
     */
    @Transactional
    public StarRelHis saveHis(StarRelHis starRelHis) {
        if (starRelHis.getIsValid() == null) {
            starRelHis.setIsValid(1);
        }
        saveOrUpdate(starRelHis);
        return starRelHis;
    }

    /**
     * Invalidate (soft delete) an archive record
     */
    @Transactional
    public boolean invalidate(String wordName, Integer userId, Integer listId, Integer type) {
        return update(new LambdaUpdateWrapper<StarRelHis>()
                .eq(StarRelHis::getWordName, wordName)
                .eq(StarRelHis::getUserId, userId)
                .eq(StarRelHis::getListId, listId)
                .eq(StarRelHis::getType, type)
                .set(StarRelHis::getIsValid, 0));
    }

    /**
     * Delete by ID
     */
    @Transactional
    public void deleteById(Integer id) {
        removeById(id);
    }

    /**
     * Delete by word name
     */
    @Transactional
    public void deleteByWordName(String wordName) {
        remove(new LambdaQueryWrapper<StarRelHis>()
                .eq(StarRelHis::getWordName, wordName));
    }

    /**
     * Delete by user
     */
    @Transactional
    public void deleteByUserId(Integer userId) {
        remove(new LambdaQueryWrapper<StarRelHis>()
                .eq(StarRelHis::getUserId, userId));
    }

    /**
     * Delete by list
     */
    @Transactional
    public void deleteByListId(Integer listId) {
        remove(new LambdaQueryWrapper<StarRelHis>()
                .eq(StarRelHis::getListId, listId));
    }
}
