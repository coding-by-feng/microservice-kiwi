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
package me.fengorz.kiwi.domain.notes.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.constant.GlobalConstants;
import me.fengorz.kiwi.common.exception.ServiceException;
import me.fengorz.kiwi.domain.notes.dto.NotesCategoryRequest;
import me.fengorz.kiwi.domain.notes.entity.NotesCategory;
import me.fengorz.kiwi.domain.notes.entity.NotesItem;
import me.fengorz.kiwi.domain.notes.mapper.NotesCategoryMapper;
import me.fengorz.kiwi.domain.notes.mapper.NotesItemMapper;
import me.fengorz.kiwi.domain.notes.vo.NotesCategoryVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Notes Category Service
 *
 * @author codingByFeng
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotesCategoryService extends ServiceImpl<NotesCategoryMapper, NotesCategory> {

    private final NotesItemMapper notesItemMapper;

    /**
     * List all categories for a user
     */
    public List<NotesCategoryVO> listByUser(Integer userId) {
        List<NotesCategory> categories = list(new LambdaQueryWrapper<NotesCategory>()
                .eq(NotesCategory::getUserId, userId)
                .eq(NotesCategory::getIsDel, GlobalConstants.FLAG_N)
                .orderByAsc(NotesCategory::getSortOrder)
                .orderByDesc(NotesCategory::getCreateTime));

        return categories.stream()
                .map(cat -> {
                    NotesCategoryVO vo = NotesCategoryVO.fromEntity(cat);
                    Long count = notesItemMapper.selectCount(
                            new LambdaQueryWrapper<NotesItem>()
                                    .eq(NotesItem::getCategoryId, cat.getId())
                                    .eq(NotesItem::getIsDel, GlobalConstants.FLAG_N));
                    vo.setItemCount(count.intValue());
                    return vo;
                })
                .collect(Collectors.toList());
    }

    /**
     * Get category by ID and verify ownership
     */
    public NotesCategoryVO getByIdAndUser(Long id, Integer userId) {
        NotesCategory category = getById(id);
        if (category == null || GlobalConstants.FLAG_Y.equals(category.getIsDel())) {
            return null;
        }
        if (!category.getUserId().equals(userId)) {
            throw new ServiceException("Access denied");
        }
        NotesCategoryVO vo = NotesCategoryVO.fromEntity(category);
        Long count = notesItemMapper.selectCount(
                new LambdaQueryWrapper<NotesItem>()
                        .eq(NotesItem::getCategoryId, category.getId())
                        .eq(NotesItem::getIsDel, GlobalConstants.FLAG_N));
        vo.setItemCount(count.intValue());
        return vo;
    }

    /**
     * Create a new category
     */
    @Transactional
    public NotesCategoryVO create(NotesCategoryRequest request, Integer userId) {
        NotesCategory category = NotesCategory.builder()
                .userId(userId)
                .name(request.getName())
                .description(request.getDescription())
                .color(request.getColor())
                .icon(request.getIcon())
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .isDel(GlobalConstants.FLAG_N)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();
        save(category);
        log.info("Created notes category {} for user {}", category.getId(), userId);
        return NotesCategoryVO.fromEntity(category);
    }

    /**
     * Update an existing category
     */
    @Transactional
    public NotesCategoryVO update(Long id, NotesCategoryRequest request, Integer userId) {
        NotesCategory category = getById(id);
        if (category == null || GlobalConstants.FLAG_Y.equals(category.getIsDel())) {
            throw new ServiceException("Category not found");
        }
        if (!category.getUserId().equals(userId)) {
            throw new ServiceException("Access denied");
        }

        category.setName(request.getName());
        category.setDescription(request.getDescription());
        category.setColor(request.getColor());
        category.setIcon(request.getIcon());
        if (request.getSortOrder() != null) {
            category.setSortOrder(request.getSortOrder());
        }
        category.setUpdateTime(LocalDateTime.now());
        updateById(category);

        log.info("Updated notes category {} for user {}", id, userId);
        return NotesCategoryVO.fromEntity(category);
    }

    /**
     * Delete a category (physical delete)
     */
    @Transactional
    public void delete(Long id, Integer userId) {
        NotesCategory category = getById(id);
        if (category == null) {
            throw new ServiceException("Category not found");
        }
        if (!category.getUserId().equals(userId)) {
            throw new ServiceException("Access denied");
        }

        // Delete all items in this category first
        notesItemMapper.delete(new LambdaQueryWrapper<NotesItem>()
                .eq(NotesItem::getCategoryId, id));

        // Delete the category
        removeById(id);

        log.info("Physically deleted notes category {} and its items for user {}", id, userId);
    }

    /**
     * Verify ownership of a category
     */
    public void verifyOwnership(Long categoryId, Integer userId) {
        NotesCategory category = getById(categoryId);
        if (category == null || GlobalConstants.FLAG_Y.equals(category.getIsDel())) {
            throw new ServiceException("Category not found");
        }
        if (!category.getUserId().equals(userId)) {
            throw new ServiceException("Access denied");
        }
    }
}
