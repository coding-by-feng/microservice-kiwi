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

package me.fengorz.kason.common.tts.model;

import lombok.Getter;
import lombok.Setter;

/**
 * @Author Kason Zhan
 * @Date 2024/7/13
 */
@Getter
@Setter
public class DeepgramTtsProperties {

    /**
     * Deepgram API key used to authorize calls to the Aura endpoint.
     */
    private String apiKey;

    /**
     * Endpoint used for text-to-speech synthesis.
     */
    private String baseUrl = "https://api.deepgram.com/v1/speak";

    /**
     * Aura-2 model identifier for English synthesis.
     */
    private String englishModel;

    /**
     * Aura-2 model identifier for Chinese synthesis.
     */
    private String chineseModel;
}
