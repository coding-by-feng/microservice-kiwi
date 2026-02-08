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
package me.fengorz.kiwi.domain.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.domain.ai.entity.AiCallHistory;
import me.fengorz.kiwi.domain.ai.mapper.AiCallHistoryMapper;
import me.fengorz.kiwi.domain.ai.vo.AiCallHistoryVO;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * AiCallHistory Service
 *
 * @author codingByFeng
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class AiCallHistoryService extends ServiceImpl<AiCallHistoryMapper, AiCallHistory> {

    /**
     * Find by ID (excludes soft-deleted records)
     */
    public Optional<AiCallHistory> findById(Long id) {
        return Optional.ofNullable(getOne(new LambdaQueryWrapper<AiCallHistory>()
                .eq(AiCallHistory::getId, id)
                .eq(AiCallHistory::getIsDelete, false)));
    }

    /**
     * Find history by user ID with pagination (excludes soft-deleted records)
     */
    public Page<AiCallHistory> findByUserId(Long userId, Page<AiCallHistory> page) {
        return page(page, new LambdaQueryWrapper<AiCallHistory>()
                .eq(AiCallHistory::getUserId, userId)
                .eq(AiCallHistory::getIsDelete, false)
                .orderByDesc(AiCallHistory::getCreateTime));
    }

    /**
     * Find favorites by user ID (excludes soft-deleted records)
     */
    public List<AiCallHistory> findFavoritesByUserId(Long userId) {
        return list(new LambdaQueryWrapper<AiCallHistory>()
                .eq(AiCallHistory::getUserId, userId)
                .eq(AiCallHistory::getIsDelete, false)
                .eq(AiCallHistory::getIsFavorite, true)
                .orderByDesc(AiCallHistory::getCreateTime));
    }

    /**
     * Find archived by user ID (excludes soft-deleted records)
     */
    public Page<AiCallHistory> findArchivedByUserId(Long userId, Page<AiCallHistory> page) {
        return page(page, new LambdaQueryWrapper<AiCallHistory>()
                .eq(AiCallHistory::getUserId, userId)
                .eq(AiCallHistory::getIsDelete, false)
                .eq(AiCallHistory::getIsArchive, true)
                .orderByDesc(AiCallHistory::getCreateTime));
    }

    /**
     * Find by prompt mode (excludes soft-deleted records)
     */
    public Page<AiCallHistory> findByPromptMode(Long userId, String promptMode, Page<AiCallHistory> page) {
        return page(page, new LambdaQueryWrapper<AiCallHistory>()
                .eq(AiCallHistory::getUserId, userId)
                .eq(AiCallHistory::getIsDelete, false)
                .eq(AiCallHistory::getPromptMode, promptMode)
                .orderByDesc(AiCallHistory::getCreateTime));
    }

    /**
     * Save history
     */
    @Transactional
    public AiCallHistory saveHistory(AiCallHistory history) {
        saveOrUpdate(history);
        return history;
    }

    /**
     * Log AI call (synchronous)
     */
    @Transactional
    public AiCallHistory logCall(Long userId, String aiUrl, String prompt, String promptMode,
                                  String targetLanguage, String nativeLanguage) {
        AiCallHistory history = AiCallHistory.builder()
                .userId(userId)
                .aiUrl(aiUrl)
                .prompt(prompt)
                .promptMode(promptMode)
                .targetLanguage(targetLanguage)
                .nativeLanguage(nativeLanguage)
                .build();

        save(history);
        return history;
    }

    /**
     * Log AI call asynchronously to avoid blocking streaming requests.
     * Returns a CompletableFuture with the history ID for later updates.
     */
    @Async("webSocketExecutor")
    @Transactional
    public CompletableFuture<Long> logCallAsync(Long userId, String aiUrl, String prompt, String promptMode,
                                                 String targetLanguage, String nativeLanguage) {
        try {
            AiCallHistory history = logCall(userId, aiUrl, prompt, promptMode, targetLanguage, nativeLanguage);
            log.debug("Async call history logged: {}", history.getId());
            return CompletableFuture.completedFuture(history.getId());
        } catch (Exception e) {
            log.error("Failed to log call history asynchronously: {}", e.getMessage(), e);
            return CompletableFuture.completedFuture(null);
        }
    }

    /**
     * Toggle favorite
     */
    @Transactional
    public void toggleFavorite(Long id) {
        findById(id).ifPresent(history -> {
            history.setIsFavorite(!Boolean.TRUE.equals(history.getIsFavorite()));
            updateById(history);
        });
    }

    /**
     * Archive history
     */
    @Transactional
    public void archive(Long id) {
        findById(id).ifPresent(history -> {
            history.archive();
            updateById(history);
        });
    }

    /**
     * Delete history (soft delete)
     */
    @Transactional
    public boolean deleteHistory(Long id) {
        return findById(id)
                .map(history -> {
                    history.setIsDelete(true);
                    history.setUpdateTime(LocalDateTime.now());
                    return updateById(history);
                })
                .orElse(false);
    }

    // ==================== Methods for Controller ====================

    /**
     * Get user call history with filter and pagination
     */
    public Page<AiCallHistoryVO> getUserCallHistory(Long userId, String filter, Page<AiCallHistory> page) {
        Page<AiCallHistory> historyPage;
        switch (filter) {
            case "archived":
                historyPage = findArchivedByUserId(userId, page);
                break;
            case "all":
                historyPage = findByUserId(userId, page);
                break;
            default: // "normal" - not archived
                historyPage = page(page, new LambdaQueryWrapper<AiCallHistory>()
                        .eq(AiCallHistory::getUserId, userId)
                        .eq(AiCallHistory::getIsArchive, false)
                        .orderByDesc(AiCallHistory::getCreateTime));
                break;
        }

        List<AiCallHistoryVO> voList = historyPage.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());

        Page<AiCallHistoryVO> resultPage = new Page<>(page.getCurrent(), page.getSize(), historyPage.getTotal());
        resultPage.setRecords(voList);
        return resultPage;
    }

    /**
     * Archive call history
     */
    @Transactional
    public boolean archiveCallHistory(Long id, Long userId) {
        return findById(id)
                .filter(h -> h.getUserId().equals(userId))
                .map(h -> {
                    h.archive();
                    updateById(h);
                    return true;
                })
                .orElse(false);
    }

    /**
     * Delete call history (soft delete)
     */
    @Transactional
    public boolean deleteCallHistory(Long id, Long userId) {
        return findById(id)
                .filter(h -> h.getUserId().equals(userId))
                .map(h -> {
                    h.setIsDelete(true);
                    h.setUpdateTime(LocalDateTime.now());
                    return updateById(h);
                })
                .orElse(false);
    }

    /**
     * Set favorite status
     */
    @Transactional
    public boolean setFavoriteStatus(Long id, Long userId, Boolean favorite) {
        return findById(id)
                .filter(h -> h.getUserId().equals(userId))
                .map(h -> {
                    h.setIsFavorite(favorite);
                    updateById(h);
                    return true;
                })
                .orElse(false);
    }

    /**
     * Update AI response for a history record
     */
    @Transactional
    public void updateAiResponse(Long historyId, String aiResponse) {
        findById(historyId).ifPresent(history -> {
            history.setAiResponse(aiResponse);
            history.setUpdateTime(LocalDateTime.now());
            updateById(history);
            log.debug("Updated AI response for history {}", historyId);
        });
    }

    /**
     * Find by ID and user ID (for authorization check)
     */
    public Optional<AiCallHistory> findByIdAndUserId(Long id, Long userId) {
        return Optional.ofNullable(getOne(new LambdaQueryWrapper<AiCallHistory>()
                .eq(AiCallHistory::getId, id)
                .eq(AiCallHistory::getUserId, userId)
                .eq(AiCallHistory::getIsDelete, false)));
    }

    /**
     * Convert entity to VO
     */
    private AiCallHistoryVO toVO(AiCallHistory entity) {
        return new AiCallHistoryVO()
                .setId(entity.getId())
                .setUserId(entity.getUserId())
                .setPromptMode(entity.getPromptMode())
                .setLanguage(entity.getTargetLanguage())
                .setOriginalText(entity.getPrompt())
                .setResult(entity.getAiResponse())
                .setFavorite(entity.getIsFavorite())
                .setArchived(entity.getIsArchive())
                .setCreateTime(entity.getCreateTime());
    }
}
