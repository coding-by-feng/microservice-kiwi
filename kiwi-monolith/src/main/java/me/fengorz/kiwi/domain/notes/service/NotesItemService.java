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
import me.fengorz.kiwi.domain.notes.dto.NotesItemRequest;
import me.fengorz.kiwi.domain.notes.entity.MediaStatus;
import me.fengorz.kiwi.domain.notes.entity.NotesItem;
import me.fengorz.kiwi.domain.notes.mapper.NotesItemMapper;
import me.fengorz.kiwi.domain.notes.vo.NotesItemVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Notes Item Service
 *
 * @author codingByFeng
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotesItemService extends ServiceImpl<NotesItemMapper, NotesItem> {

    private final NotesCategoryService categoryService;

    /**
     * List items by category with navigation info
     */
    public List<NotesItemVO> listByCategoryId(Long categoryId, Integer userId) {
        categoryService.verifyOwnership(categoryId, userId);

        List<NotesItem> items = list(new LambdaQueryWrapper<NotesItem>()
                .eq(NotesItem::getCategoryId, categoryId)
                .eq(NotesItem::getIsDel, GlobalConstants.FLAG_N)
                .orderByAsc(NotesItem::getDisplayOrder)
                .orderByDesc(NotesItem::getCreateTime));

        return items.stream()
                .map(item -> {
                    NotesItemVO vo = NotesItemVO.fromEntity(item);
                    vo.setHasPrevious(baseMapper.findPreviousItem(categoryId, item.getDisplayOrder()) != null);
                    vo.setHasNext(baseMapper.findNextItem(categoryId, item.getDisplayOrder()) != null);
                    return vo;
                })
                .collect(Collectors.toList());
    }

    /**
     * Get item by ID with navigation info
     */
    public NotesItemVO getByIdAndUser(Long id, Integer userId) {
        NotesItem item = getById(id);
        if (item == null || GlobalConstants.FLAG_Y.equals(item.getIsDel())) {
            return null;
        }
        if (!item.getUserId().equals(userId)) {
            throw new ServiceException("Access denied");
        }

        NotesItemVO vo = NotesItemVO.fromEntity(item);
        vo.setHasPrevious(baseMapper.findPreviousItem(item.getCategoryId(), item.getDisplayOrder()) != null);
        vo.setHasNext(baseMapper.findNextItem(item.getCategoryId(), item.getDisplayOrder()) != null);

        return vo;
    }

    /**
     * Get next item in category (loops to first if at end)
     */
    public NotesItemVO getNextItem(Long currentItemId, Integer userId) {
        NotesItem current = getById(currentItemId);
        if (current == null || !current.getUserId().equals(userId)) {
            throw new ServiceException("Access denied");
        }

        NotesItem next = baseMapper.findNextItem(current.getCategoryId(), current.getDisplayOrder());
        if (next == null) {
            // Loop to first item
            next = baseMapper.findFirstItem(current.getCategoryId());
        }

        if (next == null) {
            return null;
        }

        NotesItemVO vo = NotesItemVO.fromEntity(next);
        vo.setHasPrevious(true);
        vo.setHasNext(baseMapper.findNextItem(next.getCategoryId(), next.getDisplayOrder()) != null
                || baseMapper.findFirstItem(next.getCategoryId()) != null);
        return vo;
    }

    /**
     * Get previous item in category (loops to last if at beginning)
     */
    public NotesItemVO getPreviousItem(Long currentItemId, Integer userId) {
        NotesItem current = getById(currentItemId);
        if (current == null || !current.getUserId().equals(userId)) {
            throw new ServiceException("Access denied");
        }

        NotesItem prev = baseMapper.findPreviousItem(current.getCategoryId(), current.getDisplayOrder());
        if (prev == null) {
            // Loop to last item
            prev = baseMapper.findLastItem(current.getCategoryId());
        }

        if (prev == null) {
            return null;
        }

        NotesItemVO vo = NotesItemVO.fromEntity(prev);
        vo.setHasPrevious(baseMapper.findPreviousItem(prev.getCategoryId(), prev.getDisplayOrder()) != null
                || baseMapper.findLastItem(prev.getCategoryId()) != null);
        vo.setHasNext(true);
        return vo;
    }

    /**
     * Create a new note item
     */
    @Transactional
    public NotesItemVO create(NotesItemRequest request, Integer userId) {
        categoryService.verifyOwnership(request.getCategoryId(), userId);

        Integer maxOrder = baseMapper.findMaxDisplayOrder(request.getCategoryId());
        int newOrder = request.getDisplayOrder() != null ? request.getDisplayOrder() : maxOrder + 1;

        NotesItem item = NotesItem.builder()
                .categoryId(request.getCategoryId())
                .userId(userId)
                .content(request.getContent())
                .displayOrder(newOrder)
                .imagePrompt(request.getImagePrompt())
                .imageStyle(request.getImageStyle())
                .imageStatus(MediaStatus.NONE.getCode())
                .audioStatus(MediaStatus.NONE.getCode())
                .audioDurationMs(0)
                .isDel(GlobalConstants.FLAG_N)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();
        save(item);

        log.info("Created notes item {} for user {}", item.getId(), userId);
        return NotesItemVO.fromEntity(item);
    }

    /**
     * Update an existing note item
     */
    @Transactional
    public NotesItemVO update(Long id, NotesItemRequest request, Integer userId) {
        NotesItem item = getById(id);
        if (item == null || GlobalConstants.FLAG_Y.equals(item.getIsDel())) {
            throw new ServiceException("Note item not found");
        }
        if (!item.getUserId().equals(userId)) {
            throw new ServiceException("Access denied");
        }

        item.setContent(request.getContent());
        if (request.getDisplayOrder() != null) {
            item.setDisplayOrder(request.getDisplayOrder());
        }
        item.setImagePrompt(request.getImagePrompt());
        item.setImageStyle(request.getImageStyle());
        item.setUpdateTime(LocalDateTime.now());
        updateById(item);

        log.info("Updated notes item {} for user {}", id, userId);
        return NotesItemVO.fromEntity(item);
    }

    /**
     * Delete a note item (physical delete)
     */
    @Transactional
    public void delete(Long id, Integer userId) {
        NotesItem item = getById(id);
        if (item == null) {
            throw new ServiceException("Note item not found");
        }
        if (!item.getUserId().equals(userId)) {
            throw new ServiceException("Access denied");
        }

        removeById(id);

        log.info("Physically deleted notes item {} for user {}", id, userId);
    }

    /**
     * Reorder items in a category
     */
    @Transactional
    public void reorder(Long categoryId, List<Long> itemIds, Integer userId) {
        categoryService.verifyOwnership(categoryId, userId);

        for (int i = 0; i < itemIds.size(); i++) {
            NotesItem item = getById(itemIds.get(i));
            if (item != null && item.getCategoryId().equals(categoryId) && item.getUserId().equals(userId)) {
                item.setDisplayOrder(i + 1);
                item.setUpdateTime(LocalDateTime.now());
                updateById(item);
            }
        }

        log.info("Reordered {} items in category {} for user {}", itemIds.size(), categoryId, userId);
    }
}
