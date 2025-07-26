package me.fengorz.kiwi.ai.service.history;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import me.fengorz.kiwi.ai.api.entity.AiCallHistoryDO;
import me.fengorz.kiwi.ai.api.enums.HistoryFilterEnum;
import me.fengorz.kiwi.ai.api.model.request.AiStreamingRequest;
import me.fengorz.kiwi.ai.api.vo.AiCallHistoryVO;

/**
 * AI call history service interface
 */
public interface AiCallHistoryService extends IService<AiCallHistoryDO> {

    /**
     * Save AI call history from WebSocket streaming request
     *
     * @param request the AI streaming request
     * @param userId  the user ID
     * @return the saved record ID
     */
    Long saveCallHistory(AiStreamingRequest request, Long userId);

    /**
     * Get user's AI call history with pagination, ordered by timestamp desc
     *
     * @param page   pagination parameters
     * @param userId user ID
     * @param filter filter type (normal, archived, all)
     * @return paginated AI call history
     */
    IPage<AiCallHistoryVO> getUserCallHistory(Page<AiCallHistoryDO> page, Long userId, HistoryFilterEnum filter);

    /**
     * Get user's AI call history with pagination, ordered by timestamp desc (backward compatibility)
     *
     * @param page   pagination parameters
     * @param userId user ID
     * @return paginated AI call history (normal items only)
     */
    default IPage<AiCallHistoryVO> getUserCallHistory(Page<AiCallHistoryDO> page, Long userId) {
        return getUserCallHistory(page, userId, HistoryFilterEnum.NORMAL);
    }

    /**
     * Archive AI call history record by ID
     *
     * @param id     record ID
     * @param userId user ID (for security check)
     * @return true if archived successfully, false otherwise
     */
    boolean archiveCallHistory(Long id, Long userId);

    /**
     * Delete AI call history record by ID (soft delete)
     *
     * @param id     record ID
     * @param userId user ID (for security check)
     * @return true if deleted successfully, false otherwise
     */
    boolean deleteCallHistory(Long id, Long userId);
}