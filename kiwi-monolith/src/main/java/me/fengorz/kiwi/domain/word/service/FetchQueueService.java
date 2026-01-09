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
import me.fengorz.kiwi.domain.word.entity.FetchQueue;
import me.fengorz.kiwi.domain.word.mapper.FetchQueueMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * FetchQueue Service - manages word fetching queue
 *
 * @author codingByFeng
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class FetchQueueService extends ServiceImpl<FetchQueueMapper, FetchQueue> {

    /**
     * Find by ID
     */
    public Optional<FetchQueue> findById(Integer queueId) {
        return Optional.ofNullable(getById(queueId));
    }

    /**
     * Find by word name
     */
    public Optional<FetchQueue> findByWordName(String wordName) {
        return Optional.ofNullable(getOne(new LambdaQueryWrapper<FetchQueue>()
                .eq(FetchQueue::getWordName, wordName)));
    }

    /**
     * Find waiting items
     */
    public List<FetchQueue> findWaitingItems() {
        return list(new LambdaQueryWrapper<FetchQueue>()
                .eq(FetchQueue::getFetchStatus, FetchQueue.STATUS_WAITING)
                .eq(FetchQueue::getIsValid, FetchQueue.VALID_YES)
                .orderByAsc(FetchQueue::getFetchPriority));
    }

    /**
     * Find waiting items with pagination
     */
    public Page<FetchQueue> findWaitingItems(Page<FetchQueue> page) {
        return page(page, new LambdaQueryWrapper<FetchQueue>()
                .eq(FetchQueue::getFetchStatus, FetchQueue.STATUS_WAITING)
                .eq(FetchQueue::getIsValid, FetchQueue.VALID_YES)
                .orderByAsc(FetchQueue::getFetchPriority));
    }

    /**
     * Get next item to fetch
     */
    @Transactional
    public Optional<FetchQueue> getNextToFetch() {
        Page<FetchQueue> page = new Page<>(1, 1);
        Page<FetchQueue> items = page(page, new LambdaQueryWrapper<FetchQueue>()
                .eq(FetchQueue::getFetchStatus, FetchQueue.STATUS_WAITING)
                .eq(FetchQueue::getIsValid, FetchQueue.VALID_YES)
                .eq(FetchQueue::getIsLock, FetchQueue.LOCK_NO)
                .orderByAsc(FetchQueue::getFetchPriority));

        if (items.getRecords().isEmpty()) {
            return Optional.empty();
        }

        FetchQueue item = items.getRecords().get(0);
        // Lock the item
        item.setIsLock(FetchQueue.LOCK_YES);
        item.setFetchStatus(FetchQueue.STATUS_FETCHING);
        updateById(item);

        return Optional.of(item);
    }

    /**
     * Check if word exists in queue
     */
    public boolean existsByWordName(String wordName) {
        return count(new LambdaQueryWrapper<FetchQueue>()
                .eq(FetchQueue::getWordName, wordName)) > 0;
    }

    /**
     * Add word to fetch queue
     */
    @Transactional
    public FetchQueue addToQueue(String wordName, Integer priority) {
        if (existsByWordName(wordName)) {
            log.debug("Word already in queue: {}", wordName);
            return findByWordName(wordName).orElse(null);
        }

        FetchQueue queue = FetchQueue.builder()
                .wordName(wordName)
                .fetchPriority(priority != null ? priority : 100)
                .fetchStatus(FetchQueue.STATUS_WAITING)
                .isValid(FetchQueue.VALID_YES)
                .isLock(FetchQueue.LOCK_NO)
                .inTime(LocalDateTime.now())
                .build();

        save(queue);
        return queue;
    }

    /**
     * Mark fetch as success
     */
    @Transactional
    public void markSuccess(Integer queueId, Integer wordId) {
        findById(queueId).ifPresent(queue -> {
            queue.setFetchStatus(FetchQueue.STATUS_SUCCESS);
            queue.setWordId(wordId);
            queue.setOperateTime(LocalDateTime.now());
            queue.unlock();
            updateById(queue);
        });
    }

    /**
     * Mark fetch as failed
     */
    @Transactional
    public void markFailed(Integer queueId, String errorMessage) {
        findById(queueId).ifPresent(queue -> {
            queue.setFetchStatus(FetchQueue.STATUS_FAIL);
            queue.setFetchResult(errorMessage);
            queue.setOperateTime(LocalDateTime.now());
            queue.setFetchTime(queue.getFetchTime() != null ? queue.getFetchTime() + 1 : 1);
            queue.unlock();
            updateById(queue);
        });
    }

    /**
     * Reset failed items for retry
     */
    @Transactional
    public void resetFailedItems(int maxRetries) {
        List<FetchQueue> failedItems = list(new LambdaQueryWrapper<FetchQueue>()
                .eq(FetchQueue::getFetchStatus, FetchQueue.STATUS_FAIL)
                .eq(FetchQueue::getIsValid, FetchQueue.VALID_YES));

        failedItems.stream()
                .filter(item -> item.getFetchTime() == null || item.getFetchTime() < maxRetries)
                .forEach(item -> {
                    item.setFetchStatus(FetchQueue.STATUS_WAITING);
                    item.setOperateTime(LocalDateTime.now());
                    updateById(item);
                });
    }

    /**
     * Count by status
     */
    public long countByStatus(Integer status) {
        return count(new LambdaQueryWrapper<FetchQueue>()
                .eq(FetchQueue::getFetchStatus, status));
    }

    /**
     * Delete by ID
     */
    @Transactional
    public void deleteById(Integer queueId) {
        removeById(queueId);
    }
}
