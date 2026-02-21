/*
 * Copyright [2019~2025] [codingByFeng]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.fengorz.kiwi.api.ai;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.R;
import me.fengorz.kiwi.domain.ai.entity.YtbChannel;
import me.fengorz.kiwi.domain.ai.entity.YtbChannelVideo;
import me.fengorz.kiwi.domain.ai.scheduler.YtbChannelVideoSyncScheduler;
import me.fengorz.kiwi.domain.ai.service.YtbChannelService;
import me.fengorz.kiwi.domain.ai.service.YtbFavoriteService;
import me.fengorz.kiwi.domain.ai.vo.ytb.YtbChannelVO;
import me.fengorz.kiwi.domain.ai.vo.ytb.YtbChannelVideoVO;
import me.fengorz.kiwi.security.KiwiUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * YouTube Channel REST Controller
 *
 * @author codingByFeng
 */
@Slf4j
@RestController
@RequestMapping("/api/ai/ytb/channel")
@RequiredArgsConstructor
@Tag(name = "YouTube Channel", description = "YouTube channel management operations")
public class YtbChannelController {

    private final YtbChannelService ytbChannelService;
    private final YtbFavoriteService favoriteService;
    private final Optional<YtbChannelVideoSyncScheduler> syncScheduler;

    @GetMapping("/{channelId}")
    @Operation(summary = "Get channel by ID")
    public R<YtbChannel> getById(@PathVariable Long channelId) {
        return ytbChannelService.findById(channelId)
                .map(R::ok)
                .orElse(R.failed("Channel not found"));
    }

    @GetMapping("/link")
    @Operation(summary = "Get channel by channel link")
    public R<YtbChannel> getByChannelLink(@RequestParam String channelLink) {
        return ytbChannelService.findByChannelLink(channelLink)
                .map(R::ok)
                .orElse(R.failed("Channel not found"));
    }

    @GetMapping
    @Operation(summary = "Get all channels with pagination")
    public R<Page<YtbChannel>> getAll(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size) {
        Page<YtbChannel> page = new Page<>(current, size);
        return R.ok(ytbChannelService.findAllChannels(page));
    }

    @GetMapping("/page")
    @Operation(summary = "Get all channels with pagination (alternative endpoint)")
    public R<Page<YtbChannel>> getAllWithPage(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size) {
        Page<YtbChannel> page = new Page<>(current, size);
        return R.ok(ytbChannelService.findAllChannels(page));
    }

    @GetMapping("/enabled")
    @Operation(summary = "Get enabled channels")
    public R<List<YtbChannel>> getEnabledChannels() {
        return R.ok(ytbChannelService.findEnabledChannels());
    }

    @GetMapping("/{channelId}/videos")
    @Operation(summary = "Get videos for a channel")
    public R<Page<YtbChannelVideo>> getChannelVideos(
            @PathVariable Long channelId,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size) {
        Page<YtbChannelVideo> page = new Page<>(current, size);
        return R.ok(ytbChannelService.findVideosByChannelId(channelId, page));
    }

    @PostMapping
    @Operation(summary = "Create new channel by link or name")
    public R<YtbChannel> create(@RequestParam(required = false) String channelLinkOrName,
                                @RequestBody(required = false) YtbChannel channel) {
        if (StringUtils.hasText(channelLinkOrName)) {
            YtbChannel newChannel = new YtbChannel();
            newChannel.setChannelLink(channelLinkOrName);
            newChannel.setChannelName(extractChannelName(channelLinkOrName));
            newChannel.setStatus(YtbChannel.STATUS_READY);
            newChannel.setIfValid(true);
            newChannel.setCreateTime(java.time.LocalDateTime.now());
            return R.ok(ytbChannelService.saveChannel(newChannel));
        } else if (channel != null) {
            return R.ok(ytbChannelService.saveChannel(channel));
        }
        return R.failed("Either channelLinkOrName or request body must be provided");
    }

    private String extractChannelName(String channelLinkOrName) {
        if (channelLinkOrName == null) {
            return null;
        }
        // Extract @username from URL like https://youtube.com/@engineerprompt?si=xxx
        if (channelLinkOrName.contains("@")) {
            int atIndex = channelLinkOrName.indexOf("@");
            String afterAt = channelLinkOrName.substring(atIndex + 1);
            // Remove query parameters if present
            int queryIndex = afterAt.indexOf("?");
            if (queryIndex > 0) {
                afterAt = afterAt.substring(0, queryIndex);
            }
            return afterAt;
        }
        return channelLinkOrName;
    }

    @PutMapping("/{channelId}")
    @Operation(summary = "Update channel")
    public R<YtbChannel> update(@PathVariable Long channelId, @RequestBody YtbChannel channel) {
        return ytbChannelService.findById(channelId)
                .map(existing -> {
                    channel.setId(channelId);
                    return R.ok(ytbChannelService.saveChannel(channel));
                })
                .orElse(R.failed("Channel not found"));
    }

    @PostMapping("/{channelId}/enable")
    @Operation(summary = "Enable channel")
    public R<Void> enable(@PathVariable Long channelId) {
        ytbChannelService.enableChannel(channelId);
        return R.ok();
    }

    @PostMapping("/{channelId}/disable")
    @Operation(summary = "Disable channel")
    public R<Void> disable(@PathVariable Long channelId) {
        ytbChannelService.disableChannel(channelId);
        return R.ok();
    }

    @DeleteMapping("/{channelId}")
    @Operation(summary = "Delete channel")
    public R<Void> delete(@PathVariable Long channelId) {
        ytbChannelService.deleteChannelById(channelId);
        return R.ok();
    }

    // ==================== Favorites Endpoints ====================

    @PostMapping("/id/{channelId}/favorite")
    @Operation(summary = "Favorite a channel")
    public R<Boolean> favoriteChannel(
            @AuthenticationPrincipal KiwiUser user,
            @PathVariable("channelId") Long channelId) {
        if (channelId == null) {
            return R.failed("Channel ID cannot be empty");
        }
        favoriteService.favoriteChannel(user.getUserId().longValue(), channelId);
        return R.ok(true);
    }

    @DeleteMapping("/id/{channelId}/favorite")
    @Operation(summary = "Unfavorite a channel")
    public R<Boolean> unfavoriteChannel(
            @AuthenticationPrincipal KiwiUser user,
            @PathVariable("channelId") Long channelId) {
        if (channelId == null) {
            return R.failed("Channel ID cannot be empty");
        }
        favoriteService.unfavoriteChannel(user.getUserId().longValue(), channelId);
        return R.ok(true);
    }

    @GetMapping("/id/{channelId}/videos")
    @Operation(summary = "Get videos for a channel by ID with /id/ prefix")
    public R<Page<YtbChannelVideo>> getChannelVideosById(
            @PathVariable Long channelId,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size) {
        Page<YtbChannelVideo> page = new Page<>(current, size);
        return R.ok(ytbChannelService.findVideosByChannelId(channelId, page));
    }

    @GetMapping("/id/{channelId}/favorite")
    @Operation(summary = "Check if a channel is favorited")
    public R<Boolean> isChannelFavorited(
            @AuthenticationPrincipal KiwiUser user,
            @PathVariable("channelId") Long channelId) {
        if (channelId == null) {
            return R.failed("Channel ID cannot be empty");
        }
        boolean favorited = favoriteService.isChannelFavorited(user.getUserId().longValue(), channelId);
        return R.ok(favorited);
    }

    @PostMapping("/video/{videoId}/favorite")
    @Operation(summary = "Favorite a video")
    public R<Boolean> favoriteVideo(
            @AuthenticationPrincipal KiwiUser user,
            @PathVariable("videoId") Long videoId) {
        if (videoId == null) {
            return R.failed("Video ID cannot be empty");
        }
        favoriteService.favoriteVideo(user.getUserId().longValue(), videoId);
        return R.ok(true);
    }

    @DeleteMapping("/video/{videoId}/favorite")
    @Operation(summary = "Unfavorite a video")
    public R<Boolean> unfavoriteVideo(
            @AuthenticationPrincipal KiwiUser user,
            @PathVariable("videoId") Long videoId) {
        if (videoId == null) {
            return R.failed("Video ID cannot be empty");
        }
        favoriteService.unfavoriteVideo(user.getUserId().longValue(), videoId);
        return R.ok(true);
    }

    @GetMapping("/video/{videoId}/favorite")
    @Operation(summary = "Check if a video is favorited by videoId")
    public R<Boolean> isVideoFavorited(
            @AuthenticationPrincipal KiwiUser user,
            @PathVariable("videoId") Long videoId) {
        if (videoId == null) {
            return R.failed("Video ID cannot be empty");
        }
        boolean favorited = favoriteService.isVideoFavorited(user.getUserId().longValue(), videoId);
        return R.ok(favorited);
    }

    @PostMapping("/video/favorite")
    @Operation(summary = "Favorite a video by URL")
    public R<Boolean> favoriteVideoByUrl(
            @AuthenticationPrincipal KiwiUser user,
            @RequestParam("videoUrl") String videoUrl) {
        if (!StringUtils.hasText(videoUrl)) {
            return R.failed("Video URL cannot be empty");
        }
        boolean ok = favoriteService.favoriteVideoByUrl(user.getUserId().longValue(), videoUrl);
        if (!ok) {
            return R.failed("Failed to create video record");
        }
        return R.ok(true);
    }

    @DeleteMapping("/video/favorite")
    @Operation(summary = "Unfavorite a video by URL")
    public R<Boolean> unfavoriteVideoByUrl(
            @AuthenticationPrincipal KiwiUser user,
            @RequestParam("videoUrl") String videoUrl) {
        if (!StringUtils.hasText(videoUrl)) {
            return R.failed("Video URL cannot be empty");
        }
        boolean ok = favoriteService.unfavoriteVideoByUrl(user.getUserId().longValue(), videoUrl);
        if (!ok) {
            return R.failed("Video not found");
        }
        return R.ok(true);
    }

    @GetMapping("/video/favorite")
    @Operation(summary = "Check if a video is favorited by videoUrl")
    public R<Boolean> isVideoFavoritedByUrl(
            @AuthenticationPrincipal KiwiUser user,
            @RequestParam("videoUrl") String videoUrl) {
        if (!StringUtils.hasText(videoUrl)) {
            return R.failed("Video URL cannot be empty");
        }
        boolean favorited = favoriteService.isVideoFavoritedByUrl(user.getUserId().longValue(), videoUrl);
        return R.ok(favorited);
    }

    @GetMapping("/favorites/channels")
    @Operation(summary = "List favorite channels")
    public R<IPage<YtbChannelVO>> getFavoriteChannels(
            @AuthenticationPrincipal KiwiUser user,
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam(value = "size", defaultValue = "10") Integer size) {
        Page<YtbChannel> page = new Page<>(current, size);
        return R.ok(favoriteService.getFavoriteChannels(page, user.getUserId().longValue()));
    }

    @GetMapping("/favorites/videos")
    @Operation(summary = "List favorite videos")
    public R<IPage<YtbChannelVideoVO>> getFavoriteVideos(
            @AuthenticationPrincipal KiwiUser user,
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam(value = "size", defaultValue = "10") Integer size) {
        Page<YtbChannelVideo> page = new Page<>(current, size);
        return R.ok(favoriteService.getFavoriteVideos(page, user.getUserId().longValue()));
    }

    // ==================== Sync Trigger Endpoint (temporary public) ====================

    @PostMapping("/sync/trigger")
    @Operation(summary = "Manually trigger YouTube channel video sync")
    public R<String> triggerSync() {
        if (syncScheduler.isEmpty()) {
            return R.failed("Sync scheduler is not enabled");
        }
        YtbChannelVideoSyncScheduler scheduler = syncScheduler.get();
        if (scheduler.isRunning()) {
            return R.failed("Sync job is already running");
        }
        log.info("[YTB-SYNC] Manual sync triggered via API");
        new Thread(scheduler::syncChannelVideos, "ytb-sync-manual").start();
        return R.ok("Sync job triggered successfully");
    }
}
