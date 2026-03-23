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

import com.google.cloud.texttospeech.v1.*;
import com.google.protobuf.ByteString;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.exception.ServiceException;
import me.fengorz.kiwi.domain.ai.config.VertexAiCredentialProvider;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

/**
 * Google Cloud TTS Service Implementation
 * Uses Google Cloud Text-to-Speech API with Vertex AI credentials
 *
 * @author codingByFeng
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "kiwi.tts.provider", havingValue = "gcp", matchIfMissing = false)
public class GcpTtsService implements TtsService {

    private final GcpTtsProperties properties;
    private final VertexAiCredentialProvider credentialProvider;

    private TextToSpeechClient client;

    @PostConstruct
    public void init() {
        try {
            TextToSpeechSettings settings = TextToSpeechSettings.newBuilder()
                    .setCredentialsProvider(() -> credentialProvider.getCredentials())
                    .build();
            client = TextToSpeechClient.create(settings);
            log.info("Google Cloud TTS Service initialized, english voice: {}, chinese voice: {}",
                    properties.getEnglishVoice(), properties.getChineseVoice());
        } catch (IOException e) {
            log.error("Failed to initialize Google Cloud TTS client", e);
            throw new ServiceException("Failed to initialize Google Cloud TTS client: " + e.getMessage(), e);
        }
    }

    @PreDestroy
    public void destroy() {
        if (client != null) {
            client.close();
        }
    }

    @Override
    public byte[] speechEnglish(String text) {
        return synthesize(text, properties.getEnglishVoice(), "en-US");
    }

    @Override
    public byte[] speechChinese(String text) {
        return synthesize(text, properties.getChineseVoice(), "cmn-CN");
    }

    @Override
    public byte[] speechWithAccent(String text, String voice, AccentType accent) {
        if (accent == null) {
            accent = AccentType.US;
        }
        String languageCode = accent.getLanguageCode();
        String gcpVoiceName = resolveVoiceName(voice, accent);
        log.info("Generating GCP TTS with accent: {} | voice: {} | gcpVoice: {}",
                accent.name(), voice, gcpVoiceName);
        return synthesize(text, gcpVoiceName, languageCode);
    }

    private byte[] synthesize(String text, String voiceName, String languageCode) {
        if (StringUtils.isBlank(text)) {
            log.warn("Empty text provided for TTS");
            return new byte[0];
        }

        try {
            SynthesisInput input = SynthesisInput.newBuilder()
                    .setText(text)
                    .build();

            VoiceSelectionParams voiceParams = VoiceSelectionParams.newBuilder()
                    .setLanguageCode(languageCode)
                    .setName(voiceName)
                    .build();

            AudioConfig audioConfig = AudioConfig.newBuilder()
                    .setAudioEncoding(AudioEncoding.MP3)
                    .setSpeakingRate(properties.getSpeakingRate())
                    .setPitch(properties.getPitch())
                    .build();

            SynthesizeSpeechResponse response = client.synthesizeSpeech(input, voiceParams, audioConfig);
            ByteString audioContent = response.getAudioContent();
            byte[] audioBytes = audioContent.toByteArray();

            log.debug("Successfully generated {} bytes of audio for text length {}", audioBytes.length, text.length());
            return audioBytes;

        } catch (Exception e) {
            log.error("Google Cloud TTS synthesis failed for voice {} ({})", voiceName, languageCode, e);
            throw new ServiceException("Google Cloud TTS synthesis failed: " + e.getMessage());
        }
    }

    /**
     * Resolve abstract voice name (alloy, echo, etc.) to a GCP TTS voice name.
     * Checks the voice pool config first, then falls back to the default english voice
     * with the accent's language code prefix.
     */
    private String resolveVoiceName(String abstractVoice, AccentType accent) {
        // Try voice pool lookup: voicePool.alloy.US -> "en-US-Journey-D"
        Map<String, Map<String, String>> voicePool = properties.getVoicePool();
        if (voicePool != null && abstractVoice != null) {
            Map<String, String> accentMap = voicePool.get(abstractVoice);
            if (accentMap != null) {
                String gcpVoice = accentMap.get(accent.name());
                if (StringUtils.isNotBlank(gcpVoice)) {
                    return gcpVoice;
                }
            }
        }

        // Fallback: replace the language prefix of the default english voice
        // e.g., "en-US-Journey-D" with UK accent -> "en-GB-Journey-D"
        String defaultVoice = properties.getEnglishVoice();
        if (defaultVoice != null && defaultVoice.startsWith("en-")) {
            return accent.getLanguageCode() + defaultVoice.substring(5);
        }

        return defaultVoice;
    }
}
