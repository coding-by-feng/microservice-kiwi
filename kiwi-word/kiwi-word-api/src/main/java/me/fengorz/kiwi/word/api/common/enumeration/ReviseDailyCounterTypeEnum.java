/*
 *
 *   Copyright [2019~2025] [codingByFeng]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 *
 */

package me.fengorz.kiwi.word.api.common.enumeration;

import lombok.Getter;

/**
 * @Author Kason Zhan
 * @Date 2021/8/21 8:11 PM
 */
public enum ReviseDailyCounterTypeEnum {

    /**
     * 1：remember 2：keep in mind 3：review
     */
    REMEMBER(1), KEEP_IN_MIND(2), REVIEW_COUNTER(3), REVIEW_AUDIO_BAIDU_TTS_COUNTER(4),
    REVIEW_AUDIO_VOICERSS_TTS_COUNTER(5);

    @Getter
    private final int type;

    ReviseDailyCounterTypeEnum(int type) {
        this.type = type;
    }

}
