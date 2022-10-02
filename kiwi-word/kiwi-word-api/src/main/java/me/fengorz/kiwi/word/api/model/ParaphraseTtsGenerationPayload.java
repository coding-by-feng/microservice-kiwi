/*
 *
 * Copyright [2019~2025] [codingByFeng]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 *
 */

package me.fengorz.kiwi.word.api.model;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.beans.factory.InitializingBean;

import lombok.Builder;
import lombok.Getter;
import me.fengorz.kiwi.word.api.common.enumeration.ReviewAudioSourceEnum;
import me.fengorz.kiwi.word.api.common.enumeration.ReviewAudioTypeEnum;

/**
 * @Description 用于配置每个tts audio要用哪个tts channel去gen
 * @Author zhanshifeng
 * @Date 2022/10/1 17:58
 */
@Getter
@Builder
public class ParaphraseTtsGenerationPayload implements InitializingBean {

    private List<ImmutablePair<ReviewAudioTypeEnum, ReviewAudioSourceEnum>> pairs;
    private List<ImmutablePair<ReviewAudioTypeEnum, Integer>> counters;
    private List<ImmutablePair<ReviewAudioTypeEnum, Boolean>> isReplacePayload;
    private List<ImmutablePair<ReviewAudioTypeEnum, Boolean>> enablePayload;

    private Map<ReviewAudioTypeEnum, ReviewAudioSourceEnum> typeAndSourceMap;
    private Map<ReviewAudioTypeEnum, Boolean> isReplaceMap;
    private Map<ReviewAudioTypeEnum, Boolean> isEnableMap;

    public ReviewAudioSourceEnum getFromReviewAudioTypeEnum(ReviewAudioTypeEnum type) {
        return this.typeAndSourceMap.get(type);
    }

    @Override
    public void afterPropertiesSet() {
        this.typeAndSourceMap = MapUtils.unmodifiableMap(
            this.pairs.stream().collect(Collectors.toMap(ImmutablePair::getLeft, ImmutablePair::getRight)));
        this.isReplaceMap = MapUtils.unmodifiableMap(
            this.isReplacePayload.stream().collect(Collectors.toMap(ImmutablePair::getLeft, ImmutablePair::getRight)));
        this.isEnableMap = MapUtils.unmodifiableMap(
            this.enablePayload.stream().collect(Collectors.toMap(ImmutablePair::getLeft, ImmutablePair::getRight)));
    }

    public Boolean getIsReplace(ReviewAudioTypeEnum type) {
        return this.isReplaceMap.get(type);
    }

    public Boolean getEnable(ReviewAudioTypeEnum type) {
        return this.isEnableMap.get(type);
    }

}
