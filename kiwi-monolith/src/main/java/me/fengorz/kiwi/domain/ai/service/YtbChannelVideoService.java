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
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.domain.ai.entity.YtbChannelVideo;
import me.fengorz.kiwi.domain.ai.mapper.YtbChannelVideoMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * YtbChannelVideo Service - manages YouTube channel video operations
 *
 * @author codingByFeng
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class YtbChannelVideoService extends ServiceImpl<YtbChannelVideoMapper, YtbChannelVideo> {

    /**
     * Find video by ID
     */
    public Optional<YtbChannelVideo> findById(Long videoId) {
        return Optional.ofNullable(getById(videoId));
    }

    /**
     * Find video by link
     */
    public Optional<YtbChannelVideo> findByVideoLink(String videoLink) {
        return Optional.ofNullable(getOne(new LambdaQueryWrapper<YtbChannelVideo>()
                .eq(YtbChannelVideo::getVideoLink, videoLink)));
    }

    /**
     * Find video by ID with subtitles
     */
    public Optional<YtbChannelVideo> findByIdWithSubtitles(Long videoId) {
        return Optional.ofNullable(getById(videoId));
    }

    /**
     * Find videos by channel ID
     */
    public List<YtbChannelVideo> findByChannelId(Long channelId) {
        return list(new LambdaQueryWrapper<YtbChannelVideo>()
                .eq(YtbChannelVideo::getChannelId, channelId)
                .eq(YtbChannelVideo::getIfValid, true));
    }

    /**
     * Find videos by channel ID with pagination
     */
    public Page<YtbChannelVideo> findByChannelId(Long channelId, Page<YtbChannelVideo> page) {
        return page(page, new LambdaQueryWrapper<YtbChannelVideo>()
                .eq(YtbChannelVideo::getChannelId, channelId)
                .eq(YtbChannelVideo::getIfValid, true));
    }

    /**
     * Find videos by status
     */
    public List<YtbChannelVideo> findByStatus(Integer status) {
        return list(new LambdaQueryWrapper<YtbChannelVideo>()
                .eq(YtbChannelVideo::getStatus, status)
                .eq(YtbChannelVideo::getIfValid, true));
    }

    /**
     * Find ready videos (status = 0)
     */
    public List<YtbChannelVideo> findReadyVideos() {
        return list(new LambdaQueryWrapper<YtbChannelVideo>()
                .eq(YtbChannelVideo::getStatus, YtbChannelVideo.STATUS_READY)
                .eq(YtbChannelVideo::getIfValid, true));
    }

    /**
     * Find videos by channel and status
     */
    public List<YtbChannelVideo> findByChannelIdAndStatus(Long channelId, Integer status) {
        return list(new LambdaQueryWrapper<YtbChannelVideo>()
                .eq(YtbChannelVideo::getChannelId, channelId)
                .eq(YtbChannelVideo::getStatus, status)
                .eq(YtbChannelVideo::getIfValid, true));
    }

    /**
     * Count videos by channel ID
     */
    public long countByChannelId(Long channelId) {
        return count(new LambdaQueryWrapper<YtbChannelVideo>()
                .eq(YtbChannelVideo::getChannelId, channelId)
                .eq(YtbChannelVideo::getIfValid, true));
    }

    /**
     * Check if video exists by link
     */
    public boolean existsByVideoLink(String videoLink) {
        return findByVideoLink(videoLink).isPresent();
    }

    /**
     * Save video
     */
    @Transactional
    public YtbChannelVideo saveVideo(YtbChannelVideo video) {
        saveOrUpdate(video);
        return video;
    }

    /**
     * Update video status
     */
    @Transactional
    public void updateStatus(Long videoId, Integer status) {
        findById(videoId).ifPresent(video -> {
            video.setStatus(status);
            updateById(video);
        });
    }

    /**
     * Mark video as processing
     */
    @Transactional
    public void markProcessing(Long videoId) {
        updateStatus(videoId, YtbChannelVideo.STATUS_PROCESSING);
    }

    /**
     * Mark video as finished
     */
    @Transactional
    public void markFinished(Long videoId) {
        updateStatus(videoId, YtbChannelVideo.STATUS_FINISH);
    }

    /**
     * Mark video as invalid
     */
    @Transactional
    public void markInvalid(Long videoId) {
        findById(videoId).ifPresent(video -> {
            video.setIfValid(false);
            updateById(video);
        });
    }

    /**
     * Delete video
     */
    @Transactional
    public void deleteVideoById(Long videoId) {
        removeById(videoId);
    }
}
