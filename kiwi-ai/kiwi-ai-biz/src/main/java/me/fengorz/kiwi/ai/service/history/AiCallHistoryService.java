package me.fengorz.kiwi.ai.service.history;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import me.fengorz.kiwi.ai.api.entity.AiCallHistoryDO;
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
     * @return paginated AI call history
     */
    IPage<AiCallHistoryVO> getUserCallHistory(Page<AiCallHistoryDO> page, Long userId);
}