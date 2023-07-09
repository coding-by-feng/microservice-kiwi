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

import lombok.Builder;
import lombok.Getter;
import me.fengorz.kiwi.common.tts.enumeration.TtsSourceEnum;
import me.fengorz.kiwi.word.api.common.enumeration.ReviseAudioTypeEnum;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.beans.factory.InitializingBean;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Description 用于配置每个tts audio要用哪个tts channel去gen
 * @Author zhanshifeng
 * @Date 2022/10/1 17:58
 */
@Getter
@Builder
public class ParaphraseTtsGenerationPayload implements InitializingBean {

    private List<ImmutablePair<ReviseAudioTypeEnum, TtsSourceEnum>> pairs;
    private List<ImmutablePair<ReviseAudioTypeEnum, Integer>> counters;
    private List<ImmutablePair<ReviseAudioTypeEnum, Boolean>> isReplacePayload;
    private List<ImmutablePair<ReviseAudioTypeEnum, Boolean>> enablePayload;

    private Map<ReviseAudioTypeEnum, TtsSourceEnum> typeAndSourceMap;
    private Map<ReviseAudioTypeEnum, Boolean> isReplaceMap;
    private Map<ReviseAudioTypeEnum, Boolean> isEnableMap;

    public TtsSourceEnum getFromReviewAudioTypeEnum(ReviseAudioTypeEnum type) {
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

    public Boolean getIsReplace(ReviseAudioTypeEnum type) {
        return BooleanUtils.toBoolean(this.isReplaceMap.get(type));
    }

    public Boolean getIsReplace(Integer type) {
        return BooleanUtils.toBoolean(this.isReplaceMap.get(ReviseAudioTypeEnum.fromValue(type)));
    }

    public Boolean getEnable(ReviseAudioTypeEnum type) {
        return this.isEnableMap.get(type);
    }

}
