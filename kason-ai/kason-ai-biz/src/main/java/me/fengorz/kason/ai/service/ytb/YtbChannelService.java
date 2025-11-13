package me.fengorz.kason.ai.service.ytb;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import me.fengorz.kason.ai.api.entity.YtbChannelDO;
import me.fengorz.kason.ai.api.vo.ytb.YtbChannelVO;
import me.fengorz.kason.common.sdk.enumeration.ProcessStatusEnum;

public interface YtbChannelService extends IService<YtbChannelDO> {

    /**
     * Submit a YouTube channel by link or name
     *
     * @param channelLinkOrName Channel link or name
     * @param userId            User ID
     * @return Channel ID
     */
    Long submitChannel(String channelLinkOrName, Integer userId);

    IPage<YtbChannelVO> getUserChannelPageVO(Page<YtbChannelDO> page, Integer userId);


    void syncChannelVideos(Long channelId);

    /**
     * Updates the channel's status
     *
     * @param previousLogFormat The log format to use before updating the channel
     * @param channelId The ID of the channel to update
     * @param channel The channel object to update
     * @param status The new status to set
     * @param postLogFormat The log format to use after updating the channel
     */
    void updateChannelStatus(String previousLogFormat, Long channelId, YtbChannelDO channel, ProcessStatusEnum status, String postLogFormat);

}