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
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.domain.ai.entity.YtbVideoSubtitles;
import me.fengorz.kiwi.domain.ai.mapper.YtbVideoSubtitlesMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * YtbVideoSubtitles Service - manages YouTube video subtitle operations
 *
 * @author codingByFeng
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class YtbVideoSubtitlesService extends ServiceImpl<YtbVideoSubtitlesMapper, YtbVideoSubtitles> {

    /**
     * Find subtitles by ID
     */
    public Optional<YtbVideoSubtitles> findById(Long subtitlesId) {
        return Optional.ofNullable(getById(subtitlesId));
    }

    /**
     * Find subtitles by ID with translations
     */
    public Optional<YtbVideoSubtitles> findByIdWithTranslations(Long subtitlesId) {
        return Optional.ofNullable(getById(subtitlesId));
    }

    /**
     * Find subtitles by video ID
     */
    public List<YtbVideoSubtitles> findByVideoId(Long videoId) {
        return list(new LambdaQueryWrapper<YtbVideoSubtitles>()
                .eq(YtbVideoSubtitles::getVideoId, videoId)
                .eq(YtbVideoSubtitles::getIfValid, true));
    }

    /**
     * Find subtitles by video ID with translations
     */
    public List<YtbVideoSubtitles> findByVideoIdWithTranslations(Long videoId) {
        return list(new LambdaQueryWrapper<YtbVideoSubtitles>()
                .eq(YtbVideoSubtitles::getVideoId, videoId)
                .eq(YtbVideoSubtitles::getIfValid, true));
    }

    /**
     * Find subtitles by video ID and type
     */
    public Optional<YtbVideoSubtitles> findByVideoIdAndType(Long videoId, String type) {
        return Optional.ofNullable(getOne(new LambdaQueryWrapper<YtbVideoSubtitles>()
                .eq(YtbVideoSubtitles::getVideoId, videoId)
                .eq(YtbVideoSubtitles::getType, type)
                .eq(YtbVideoSubtitles::getIfValid, true)));
    }

    /**
     * Find subtitles by status
     */
    public List<YtbVideoSubtitles> findByStatus(Integer status) {
        return list(new LambdaQueryWrapper<YtbVideoSubtitles>()
                .eq(YtbVideoSubtitles::getStatus, status)
                .eq(YtbVideoSubtitles::getIfValid, true));
    }

    /**
     * Count subtitles by video ID
     */
    public long countByVideoId(Long videoId) {
        return count(new LambdaQueryWrapper<YtbVideoSubtitles>()
                .eq(YtbVideoSubtitles::getVideoId, videoId)
                .eq(YtbVideoSubtitles::getIfValid, true));
    }

    /**
     * Save subtitles
     */
    @Transactional
    public YtbVideoSubtitles saveSubtitles(YtbVideoSubtitles subtitles) {
        saveOrUpdate(subtitles);
        return subtitles;
    }

    /**
     * Update subtitles status
     */
    @Transactional
    public void updateStatus(Long subtitlesId, Integer status) {
        findById(subtitlesId).ifPresent(subtitles -> {
            subtitles.setStatus(status);
            updateById(subtitles);
        });
    }

    /**
     * Update subtitles text
     */
    @Transactional
    public void updateSubtitlesText(Long subtitlesId, String subtitlesText) {
        findById(subtitlesId).ifPresent(subtitles -> {
            subtitles.setSubtitlesText(subtitlesText);
            updateById(subtitles);
        });
    }

    /**
     * Mark subtitles as finished
     */
    @Transactional
    public void markFinished(Long subtitlesId) {
        updateStatus(subtitlesId, YtbVideoSubtitles.STATUS_FINISH);
    }

    /**
     * Mark subtitles as invalid
     */
    @Transactional
    public void markInvalid(Long subtitlesId) {
        findById(subtitlesId).ifPresent(subtitles -> {
            subtitles.setIfValid(false);
            updateById(subtitles);
        });
    }

    /**
     * Delete subtitles
     */
    @Transactional
    public void deleteSubtitlesById(Long subtitlesId) {
        removeById(subtitlesId);
    }

    /**
     * Delete subtitles by video ID
     */
    @Transactional
    public void deleteByVideoId(Long videoId) {
        remove(new LambdaQueryWrapper<YtbVideoSubtitles>()
                .eq(YtbVideoSubtitles::getVideoId, videoId));
    }
}
