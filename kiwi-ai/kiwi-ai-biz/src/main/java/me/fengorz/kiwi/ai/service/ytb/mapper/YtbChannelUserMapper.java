package me.fengorz.kiwi.ai.service.ytb.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import me.fengorz.kiwi.ai.api.entity.YtbChannelUserDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * User-channel subscription relationship Mapper
 */
@Mapper
public interface YtbChannelUserMapper extends BaseMapper<YtbChannelUserDO> {
}