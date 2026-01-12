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
import me.fengorz.kiwi.domain.ai.entity.YtbVideoSubtitlesTranslation;
import me.fengorz.kiwi.domain.ai.mapper.YtbVideoSubtitlesTranslationMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * YtbVideoSubtitlesTranslation Service - manages YouTube video subtitles translation operations
 *
 * @author codingByFeng
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class YtbVideoSubtitlesTranslationService extends ServiceImpl<YtbVideoSubtitlesTranslationMapper, YtbVideoSubtitlesTranslation> {

    /**
     * Find translation by ID
     */
    public Optional<YtbVideoSubtitlesTranslation> findById(Long translationId) {
        return Optional.ofNullable(getById(translationId));
    }

    /**
     * Find translations by subtitles ID
     */
    public List<YtbVideoSubtitlesTranslation> findBySubtitlesId(Long subtitlesId) {
        return list(new LambdaQueryWrapper<YtbVideoSubtitlesTranslation>()
                .eq(YtbVideoSubtitlesTranslation::getSubtitlesId, subtitlesId)
                .eq(YtbVideoSubtitlesTranslation::getIfValid, true));
    }

    /**
     * Find translation by subtitles ID and language
     */
    public Optional<YtbVideoSubtitlesTranslation> findBySubtitlesIdAndLang(Long subtitlesId, String lang) {
        return Optional.ofNullable(getOne(new LambdaQueryWrapper<YtbVideoSubtitlesTranslation>()
                .eq(YtbVideoSubtitlesTranslation::getSubtitlesId, subtitlesId)
                .eq(YtbVideoSubtitlesTranslation::getLang, lang)
                .eq(YtbVideoSubtitlesTranslation::getIfValid, true)));
    }

    /**
     * Save translation
     */
    @Transactional
    public YtbVideoSubtitlesTranslation saveTranslation(YtbVideoSubtitlesTranslation translation) {
        saveOrUpdate(translation);
        return translation;
    }

    /**
     * Delete translation by ID
     */
    @Transactional
    public void deleteById(Long translationId) {
        removeById(translationId);
    }

    /**
     * Delete translations by subtitles ID
     */
    @Transactional
    public void deleteBySubtitlesId(Long subtitlesId) {
        remove(new LambdaQueryWrapper<YtbVideoSubtitlesTranslation>()
                .eq(YtbVideoSubtitlesTranslation::getSubtitlesId, subtitlesId));
    }

    /**
     * Delete translations by multiple subtitles IDs
     */
    @Transactional
    public void deleteBySubtitlesIds(List<Long> subtitlesIds) {
        if (subtitlesIds == null || subtitlesIds.isEmpty()) {
            return;
        }
        remove(new LambdaQueryWrapper<YtbVideoSubtitlesTranslation>()
                .in(YtbVideoSubtitlesTranslation::getSubtitlesId, subtitlesIds));
    }
}
