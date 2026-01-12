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

/**
 * OpenAI TTS Configuration Properties
 *
 * @Author Kason Zhan
 * @Date 2026/1/11
 */
@Getter
@Setter
public class OpenAiTtsProperties {

    /**
     * OpenAI API key for authentication
     */
    private String apiKey;

    /**
     * OpenAI TTS API endpoint
     * Default: https://api.openai.com/v1/audio/speech
     */
    private String baseUrl = "https://api.openai.com/v1/audio/speech";

    /**
     * TTS model to use: "tts-1" (standard) or "tts-1-hd" (high definition)
     * tts-1 is faster and cheaper, tts-1-hd has better quality
     */
    private String model = "tts-1";

    /**
     * Voice for English synthesis
     * Options: alloy, echo, fable, onyx, nova, shimmer
     */
    private String englishVoice = "alloy";

    /**
     * Voice for Chinese synthesis (uses same voice options)
     */
    private String chineseVoice = "nova";

    /**
     * Audio output format: mp3, opus, aac, flac, wav, pcm
     */
    private String responseFormat = "mp3";

    /**
     * Speed of the generated audio (0.25 to 4.0, default 1.0)
     */
    private Double speed = 1.0;
}
