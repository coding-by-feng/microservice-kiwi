package me.fengorz.kiwi.ai.service.history;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.ai.api.entity.AiCallHistoryDO;
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
    public IPage<AiCallHistoryVO> getUserCallHistory(Page<AiCallHistoryDO> page, Long userId) {
        log.info("Getting AI call history for user: {}, page: {}, size: {}", 
                userId, page.getCurrent(), page.getSize());

        // Query user's call history ordered by timestamp desc
        IPage<AiCallHistoryDO> historyPage = this.page(page, 
                new LambdaQueryWrapper<AiCallHistoryDO>()
                        .eq(AiCallHistoryDO::getUserId, userId)
                        .eq(AiCallHistoryDO::getIsDelete, false)
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

    /**
     * Convert DO to VO
     */
    private AiCallHistoryVO convertToVO(AiCallHistoryDO historyDO) {
        AiCallHistoryVO vo = new AiCallHistoryVO();
        BeanUtils.copyProperties(historyDO, vo);
        return vo;
    }
}