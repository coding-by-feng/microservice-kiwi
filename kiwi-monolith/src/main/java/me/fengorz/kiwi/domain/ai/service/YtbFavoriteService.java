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
package me.fengorz.kiwi.domain.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import me.fengorz.kiwi.domain.ai.entity.YtbChannel;
import me.fengorz.kiwi.domain.ai.entity.YtbChannelFavorite;
import me.fengorz.kiwi.domain.ai.entity.YtbChannelVideo;
import me.fengorz.kiwi.domain.ai.entity.YtbVideoFavorite;
import me.fengorz.kiwi.domain.ai.mapper.YtbChannelFavoriteMapper;
import me.fengorz.kiwi.domain.ai.mapper.YtbChannelMapper;
import me.fengorz.kiwi.domain.ai.mapper.YtbChannelVideoMapper;
import me.fengorz.kiwi.domain.ai.mapper.YtbVideoFavoriteMapper;
import me.fengorz.kiwi.domain.ai.vo.ytb.YtbChannelVO;
import me.fengorz.kiwi.domain.ai.vo.ytb.YtbChannelVideoVO;
import me.fengorz.kiwi.domain.ai.ytb.YouTubeClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * YouTube Favorite Service - manages user favorites for channels and videos
 *
 * @author codingByFeng
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class YtbFavoriteService {

    private final YtbChannelFavoriteMapper channelFavoriteMapper;
    private final YtbVideoFavoriteMapper videoFavoriteMapper;
    private final YtbChannelMapper channelMapper;
    private final YtbChannelVideoMapper videoMapper;
    private final YouTubeClient youTubeClient;

    @Transactional
    public boolean favoriteChannel(Long userId, Long channelId) {
        YtbChannelFavorite existing = channelFavoriteMapper.selectOne(
                new LambdaQueryWrapper<YtbChannelFavorite>()
                        .eq(YtbChannelFavorite::getUserId, userId)
                        .eq(YtbChannelFavorite::getChannelId, channelId));

        if (existing != null) {
            if (Boolean.FALSE.equals(existing.getIfValid())) {
                existing.setIfValid(true);
                channelFavoriteMapper.updateById(existing);
            }
            return true;
        }

        YtbChannelFavorite favorite = new YtbChannelFavorite();
        favorite.setUserId(userId);
        favorite.setChannelId(channelId);
        favorite.setCreateTime(LocalDateTime.now());
        favorite.setIfValid(true);
        channelFavoriteMapper.insert(favorite);
        return true;
    }

    @Transactional
    public boolean unfavoriteChannel(Long userId, Long channelId) {
        YtbChannelFavorite existing = channelFavoriteMapper.selectOne(
                new LambdaQueryWrapper<YtbChannelFavorite>()
                        .eq(YtbChannelFavorite::getUserId, userId)
                        .eq(YtbChannelFavorite::getChannelId, channelId));

        if (existing != null) {
            existing.setIfValid(false);
            channelFavoriteMapper.updateById(existing);
        }
        return true;
    }

    @Transactional
    public boolean favoriteVideo(Long userId, Long videoId) {
        YtbVideoFavorite existing = videoFavoriteMapper.selectOne(
                new LambdaQueryWrapper<YtbVideoFavorite>()
                        .eq(YtbVideoFavorite::getUserId, userId)
                        .eq(YtbVideoFavorite::getVideoId, videoId));

        if (existing != null) {
            if (Boolean.FALSE.equals(existing.getIfValid())) {
                existing.setIfValid(true);
                videoFavoriteMapper.updateById(existing);
            }
            return true;
        }

        YtbVideoFavorite favorite = new YtbVideoFavorite();
        favorite.setUserId(userId);
        favorite.setVideoId(videoId);
        favorite.setCreateTime(LocalDateTime.now());
        favorite.setIfValid(true);
        videoFavoriteMapper.insert(favorite);
        return true;
    }

    @Transactional
    public boolean unfavoriteVideo(Long userId, Long videoId) {
        YtbVideoFavorite existing = videoFavoriteMapper.selectOne(
                new LambdaQueryWrapper<YtbVideoFavorite>()
                        .eq(YtbVideoFavorite::getUserId, userId)
                        .eq(YtbVideoFavorite::getVideoId, videoId));

        if (existing != null) {
            existing.setIfValid(false);
            videoFavoriteMapper.updateById(existing);
        }
        return true;
    }

    @Transactional
    @CacheEvict(value = "ytb:video:favorite", key = "#userId + ':' + #videoUrl")
    public boolean favoriteVideoByUrl(Long userId, String videoUrl) {
        YtbChannelVideo video = videoMapper.selectOne(
                new LambdaQueryWrapper<YtbChannelVideo>()
                        .eq(YtbChannelVideo::getVideoLink, videoUrl));

        if (video == null) {
            video = new YtbChannelVideo();
            video.setVideoLink(videoUrl);

            // Fetch actual video title from YouTube
            String videoTitle = fetchVideoTitle(videoUrl);
            video.setVideoTitle(videoTitle);
            log.info("[FAVORITE] Created new video record - URL: {}, Title: {}", videoUrl, videoTitle);

            video.setStatus(YtbChannelVideo.STATUS_READY);
            video.setCreateTime(LocalDateTime.now());
            video.setIfValid(true);
            videoMapper.insert(video);
        }

        return favoriteVideo(userId, video.getId());
    }

    private String fetchVideoTitle(String videoUrl) {
        try {
            String title = youTubeClient.getVideoTitle(videoUrl);
            if (title != null && !title.isBlank()) {
                return title;
            }
        } catch (Exception e) {
            log.warn("[FAVORITE] Failed to fetch video title for URL: {}, error: {}", videoUrl, e.getMessage());
        }
        return "Unknown";
    }

    @Transactional
    @CacheEvict(value = "ytb:video:favorite", key = "#userId + ':' + #videoUrl")
    public boolean unfavoriteVideoByUrl(Long userId, String videoUrl) {
        YtbChannelVideo video = videoMapper.selectOne(
                new LambdaQueryWrapper<YtbChannelVideo>()
                        .eq(YtbChannelVideo::getVideoLink, videoUrl));

        if (video == null) {
            return false;
        }

        return unfavoriteVideo(userId, video.getId());
    }

    public IPage<YtbChannelVO> getFavoriteChannels(Page<YtbChannel> page, Long userId) {
        List<Long> favoriteChannelIds = channelFavoriteMapper.selectList(
                        new LambdaQueryWrapper<YtbChannelFavorite>()
                                .eq(YtbChannelFavorite::getUserId, userId)
                                .eq(YtbChannelFavorite::getIfValid, true))
                .stream()
                .map(YtbChannelFavorite::getChannelId)
                .collect(Collectors.toList());

        if (favoriteChannelIds.isEmpty()) {
            Page<YtbChannelVO> emptyPage = new Page<>(page.getCurrent(), page.getSize(), 0);
            emptyPage.setRecords(List.of());
            return emptyPage;
        }

        Page<YtbChannel> channelPage = channelMapper.selectPage(page,
                new LambdaQueryWrapper<YtbChannel>()
                        .in(YtbChannel::getId, favoriteChannelIds)
                        .eq(YtbChannel::getIfValid, true));

        Page<YtbChannelVO> resultPage = new Page<>(channelPage.getCurrent(), channelPage.getSize(), channelPage.getTotal());
        resultPage.setRecords(channelPage.getRecords().stream()
                .map(channel -> YtbChannelVO.builder()
                        .channelId(channel.getId())
                        .channelName(channel.getChannelName())
                        .status(channel.getStatus())
                        .favorited(true)
                        .favoriteCount(countChannelFavorites(channel.getId()))
                        .build())
                .collect(Collectors.toList()));

        return resultPage;
    }

    public IPage<YtbChannelVideoVO> getFavoriteVideos(Page<YtbChannelVideo> page, Long userId) {
        List<Long> favoriteVideoIds = videoFavoriteMapper.selectList(
                        new LambdaQueryWrapper<YtbVideoFavorite>()
                                .eq(YtbVideoFavorite::getUserId, userId)
                                .eq(YtbVideoFavorite::getIfValid, true))
                .stream()
                .map(YtbVideoFavorite::getVideoId)
                .collect(Collectors.toList());

        if (favoriteVideoIds.isEmpty()) {
            Page<YtbChannelVideoVO> emptyPage = new Page<>(page.getCurrent(), page.getSize(), 0);
            emptyPage.setRecords(List.of());
            return emptyPage;
        }

        Page<YtbChannelVideo> videoPage = videoMapper.selectPage(page,
                new LambdaQueryWrapper<YtbChannelVideo>()
                        .in(YtbChannelVideo::getId, favoriteVideoIds)
                        .eq(YtbChannelVideo::getIfValid, true));

        Page<YtbChannelVideoVO> resultPage = new Page<>(videoPage.getCurrent(), videoPage.getSize(), videoPage.getTotal());
        resultPage.setRecords(videoPage.getRecords().stream()
                .map(video -> YtbChannelVideoVO.builder()
                        .id(video.getId())
                        .videoTitle(video.getVideoTitle())
                        .videoLink(video.getVideoLink())
                        .publishedAt(video.getPublishedAt())
                        .status(video.getStatus())
                        .favorited(true)
                        .favoriteCount(countVideoFavorites(video.getId()))
                        .build())
                .collect(Collectors.toList()));

        return resultPage;
    }

    public boolean isVideoFavorited(Long userId, Long videoId) {
        return videoFavoriteMapper.selectCount(
                new LambdaQueryWrapper<YtbVideoFavorite>()
                        .eq(YtbVideoFavorite::getUserId, userId)
                        .eq(YtbVideoFavorite::getVideoId, videoId)
                        .eq(YtbVideoFavorite::getIfValid, true)) > 0;
    }

    @Cacheable(value = "ytb:video:favorite", key = "#userId + ':' + #videoUrl")
    public boolean isVideoFavoritedByUrl(Long userId, String videoUrl) {
        // Optimized: Single JOIN query instead of 2 separate queries (50-100ms faster)
        return videoFavoriteMapper.countByVideoUrlAndUserId(videoUrl, userId) > 0;
    }

    public boolean isChannelFavorited(Long userId, Long channelId) {
        return channelFavoriteMapper.selectCount(
                new LambdaQueryWrapper<YtbChannelFavorite>()
                        .eq(YtbChannelFavorite::getUserId, userId)
                        .eq(YtbChannelFavorite::getChannelId, channelId)
                        .eq(YtbChannelFavorite::getIfValid, true)) > 0;
    }

    private Long countChannelFavorites(Long channelId) {
        return channelFavoriteMapper.selectCount(
                new LambdaQueryWrapper<YtbChannelFavorite>()
                        .eq(YtbChannelFavorite::getChannelId, channelId)
                        .eq(YtbChannelFavorite::getIfValid, true));
    }

    private Long countVideoFavorites(Long videoId) {
        return videoFavoriteMapper.selectCount(
                new LambdaQueryWrapper<YtbVideoFavorite>()
                        .eq(YtbVideoFavorite::getVideoId, videoId)
                        .eq(YtbVideoFavorite::getIfValid, true));
    }
}
