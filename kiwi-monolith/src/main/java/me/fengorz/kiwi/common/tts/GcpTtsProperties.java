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
 * Google Cloud TTS Properties Configuration
 *
 * @author codingByFeng
 */
@Data
@Component
@ConfigurationProperties(prefix = "kiwi.tts.gcp")
public class GcpTtsProperties {

    /**
     * Default English voice name (e.g., en-US-Journey-D)
     */
    private String englishVoice = "en-US-Journey-D";

    /**
     * Default Chinese voice name (e.g., cmn-CN-Wavenet-A)
     */
    private String chineseVoice = "cmn-CN-Wavenet-A";

    /**
     * Speaking rate: 0.25 to 4.0 (1.0 is normal)
     */
    private double speakingRate = 1.0;

    /**
     * Pitch adjustment: -20.0 to 20.0 semitones
     */
    private double pitch = 0.0;

    /**
     * Voice pool mapping: abstract voice name -> accent -> GCP voice name.
     * E.g., voicePool.alloy.US = "en-US-Journey-D"
     */
    private Map<String, Map<String, String>> voicePool = new HashMap<>();
}
