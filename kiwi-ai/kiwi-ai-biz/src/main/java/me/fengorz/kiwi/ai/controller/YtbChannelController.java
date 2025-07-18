package me.fengorz.kiwi.ai.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.ai.api.entity.YtbChannelDO;
import me.fengorz.kiwi.ai.api.entity.YtbChannelVideoDO;
import me.fengorz.kiwi.ai.api.vo.ytb.YtbChannelVO;
import me.fengorz.kiwi.ai.api.vo.ytb.YtbChannelVideoVO;
import me.fengorz.kiwi.ai.service.ytb.YtbChannelService;
import me.fengorz.kiwi.ai.service.ytb.YtbChannelVideoService;
import me.fengorz.kiwi.common.api.R;
import me.fengorz.kiwi.common.sdk.annotation.log.LogMarker;
import me.fengorz.kiwi.common.sdk.controller.BaseController;
import me.fengorz.kiwi.common.sdk.web.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Validated
@RestController
@RequestMapping("/ai/ytb/channel")
public class YtbChannelController extends BaseController {

    private final YtbChannelService channelService;
    private final YtbChannelVideoService videoService;

    public YtbChannelController(@Qualifier("ytbChannelServiceV2") YtbChannelService channelService,
                                YtbChannelVideoService videoService) {
        this.channelService = channelService;
        this.videoService = videoService;
    }

    /**
     * Submit a YouTube channel
     */
    @LogMarker
    @PostMapping
    public R<Long> submitChannel(@RequestParam("channelLinkOrName") String channelLinkOrName) {
        if (!StringUtils.hasText(channelLinkOrName)) {
            return R.failed("Channel link or name cannot be empty");
        }

        return R.success(channelService.submitChannel(channelLinkOrName, getCurrentUserId()));
    }

    private static Integer getCurrentUserId() {
        return SecurityUtils.getCurrentUserId();
    }

    /**
     * Retrieves paginated list of user's YouTube channels
     *
     * @param current Page number (1-based indexing)
     * @param size    Number of items per page
     * @return Paginated list of user's YouTube channels with mapped data
     */
    @LogMarker
    @GetMapping("/page")
    public R<IPage<YtbChannelVO>> getUserChannelPage(
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam(value = "size", defaultValue = "10") Integer size) {

        // Validate pagination parameters
        if (current < 1) {
            return R.failed("Page number must be greater than 0");
        }

        if (size < 1 || size > 100) {
            return R.failed("Page size must be between 1 and 100");
        }

        try {
            // Create pagination object
            Page<YtbChannelDO> page = new Page<>(current, size);

            // Get current user and retrieve their channels
            Integer userId = getCurrentUserId();
            IPage<YtbChannelVO> resultPage = channelService.getUserChannelPageVO(page, userId);

            return R.success(resultPage);
        } catch (Exception e) {
            log.error("Error retrieving user channel page: {}", e.getMessage(), e);
            return R.failed("Failed to retrieve channels");
        }
    }

    /**
     * Get videos by channel ID
     */
    @LogMarker
    @GetMapping("/{channelId}/videos")
    public R<IPage<YtbChannelVideoVO>> getVideosByChannelId(
            @PathVariable("channelId") Long channelId,
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam(value = "size", defaultValue = "10") Integer size) {

        if (channelId == null) {
            return R.failed("Channel ID cannot be empty");
        }

        Page<YtbChannelVideoDO> page = new Page<>(current, size);
        IPage<YtbChannelVideoVO> resultPage = videoService.getVideosByChannelId(page, channelId);

        return R.success(resultPage);
    }

}