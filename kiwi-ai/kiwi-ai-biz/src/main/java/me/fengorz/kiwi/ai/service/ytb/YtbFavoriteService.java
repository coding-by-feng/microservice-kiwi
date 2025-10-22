package me.fengorz.kiwi.ai.service.ytb;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import me.fengorz.kiwi.ai.api.entity.YtbChannelDO;
import me.fengorz.kiwi.ai.api.entity.YtbChannelVideoDO;
import me.fengorz.kiwi.ai.api.vo.ytb.YtbChannelVO;
import me.fengorz.kiwi.ai.api.vo.ytb.YtbChannelVideoVO;

public interface YtbFavoriteService {

    boolean favoriteChannel(Integer userId, Long channelId);

    boolean unfavoriteChannel(Integer userId, Long channelId);

    boolean favoriteVideo(Integer userId, Long videoId);

    boolean unfavoriteVideo(Integer userId, Long videoId);

    IPage<YtbChannelVO> getFavoriteChannels(Page<YtbChannelDO> page, Integer userId);

    IPage<YtbChannelVideoVO> getFavoriteVideos(Page<YtbChannelVideoDO> page, Integer userId);
}

