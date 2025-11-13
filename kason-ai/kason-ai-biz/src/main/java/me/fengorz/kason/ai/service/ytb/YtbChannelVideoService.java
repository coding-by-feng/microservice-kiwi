package me.fengorz.kason.ai.service.ytb;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import me.fengorz.kason.ai.api.entity.YtbChannelVideoDO;
import me.fengorz.kason.ai.api.vo.ytb.YtbChannelVideoVO;

public interface YtbChannelVideoService extends IService<YtbChannelVideoDO> {

    /**
     * Get videos page by channel ID
     * @param page Page parameters
     * @param channelId Channel ID
     * @return Video page
     */
    IPage<YtbChannelVideoVO> getVideosByChannelId(Page<YtbChannelVideoDO> page, Long channelId);

}