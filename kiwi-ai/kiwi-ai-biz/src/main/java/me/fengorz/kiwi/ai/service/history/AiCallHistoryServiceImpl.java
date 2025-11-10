package me.fengorz.kiwi.ai.service.history;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.ai.api.entity.AiCallHistoryDO;
import me.fengorz.kiwi.ai.api.enums.HistoryFilterEnum;
import me.fengorz.kiwi.ai.api.model.request.AiStreamingRequest;
import me.fengorz.kiwi.ai.api.vo.AiCallHistoryVO;
import me.fengorz.kiwi.ai.service.history.mapper.AiCallHistoryMapper;
import me.fengorz.kiwi.common.db.service.SeqService;
import me.fengorz.kiwi.common.sdk.web.WebTools;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiCallHistoryServiceImpl extends ServiceImpl<AiCallHistoryMapper, AiCallHistoryDO> implements AiCallHistoryService {

    private final SeqService seqService;

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public Long saveCallHistory(AiStreamingRequest request, Long userId) {
        log.info("Saving AI call history for user: {}, prompt mode: {}", userId, request.getPromptMode());

        try {
            // Check if a record with the same aiUrl and userId already exists
            AiCallHistoryDO existingRecord = this.getOne(
                    new LambdaQueryWrapper<AiCallHistoryDO>()
                            .eq(AiCallHistoryDO::getUserId, userId)
                            .eq(AiCallHistoryDO::getPromptMode, request.getPromptMode())
                            .eq(AiCallHistoryDO::getPrompt, request.getPrompt())
                            .eq(AiCallHistoryDO::getIsDelete, false)
                            .last("LIMIT 1")
            );

            if (existingRecord != null) {
                log.info("AI call history already exists for user: {} and aiUrl: {}, skipping save. Existing ID: {}",
                        userId, request.getAiUrl(), existingRecord.getId());
                return existingRecord.getId();
            }

            // Generate ID
            Long id = Long.valueOf(seqService.genCommonIntSequence());

            // Decode the prompt to store readable text
            String decodedPrompt = null;
            if (request.getPrompt() != null) {
                try {
                    decodedPrompt = WebTools.decode(request.getPrompt());
                } catch (Exception e) {
                    log.warn("Failed to decode prompt, using original: {}", e.getMessage());
                    decodedPrompt = request.getPrompt();
                }
            }

            // Convert timestamp from Long to LocalDateTime
            LocalDateTime requestTimestamp = null;
            if (request.getTimestamp() != null) {
                requestTimestamp = LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(request.getTimestamp()),
                        ZoneId.systemDefault()
                );
            }

            // Create history record
            AiCallHistoryDO historyDO = new AiCallHistoryDO()
                    .setId(id)
                    .setUserId(userId)
                    .setAiUrl(request.getAiUrl())
                    .setPrompt(decodedPrompt)
                    .setPromptMode(request.getPromptMode())
                    .setTargetLanguage(request.getTargetLanguage())
                    .setNativeLanguage(request.getNativeLanguage())
                    .setTimestamp(requestTimestamp)
                    .setIsDelete(false)
                    .setIsArchive(false)
                    .setIsFavorite(false)
                    .setCreateTime(LocalDateTime.now());

            // Save to database
            this.save(historyDO);

            log.info("Successfully saved AI call history with ID: {}", id);
            return id;
        } catch (Exception e) {
            log.error("Failed to save AI call history for user: {}, error: {}", userId, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public IPage<AiCallHistoryVO> getUserCallHistory(Page<AiCallHistoryDO> page, Long userId, HistoryFilterEnum filter) {
        log.info("Getting AI call history for user: {}, page: {}, size: {}, filter: {}", 
                userId, page.getCurrent(), page.getSize(), filter.getCode());

        // Build query wrapper based on filter
        LambdaQueryWrapper<AiCallHistoryDO> queryWrapper = new LambdaQueryWrapper<AiCallHistoryDO>()
                .eq(AiCallHistoryDO::getUserId, userId)
                .eq(AiCallHistoryDO::getIsDelete, false);

        // Apply archive filter based on the filter type
        switch (filter) {
            case NORMAL:
                queryWrapper.eq(AiCallHistoryDO::getIsArchive, false);
                break;
            case ARCHIVED:
                queryWrapper.eq(AiCallHistoryDO::getIsArchive, true);
                break;
            case FAVORITE:
                queryWrapper.eq(AiCallHistoryDO::getIsFavorite, true);
                break;
            case ALL:
                // No additional filter for archive status
                break;
        }

        // Add ordering
        queryWrapper.orderByDesc(AiCallHistoryDO::getTimestamp)
                   .orderByDesc(AiCallHistoryDO::getCreateTime);

        // Query user's call history
        IPage<AiCallHistoryDO> historyPage = this.page(page, queryWrapper);

        // Convert DO to VO
        List<AiCallHistoryVO> historyVOList = historyPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        // Create result page
        Page<AiCallHistoryVO> voPage = new Page<>();
        voPage.setCurrent(historyPage.getCurrent());
        voPage.setSize(historyPage.getSize());
        voPage.setTotal(historyPage.getTotal());
        voPage.setRecords(historyVOList);

        log.info("Retrieved {} AI call history records for user: {} with filter: {}", 
                historyVOList.size(), userId, filter.getCode());
        return voPage;
    }

    @Override
    public IPage<AiCallHistoryVO> getUserCallHistory(Page<AiCallHistoryDO> page, Long userId) {
        log.info("Getting AI call history for user: {}, page: {}, size: {}", 
                userId, page.getCurrent(), page.getSize());

        // Query user's call history ordered by timestamp desc
        IPage<AiCallHistoryDO> historyPage = this.page(page, 
                new LambdaQueryWrapper<AiCallHistoryDO>()
                        .eq(AiCallHistoryDO::getUserId, userId)
                        .eq(AiCallHistoryDO::getIsDelete, false)
                        .eq(AiCallHistoryDO::getIsArchive, false)
                        .orderByDesc(AiCallHistoryDO::getTimestamp)
                        .orderByDesc(AiCallHistoryDO::getCreateTime));

        // Convert DO to VO
        List<AiCallHistoryVO> historyVOList = historyPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        // Create result page
        Page<AiCallHistoryVO> voPage = new Page<>();
        voPage.setCurrent(historyPage.getCurrent());
        voPage.setSize(historyPage.getSize());
        voPage.setTotal(historyPage.getTotal());
        voPage.setRecords(historyVOList);

        log.info("Retrieved {} AI call history records for user: {}", historyVOList.size(), userId);
        return voPage;
    }

    @Override
    public boolean archiveCallHistory(Long id, Long userId) {
        log.info("Archiving AI call history record - ID: {}, User: {}", id, userId);

        try {
            // First check if the record exists and belongs to the user
            AiCallHistoryDO existingRecord = this.getOne(
                    new LambdaQueryWrapper<AiCallHistoryDO>()
                            .eq(AiCallHistoryDO::getId, id)
                            .eq(AiCallHistoryDO::getUserId, userId)
                            .eq(AiCallHistoryDO::getIsDelete, false));

            if (existingRecord == null) {
                log.warn("AI call history record not found or doesn't belong to user - ID: {}, User: {}", id, userId);
                return false;
            }

            // Check if already archived
            if (Boolean.TRUE.equals(existingRecord.getIsArchive())) {
                log.info("AI call history record is already archived - ID: {}", id);
                return true;
            }

            // Archive the record
            existingRecord.setIsArchive(true);
            existingRecord.setUpdateTime(LocalDateTime.now());
            
            boolean result = this.updateById(existingRecord);
            
            if (result) {
                log.info("Successfully archived AI call history record - ID: {}", id);
            } else {
                log.error("Failed to archive AI call history record - ID: {}", id);
            }
            
            return result;
        } catch (Exception e) {
            log.error("Error archiving AI call history record - ID: {}, User: {}, Error: {}", 
                    id, userId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean deleteCallHistory(Long id, Long userId) {
        log.info("Deleting AI call history record - ID: {}, User: {}", id, userId);

        try {
            // First check if the record exists and belongs to the user
            AiCallHistoryDO existingRecord = this.getOne(
                    new LambdaQueryWrapper<AiCallHistoryDO>()
                            .eq(AiCallHistoryDO::getId, id)
                            .eq(AiCallHistoryDO::getUserId, userId)
                            .eq(AiCallHistoryDO::getIsDelete, false));

            if (existingRecord == null) {
                log.warn("AI call history record not found or doesn't belong to user - ID: {}, User: {}", id, userId);
                return false;
            }

            // Check if already deleted
            if (Boolean.TRUE.equals(existingRecord.getIsDelete())) {
                log.info("AI call history record is already deleted - ID: {}", id);
                return true;
            }

            // Soft delete the record
            existingRecord.setIsDelete(true);
            existingRecord.setUpdateTime(LocalDateTime.now());
            
            boolean result = this.updateById(existingRecord);
            
            if (result) {
                log.info("Successfully deleted AI call history record - ID: {}", id);
            } else {
                log.error("Failed to delete AI call history record - ID: {}", id);
            }
            
            return result;
        } catch (Exception e) {
            log.error("Error deleting AI call history record - ID: {}, User: {}, Error: {}", 
                    id, userId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean setFavoriteStatus(Long id, Long userId, Boolean favorite) {
        log.info("Setting favorite status - ID: {}, User: {}, favorite: {}", id, userId, favorite);
        try {
            AiCallHistoryDO existingRecord = this.getOne(new LambdaQueryWrapper<AiCallHistoryDO>()
                    .eq(AiCallHistoryDO::getId, id)
                    .eq(AiCallHistoryDO::getUserId, userId)
                    .eq(AiCallHistoryDO::getIsDelete, false));
            if (existingRecord == null) {
                log.warn("Record not found or not owned by user - ID: {}, User: {}", id, userId);
                return false;
            }
            if (favorite != null && favorite.equals(existingRecord.getIsFavorite())) {
                log.info("Favorite status unchanged - ID: {}", id);
                return true;
            }
            existingRecord.setIsFavorite(Boolean.TRUE.equals(favorite));
            existingRecord.setUpdateTime(LocalDateTime.now());
            boolean result = this.updateById(existingRecord);
            if (result) {
                log.info("Favorite status updated - ID: {}", id);
            } else {
                log.error("Failed to update favorite status - ID: {}", id);
            }
            return result;
        } catch (Exception e) {
            log.error("Error updating favorite status - ID: {}, User: {}, Error: {}", id, userId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Convert DO to VO
     */
    private AiCallHistoryVO convertToVO(AiCallHistoryDO historyDO) {
        AiCallHistoryVO vo = new AiCallHistoryVO();
        BeanUtils.copyProperties(historyDO, vo);
        return vo;
    }
}