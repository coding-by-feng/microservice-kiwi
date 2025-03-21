package me.fengorz.kiwi.ai.service.ytb;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import me.fengorz.kiwi.ai.api.entity.YtbChannelDO;
import me.fengorz.kiwi.ai.api.vo.ytb.YtbChannelVO;

public interface YtbChannelService extends IService<YtbChannelDO> {
    
    /**
     * Submit a YouTube channel by link or name
     * @param channelLinkOrName Channel link or name
     * @param userId User ID
     * @return Channel ID
     */
    Long submitChannel(String channelLinkOrName, Integer userId);
    
    IPage<YtbChannelVO> getUserChannelPageVO(Page<YtbChannelDO> page, Integer userId);

}