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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.domain.ai.entity.YtbChannel;
import me.fengorz.kiwi.domain.ai.entity.YtbChannelVideo;
import me.fengorz.kiwi.domain.ai.mapper.YtbChannelMapper;
import me.fengorz.kiwi.domain.ai.mapper.YtbChannelVideoMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * YtbChannel Service
 *
 * @author codingByFeng
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class YtbChannelService extends ServiceImpl<YtbChannelMapper, YtbChannel> {

    private final YtbChannelVideoMapper videoMapper;

    /**
     * Find channel by ID
     */
    public Optional<YtbChannel> findById(Long channelId) {
        return Optional.ofNullable(getById(channelId));
    }

    /**
     * Find channel by link
     */
    public Optional<YtbChannel> findByChannelLink(String channelLink) {
        return Optional.ofNullable(getOne(new LambdaQueryWrapper<YtbChannel>()
                .eq(YtbChannel::getChannelLink, channelLink)));
    }

    /**
     * Find channel with videos
     */
    public Optional<YtbChannel> findByIdWithVideos(Long channelId) {
        return Optional.ofNullable(getById(channelId));
    }

    /**
     * Find channels by user ID (not applicable in current entity, returns empty)
     */
    public List<YtbChannel> findByUserId(Integer userId) {
        return list(new LambdaQueryWrapper<YtbChannel>()
                .eq(YtbChannel::getIfValid, true));
    }

    /**
     * Find channels by user ID with pagination (not applicable in current entity)
     */
    public Page<YtbChannel> findByUserId(Integer userId, Page<YtbChannel> page) {
        return page(page, new LambdaQueryWrapper<YtbChannel>()
                .eq(YtbChannel::getIfValid, true));
    }

    /**
     * Find channels by status
     */
    public List<YtbChannel> findByStatus(Integer status) {
        return list(new LambdaQueryWrapper<YtbChannel>()
                .eq(YtbChannel::getStatus, status)
                .eq(YtbChannel::getIfValid, true));
    }

    /**
     * Find all channels with pagination
     */
    public Page<YtbChannel> findAllChannels(Page<YtbChannel> page) {
        return page(page, new LambdaQueryWrapper<YtbChannel>()
                .eq(YtbChannel::getIfValid, true));
    }

    /**
     * Save channel
     */
    @Transactional
    public YtbChannel saveChannel(YtbChannel channel) {
        saveOrUpdate(channel);
        return channel;
    }

    /**
     * Add video to channel
     */
    @Transactional
    public void addVideo(Long channelId, YtbChannelVideo video) {
        findById(channelId).ifPresent(channel -> {
            video.setChannelId(channelId);
            videoMapper.insert(video);
        });
    }

    /**
     * Update channel status
     */
    @Transactional
    public void updateStatus(Long channelId, Integer status) {
        findById(channelId).ifPresent(channel -> {
            channel.setStatus(status);
            updateById(channel);
        });
    }

    /**
     * Mark channel as invalid
     */
    @Transactional
    public void markInvalid(Long channelId) {
        findById(channelId).ifPresent(channel -> {
            channel.setIfValid(false);
            updateById(channel);
        });
    }

    /**
     * Delete channel
     */
    @Transactional
    public void deleteChannelById(Long channelId) {
        removeById(channelId);
    }

    /**
     * Find enabled channels (all valid channels are eligible for sync)
     */
    public List<YtbChannel> findEnabledChannels() {
        return list(new LambdaQueryWrapper<YtbChannel>()
                .eq(YtbChannel::getIfValid, true));
    }

    /**
     * Find videos by channel ID
     */
    public Page<YtbChannelVideo> findVideosByChannelId(Long channelId, Page<YtbChannelVideo> page) {
        return videoMapper.selectPage(page, new LambdaQueryWrapper<YtbChannelVideo>()
                .eq(YtbChannelVideo::getChannelId, channelId)
                .eq(YtbChannelVideo::getIfValid, true)
                .orderByDesc(YtbChannelVideo::getPublishedAt));
    }

    /**
     * Enable channel
     */
    @Transactional
    public void enableChannel(Long channelId) {
        findById(channelId).ifPresent(channel -> {
            channel.setStatus(1);
            channel.setIfValid(true);
            updateById(channel);
        });
    }

    /**
     * Disable channel
     */
    @Transactional
    public void disableChannel(Long channelId) {
        findById(channelId).ifPresent(channel -> {
            channel.setStatus(0);
            updateById(channel);
        });
    }
}
