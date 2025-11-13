package me.fengorz.kiwi.ai.service.ytb;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.ai.api.entity.YtbChannelDO;
import me.fengorz.kiwi.ai.api.entity.YtbChannelFavoriteDO;
import me.fengorz.kiwi.ai.api.entity.YtbChannelVideoDO;
import me.fengorz.kiwi.ai.api.entity.YtbVideoFavoriteDO;
import me.fengorz.kiwi.ai.api.vo.ytb.YtbChannelVO;
import me.fengorz.kiwi.ai.api.vo.ytb.YtbChannelVideoVO;
import me.fengorz.kiwi.ai.service.ytb.mapper.YtbChannelFavoriteMapper;
import me.fengorz.kiwi.ai.service.ytb.mapper.YtbChannelMapper;
import me.fengorz.kiwi.ai.service.ytb.mapper.YtbChannelVideoMapper;
import me.fengorz.kiwi.ai.service.ytb.mapper.YtbVideoFavoriteMapper;
import me.fengorz.kiwi.common.db.service.SeqService;
import me.fengorz.kiwi.common.ytb.YouTuBeHelper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class YtbFavoriteServiceImpl implements YtbFavoriteService {

    private final YtbChannelFavoriteMapper channelFavoriteMapper;
    private final YtbVideoFavoriteMapper videoFavoriteMapper;
    private final YtbChannelMapper channelMapper;
    private final YtbChannelVideoMapper videoMapper;
    private final SeqService seqService;
    // Inject YouTube helper to fetch real video titles
    private final YouTuBeHelper youTuBeHelper;

    @Override
    public boolean favoriteChannel(Integer userId, Long channelId) {
        YtbChannelFavoriteDO existing = channelFavoriteMapper.selectOne(new LambdaQueryWrapper<YtbChannelFavoriteDO>()
                .eq(YtbChannelFavoriteDO::getUserId, userId)
                .eq(YtbChannelFavoriteDO::getChannelId, channelId));
        if (existing == null) {
            YtbChannelFavoriteDO rec = new YtbChannelFavoriteDO()
                    .setId(Long.valueOf(seqService.genCommonIntSequence()))
                    .setUserId(Long.valueOf(userId))
                    .setChannelId(channelId)
                    .setCreateTime(LocalDateTime.now())
                    .setIfValid(true);
            channelFavoriteMapper.insert(rec);
        } else if (Boolean.FALSE.equals(existing.getIfValid())) {
            existing.setIfValid(true);
            channelFavoriteMapper.updateById(existing);
        }
        return true;
    }

    @Override
    public boolean unfavoriteChannel(Integer userId, Long channelId) {
        YtbChannelFavoriteDO existing = channelFavoriteMapper.selectOne(new LambdaQueryWrapper<YtbChannelFavoriteDO>()
                .eq(YtbChannelFavoriteDO::getUserId, userId)
                .eq(YtbChannelFavoriteDO::getChannelId, channelId)
                .eq(YtbChannelFavoriteDO::getIfValid, true));
        if (existing != null) {
            existing.setIfValid(false);
            channelFavoriteMapper.updateById(existing);
        }
        return true;
    }

    @Override
    public boolean favoriteVideo(Integer userId, Long videoId) {
        YtbVideoFavoriteDO existing = videoFavoriteMapper.selectOne(new LambdaQueryWrapper<YtbVideoFavoriteDO>()
                .eq(YtbVideoFavoriteDO::getUserId, userId)
                .eq(YtbVideoFavoriteDO::getVideoId, videoId));
        if (existing == null) {
            YtbVideoFavoriteDO rec = new YtbVideoFavoriteDO()
                    .setId(Long.valueOf(seqService.genCommonIntSequence()))
                    .setUserId(Long.valueOf(userId))
                    .setVideoId(videoId)
                    .setCreateTime(LocalDateTime.now())
                    .setIfValid(true);
            videoFavoriteMapper.insert(rec);
        } else if (Boolean.FALSE.equals(existing.getIfValid())) {
            existing.setIfValid(true);
            videoFavoriteMapper.updateById(existing);
        }
        return true;
    }

    @Override
    public boolean unfavoriteVideo(Integer userId, Long videoId) {
        YtbVideoFavoriteDO existing = videoFavoriteMapper.selectOne(new LambdaQueryWrapper<YtbVideoFavoriteDO>()
                .eq(YtbVideoFavoriteDO::getUserId, userId)
                .eq(YtbVideoFavoriteDO::getVideoId, videoId)
                .eq(YtbVideoFavoriteDO::getIfValid, true));
        if (existing != null) {
            existing.setIfValid(false);
            videoFavoriteMapper.updateById(existing);
        }
        return true;
    }

    @Override
    public boolean favoriteVideoByUrl(Integer userId, String videoUrl) {
        if (videoUrl == null || videoUrl.trim().isEmpty()) {
            return false;
        }
        YtbChannelVideoDO video = videoMapper.selectOne(new LambdaQueryWrapper<YtbChannelVideoDO>()
                .eq(YtbChannelVideoDO::getVideoLink, videoUrl));
        if (video == null) {
            // Resolve real title using yt-dlp; fallback to URL-derived title if it fails
            String realTitle = resolveVideoTitleSafely(videoUrl);
            YtbChannelVideoDO toSave = new YtbChannelVideoDO()
                    .setId(Long.valueOf(seqService.genCommonIntSequence()))
                    .setChannelId(0L)
                    .setVideoTitle(realTitle)
                    .setVideoLink(videoUrl)
                    .setStatus(0)
                    .setCreateTime(LocalDateTime.now())
                    .setIfValid(true);
            try {
                videoMapper.insert(toSave);
            } catch (Exception ignore) {
                // Duplicate/race: ignore and re-query
            }
            video = videoMapper.selectOne(new LambdaQueryWrapper<YtbChannelVideoDO>()
                    .eq(YtbChannelVideoDO::getVideoLink, videoUrl));
            if (video == null) {
                return false;
            }
        } else {
            // If record exists but title looks like a URL, try to backfill the real title
            if (video.getVideoTitle() == null || video.getVideoTitle().startsWith("http")) {
                try {
                    String realTitle = youTuBeHelper.getVideoTitle(videoUrl);
                    if (realTitle != null && !realTitle.trim().isEmpty()) {
                        video.setVideoTitle(realTitle.trim());
                        videoMapper.updateById(video);
                    }
                } catch (Exception e) {
                    log.warn("Failed to backfill real title for video {}: {}", videoUrl, e.getMessage());
                }
            }
        }
        return this.favoriteVideo(userId, video.getId());
    }

    @Override
    public boolean unfavoriteVideoByUrl(Integer userId, String videoUrl) {
        if (videoUrl == null || videoUrl.trim().isEmpty()) {
            return false;
        }
        YtbChannelVideoDO video = videoMapper.selectOne(new LambdaQueryWrapper<YtbChannelVideoDO>()
                .eq(YtbChannelVideoDO::getVideoLink, videoUrl));
        if (video == null) {
            return false;
        }
        return this.unfavoriteVideo(userId, video.getId());
    }

    @Override
    public IPage<YtbChannelVO> getFavoriteChannels(Page<YtbChannelDO> page, Integer userId) {
        List<YtbChannelFavoriteDO> favs = channelFavoriteMapper.selectList(new LambdaQueryWrapper<YtbChannelFavoriteDO>()
                .eq(YtbChannelFavoriteDO::getUserId, userId)
                .eq(YtbChannelFavoriteDO::getIfValid, true));
        if (org.apache.commons.collections4.CollectionUtils.isEmpty(favs)) {
            return new Page<>(page.getCurrent(), page.getSize(), 0);
        }
        List<Long> channelIds = favs.stream().map(YtbChannelFavoriteDO::getChannelId).collect(Collectors.toList());
        IPage<YtbChannelDO> channels = new Page<>(page.getCurrent(), page.getSize());
        channels = channelMapper.selectPage(channels, new LambdaQueryWrapper<YtbChannelDO>()
                .in(YtbChannelDO::getId, channelIds)
                .eq(YtbChannelDO::getIfValid, true)
                .orderByDesc(YtbChannelDO::getCreateTime));

        // Batch favorite counts and user favorited set for the paged records
        List<Long> pageChannelIds = channels.getRecords().stream().map(YtbChannelDO::getId).collect(Collectors.toList());
        Map<Long, Long> favCountMapLocal = new HashMap<>();
        Set<Long> userFavSet = favs.stream().map(YtbChannelFavoriteDO::getChannelId).collect(Collectors.toSet());
        if (!pageChannelIds.isEmpty()) {
            List<YtbChannelFavoriteDO> allFavsForPage = channelFavoriteMapper.selectList(new LambdaQueryWrapper<YtbChannelFavoriteDO>()
                    .in(YtbChannelFavoriteDO::getChannelId, pageChannelIds)
                    .eq(YtbChannelFavoriteDO::getIfValid, true));
            favCountMapLocal = allFavsForPage.stream().collect(Collectors.groupingBy(YtbChannelFavoriteDO::getChannelId, Collectors.counting()));
        }
        final Map<Long, Long> favCountMap = favCountMapLocal;

        Page<YtbChannelVO> voPage = new Page<>(channels.getCurrent(), channels.getSize(), channels.getTotal());
        voPage.setRecords(channels.getRecords().stream().map(ch -> YtbChannelVO.builder()
                .channelId(ch.getId())
                .channelName(ch.getChannelName())
                .status(ch.getStatus())
                .favorited(userFavSet.contains(ch.getId()))
                .favoriteCount(favCountMap.getOrDefault(ch.getId(), 0L))
                .build()).collect(Collectors.toList()));
        return voPage;
    }

    @Override
    public IPage<YtbChannelVideoVO> getFavoriteVideos(Page<YtbChannelVideoDO> page, Integer userId) {
        List<YtbVideoFavoriteDO> favs = videoFavoriteMapper.selectList(new LambdaQueryWrapper<YtbVideoFavoriteDO>()
                .eq(YtbVideoFavoriteDO::getUserId, userId)
                .eq(YtbVideoFavoriteDO::getIfValid, true));
        if (org.apache.commons.collections4.CollectionUtils.isEmpty(favs)) {
            return new Page<>(page.getCurrent(), page.getSize(), 0);
        }
        List<Long> videoIds = favs.stream().map(YtbVideoFavoriteDO::getVideoId).collect(Collectors.toList());
        IPage<YtbChannelVideoDO> videos = new Page<>(page.getCurrent(), page.getSize());
        videos = videoMapper.selectPage(videos, new LambdaQueryWrapper<YtbChannelVideoDO>()
                .in(YtbChannelVideoDO::getId, videoIds)
                .eq(YtbChannelVideoDO::getIfValid, true)
                .orderByDesc(YtbChannelVideoDO::getPublishedAt)
                .orderByDesc(YtbChannelVideoDO::getCreateTime));

        // Batch favorite counts and user favorited set for the paged records
        List<Long> pageVideoIds = videos.getRecords().stream().map(YtbChannelVideoDO::getId).collect(Collectors.toList());
        Map<Long, Long> favCountMapLocal = new HashMap<>();
        Set<Long> userFavSet = favs.stream().map(YtbVideoFavoriteDO::getVideoId).collect(Collectors.toSet());
        if (!pageVideoIds.isEmpty()) {
            List<YtbVideoFavoriteDO> allFavsForPage = videoFavoriteMapper.selectList(new LambdaQueryWrapper<YtbVideoFavoriteDO>()
                    .in(YtbVideoFavoriteDO::getVideoId, pageVideoIds)
                    .eq(YtbVideoFavoriteDO::getIfValid, true));
            favCountMapLocal = allFavsForPage.stream().collect(Collectors.groupingBy(YtbVideoFavoriteDO::getVideoId, Collectors.counting()));
        }
        final Map<Long, Long> favCountMap = favCountMapLocal;

        Page<YtbChannelVideoVO> voPage = new Page<>(videos.getCurrent(), videos.getSize(), videos.getTotal());
        voPage.setRecords(videos.getRecords().stream().map(v -> {
            YtbChannelVideoVO vo = new YtbChannelVideoVO();
            org.springframework.beans.BeanUtils.copyProperties(v, vo);
            vo.setFavorited(userFavSet.contains(v.getId()));
            vo.setFavoriteCount(favCountMap.getOrDefault(v.getId(), 0L));
            return vo;
        }).collect(java.util.stream.Collectors.toList()));
        return voPage;
    }

    @Override
    public boolean isVideoFavorited(Integer userId, Long videoId) {
        if (userId == null || videoId == null) {
            return false;
        }
        YtbVideoFavoriteDO existing = videoFavoriteMapper.selectOne(new LambdaQueryWrapper<YtbVideoFavoriteDO>()
                .eq(YtbVideoFavoriteDO::getUserId, userId)
                .eq(YtbVideoFavoriteDO::getVideoId, videoId)
                .eq(YtbVideoFavoriteDO::getIfValid, true));
        return existing != null;
    }

    @Override
    public boolean isVideoFavoritedByUrl(Integer userId, String videoUrl) {
        if (userId == null || videoUrl == null || videoUrl.trim().isEmpty()) {
            return false;
        }
        YtbChannelVideoDO video = videoMapper.selectOne(new LambdaQueryWrapper<YtbChannelVideoDO>()
                .eq(YtbChannelVideoDO::getVideoLink, videoUrl));
        if (video == null) {
            return false;
        }
        return isVideoFavorited(userId, video.getId());
    }

    private String deriveTitleFromUrl(String videoUrl) {
        try {
            java.net.URI uri = java.net.URI.create(videoUrl);
            String query = uri.getQuery();
            if (query != null) {
                for (String pair : query.split("&")) {
                    String[] kv = pair.split("=", 2);
                    if (kv.length == 2 && "v".equalsIgnoreCase(kv[0]) && kv[1] != null && !kv[1].isEmpty()) {
                        return "YouTube Video " + kv[1];
                    }
                }
            }
            String path = uri.getPath();
            if (path != null && path.contains("/shorts/")) {
                String id = path.substring(path.indexOf("/shorts/") + 8);
                if (!id.isEmpty()) return "YouTube Short " + id;
            }
        } catch (Exception ignore) {
        }
        return videoUrl;
    }

    private String resolveVideoTitleSafely(String videoUrl) {
        try {
            String title = youTuBeHelper.getVideoTitle(videoUrl);
            if (title != null && !title.trim().isEmpty()) {
                return title.trim();
            }
        } catch (Exception e) {
            log.warn("Failed to fetch video title via yt-dlp for {}: {}", videoUrl, e.getMessage());
        }
        return deriveTitleFromUrl(videoUrl);
    }
}
