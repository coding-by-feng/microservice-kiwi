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
package me.fengorz.kiwi.common.tts;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Gemini TTS Properties Configuration
 * Uses Vertex AI endpoint with Bearer token authentication
 *
 * @author codingByFeng
 */
@Data
@Component
@ConfigurationProperties(prefix = "kiwi.tts.gemini")
public class GeminiTtsProperties {

    /**
     * Gemini TTS model name
     */
    private String model = "gemini-2.5-flash-preview-tts";

    /**
     * Default English voice name (e.g., Kore, Puck, Charon, Aoede, Fenrir, Leda, Orus, Zephyr)
     */
    private String englishVoice = "Kore";

    /**
     * Default Chinese voice name
     */
    private String chineseVoice = "Kore";

    /**
     * Voice mapping: abstract voice name (alloy, echo, etc.) -> Gemini voice name
     */
    private Map<String, String> voiceMapping = new HashMap<>();

    /**
     * Gemini API key for fallback (generativelanguage.googleapis.com)
     */
    private String apiKey;

    /**
     * Build the Gemini API fallback endpoint URL
     */
    public String getGeminiApiEndpoint() {
        return String.format("https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s",
                model, apiKey);
    }
}
