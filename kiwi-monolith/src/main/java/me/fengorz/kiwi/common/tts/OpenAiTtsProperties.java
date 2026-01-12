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
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * OpenAI TTS Properties Configuration
 * Supports both standard TTS (tts-1) and steerable TTS (gpt-4o-mini-tts)
 *
 * @author codingByFeng
 */
@Data
@Component
@ConfigurationProperties(prefix = "kiwi.tts.openai")
public class OpenAiTtsProperties {

    /**
     * OpenAI API key
     */
    private String apiKey;

    /**
     * OpenAI TTS API base URL
     */
    private String baseUrl = "https://api.openai.com/v1/audio/speech";

    /**
     * TTS model: tts-1, tts-1-hd, or gpt-4o-mini-tts (steerable)
     */
    private String model = "gpt-4o-mini-tts";

    /**
     * Voice for English: alloy, echo, fable, onyx, nova, shimmer, coral, sage, ash
     */
    private String englishVoice = "alloy";

    /**
     * Voice for Chinese (nova works well for Chinese)
     */
    private String chineseVoice = "nova";

    /**
     * English accent: US, UK, AU, IN (only works with gpt-4o-mini-tts)
     */
    private AccentType englishAccent = AccentType.US;

    /**
     * Response format: mp3, opus, aac, flac, wav, pcm
     */
    private String responseFormat = "mp3";

    /**
     * Speech speed: 0.25 to 4.0 (1.0 is normal)
     */
    private Double speed = 1.0;

    /**
     * Whether to use steerable TTS (gpt-4o-mini-tts with instructions)
     */
    public boolean isSteerableTts() {
        return "gpt-4o-mini-tts".equalsIgnoreCase(model);
    }

    /**
     * Get accent instruction for steerable TTS
     */
    public String getAccentInstruction() {
        if (englishAccent == null) {
            return "";
        }
        return englishAccent.getInstruction();
    }

    /**
     * Supported English accents for steerable TTS
     */
    @Getter
    public enum AccentType {
        US("Speak with an American English accent. Use American pronunciation and intonation patterns."),
        UK("Speak with a British English accent. Use British pronunciation, vocabulary and intonation patterns typical of England."),
        AU("Speak with an Australian English accent. Use Australian pronunciation and intonation patterns."),
        IN("Speak with an Indian English accent. Use Indian English pronunciation patterns, with characteristic intonation and rhythm typical of Indian speakers.");

        private final String instruction;

        AccentType(String instruction) {
            this.instruction = instruction;
        }

        public static AccentType fromCode(String code) {
            if (code == null) {
                return US;
            }
            for (AccentType type : values()) {
                if (type.name().equalsIgnoreCase(code)) {
                    return type;
                }
            }
            return US;
        }
    }
}
