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

package me.fengorz.kason.common.tts.enumeration;

import lombok.Getter;

/**
 * @Description TTS power source
 * @Author Kason Zhan
 * @Date 2022/7/12 09:20
 */
public enum TtsSourceEnum {

    VOICERSS("voicerss"), BAIDU("baidu"), COMBO("combo"),
    GCP("gcp"), DEEPGRAM("deepgram");

    @Getter
    private final String source;

    TtsSourceEnum(String source) {
        this.source = source;
    }
}
