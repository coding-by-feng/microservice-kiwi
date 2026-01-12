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

package me.fengorz.kiwi.common.tts.model;

import lombok.Getter;
import lombok.Setter;
import me.fengorz.kiwi.common.tts.enumeration.TtsSourceEnum;

/**
 * TTS Provider Selection Properties
 * Allows switching between different TTS providers via YAML configuration
 *
 * @Author Kason Zhan
 * @Date 2026/1/11
 */
@Getter
@Setter
public class TtsProviderProperties {

    /**
     * Active TTS provider: voicerss, baidu, gcp, deepgram, openai
     * Default is "openai" for cost-effectiveness
     */
    private String provider = "openai";

    public boolean isVoiceRss() {
        return TtsSourceEnum.VOICERSS.getSource().equalsIgnoreCase(provider);
    }

    public boolean isBaidu() {
        return TtsSourceEnum.BAIDU.getSource().equalsIgnoreCase(provider);
    }

    public boolean isGcp() {
        return TtsSourceEnum.GCP.getSource().equalsIgnoreCase(provider);
    }

    public boolean isDeepgram() {
        return TtsSourceEnum.DEEPGRAM.getSource().equalsIgnoreCase(provider);
    }

    public boolean isOpenAi() {
        return TtsSourceEnum.OPENAI.getSource().equalsIgnoreCase(provider);
    }
}
