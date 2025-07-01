package me.fengorz.kiwi.ai.service.history.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import me.fengorz.kiwi.ai.api.entity.AiCallHistoryDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI call history Mapper
 */
@Mapper
public interface AiCallHistoryMapper extends BaseMapper<AiCallHistoryDO> {
}