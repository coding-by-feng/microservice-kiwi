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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.exception.ServiceException;
import me.fengorz.kiwi.domain.ai.config.GeminiApiProperties;
import me.fengorz.kiwi.domain.ai.config.VertexAiCredentialProvider;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

/**
 * Gemini TTS Service Implementation
 * Uses Vertex AI endpoint with Bearer token authentication for Text-to-Speech
 *
 * @author codingByFeng
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "kiwi.tts.provider", havingValue = "gemini", matchIfMissing = false)
public class GeminiTtsService implements TtsService {

    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json");

    private final OkHttpClient httpClient;
    private final GeminiTtsProperties properties;
    private final GeminiApiProperties apiProperties;
    private final VertexAiCredentialProvider credentialProvider;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GeminiTtsService(GeminiTtsProperties properties,
                            GeminiApiProperties apiProperties,
                            VertexAiCredentialProvider credentialProvider) {
        this.properties = properties;
        this.apiProperties = apiProperties;
        this.credentialProvider = credentialProvider;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        log.info("Gemini TTS Service initialized with Vertex AI endpoint, model: {}, english voice: {}",
                properties.getModel(), properties.getEnglishVoice());
    }

    @Override
    public byte[] speechEnglish(String text) {
        return synthesize(text, properties.getEnglishVoice());
    }

    @Override
    public byte[] speechChinese(String text) {
        return synthesize(text, properties.getChineseVoice());
    }

    @Override
    public String getAudioFormat() {
        return "wav";
    }

    @Override
    public byte[] speechWithAccent(String text, String voice, AccentType accent) {
        String geminiVoice = resolveVoiceName(voice);
        log.info("Generating Gemini TTS with accent: {} | voice: {} | geminiVoice: {}",
                accent != null ? accent.name() : "default", voice, geminiVoice);
        return synthesize(text, geminiVoice);
    }

    private String buildEndpoint() {
        return String.format("%s/%s:generateContent",
                apiProperties.getVertexAiEndpoint(), properties.getModel());
    }

    /**
     * Synthesize audio: tries Vertex AI first, falls back to Gemini API on auth errors.
     */
    private byte[] synthesize(String text, String voiceName) {
        if (StringUtils.isBlank(text)) {
            log.warn("Empty text provided for TTS");
            return new byte[0];
        }

        try {
            return synthesizeViaVertexAi(text, voiceName);
        } catch (ServiceException e) {
            if (e.getMessage() != null && (e.getMessage().contains("403") || e.getMessage().contains("401"))) {
                log.warn("Vertex AI TTS failed with auth error, falling back to Gemini API: {}", e.getMessage());
                return synthesizeViaGeminiApi(text, voiceName);
            }
            throw e;
        }
    }

    private byte[] synthesizeViaVertexAi(String text, String voiceName) {
        String accessToken = credentialProvider.getAccessToken();
        String endpoint = buildEndpoint();
        String jsonPayload = buildJsonPayload(text, voiceName);
        RequestBody body = RequestBody.create(JSON_MEDIA_TYPE, jsonPayload);

        Request request = new Request.Builder()
                .url(endpoint)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + accessToken)
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "No error body";
                log.error("Vertex AI TTS request failed with status {}: {}", response.code(), errorBody);
                throw new ServiceException("Vertex AI TTS request failed: " + response.code());
            }

            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                throw new ServiceException("Vertex AI TTS response body is empty");
            }

            String responseJson = responseBody.string();
            return extractAudioBytes(responseJson);

        } catch (IOException e) {
            log.error("Vertex AI TTS API call failed", e);
            throw new ServiceException("Vertex AI TTS API call failed: " + e.getMessage());
        }
    }

    private byte[] synthesizeViaGeminiApi(String text, String voiceName) {
        if (StringUtils.isBlank(properties.getApiKey())) {
            throw new ServiceException("Gemini API key not configured for TTS fallback");
        }

        String endpoint = properties.getGeminiApiEndpoint();
        String jsonPayload = buildJsonPayload(text, voiceName);
        RequestBody body = RequestBody.create(JSON_MEDIA_TYPE, jsonPayload);

        Request request = new Request.Builder()
                .url(endpoint)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "No error body";
                log.error("Gemini API TTS request failed with status {}: {}", response.code(), errorBody);
                throw new ServiceException("Gemini API TTS request failed: " + response.code() + " - " + errorBody);
            }

            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                throw new ServiceException("Gemini API TTS response body is empty");
            }

            String responseJson = responseBody.string();
            return extractAudioBytes(responseJson);

        } catch (IOException e) {
            log.error("Gemini API TTS call failed", e);
            throw new ServiceException("Gemini API TTS call failed: " + e.getMessage());
        }
    }

    private String buildJsonPayload(String text, String voiceName) {
        try {
            String escaped = objectMapper.writeValueAsString(text);
            return String.format("""
                {
                    "contents": [
                        {
                            "role": "user",
                            "parts": [
                                {"text": %s}
                            ]
                        }
                    ],
                    "generationConfig": {
                        "response_modalities": ["AUDIO"],
                        "speech_config": {
                            "voice_config": {
                                "prebuilt_voice_config": {
                                    "voice_name": "%s"
                                }
                            }
                        }
                    }
                }
                """, escaped, voiceName);
        } catch (Exception e) {
            throw new ServiceException("Failed to build Gemini TTS request payload: " + e.getMessage());
        }
    }

    private byte[] extractAudioBytes(String responseJson) {
        try {
            JsonNode root = objectMapper.readTree(responseJson);
            JsonNode candidates = root.get("candidates");

            if (candidates == null || !candidates.isArray() || candidates.isEmpty()) {
                throw new ServiceException("No audio generated in Gemini TTS response");
            }

            JsonNode parts = candidates.get(0).path("content").path("parts");
            if (!parts.isArray() || parts.isEmpty()) {
                throw new ServiceException("No audio parts in Gemini TTS response");
            }

            JsonNode inlineData = parts.get(0).get("inlineData");
            if (inlineData == null) {
                throw new ServiceException("No inline data in Gemini TTS response");
            }

            String mimeType = inlineData.get("mimeType").asText();
            String base64Data = inlineData.get("data").asText();

            byte[] pcmBytes = Base64.getDecoder().decode(base64Data);

            // Parse sample rate from mimeType (e.g., "audio/L16;rate=24000")
            int sampleRate = parseSampleRate(mimeType);

            log.debug("Successfully received {} bytes of PCM audio ({}), converting to WAV", pcmBytes.length, mimeType);
            return pcmToWav(pcmBytes, sampleRate);

        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to parse Gemini TTS response", e);
            throw new ServiceException("Failed to parse Gemini TTS response: " + e.getMessage());
        }
    }

    private int parseSampleRate(String mimeType) {
        if (mimeType != null && mimeType.contains("rate=")) {
            try {
                String rateStr = mimeType.substring(mimeType.indexOf("rate=") + 5);
                int end = rateStr.indexOf(';');
                if (end > 0) {
                    rateStr = rateStr.substring(0, end);
                }
                return Integer.parseInt(rateStr.trim());
            } catch (NumberFormatException e) {
                log.warn("Failed to parse sample rate from mimeType: {}, defaulting to 24000", mimeType);
            }
        }
        return 24000;
    }

    /**
     * Convert raw PCM 16-bit mono audio to WAV format by prepending a 44-byte WAV header
     */
    private byte[] pcmToWav(byte[] pcmData, int sampleRate) {
        int channels = 1;
        int bitsPerSample = 16;
        int byteRate = sampleRate * channels * bitsPerSample / 8;
        int blockAlign = channels * bitsPerSample / 8;
        int dataSize = pcmData.length;
        int chunkSize = 36 + dataSize;

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream(44 + dataSize);
            ByteBuffer header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN);

            // RIFF header
            header.put("RIFF".getBytes());
            header.putInt(chunkSize);
            header.put("WAVE".getBytes());

            // fmt sub-chunk
            header.put("fmt ".getBytes());
            header.putInt(16);
            header.putShort((short) 1);     // PCM format
            header.putShort((short) channels);
            header.putInt(sampleRate);
            header.putInt(byteRate);
            header.putShort((short) blockAlign);
            header.putShort((short) bitsPerSample);

            // data sub-chunk
            header.put("data".getBytes());
            header.putInt(dataSize);

            out.write(header.array());
            out.write(pcmData);
            return out.toByteArray();

        } catch (IOException e) {
            throw new ServiceException("Failed to convert PCM to WAV: " + e.getMessage());
        }
    }

    /**
     * Resolve abstract voice name (alloy, echo, etc.) to a Gemini TTS voice name
     */
    private String resolveVoiceName(String abstractVoice) {
        if (StringUtils.isBlank(abstractVoice)) {
            return properties.getEnglishVoice();
        }

        // Check voice mapping config first
        if (properties.getVoiceMapping() != null && !properties.getVoiceMapping().isEmpty()) {
            String mapped = properties.getVoiceMapping().get(abstractVoice);
            if (StringUtils.isNotBlank(mapped)) {
                return mapped;
            }
        }

        // Default mapping for common OpenAI voice names
        return switch (abstractVoice.toLowerCase()) {
            case "alloy" -> "Kore";
            case "echo" -> "Charon";
            case "fable" -> "Aoede";
            case "onyx" -> "Fenrir";
            case "nova" -> "Puck";
            case "shimmer" -> "Leda";
            case "coral" -> "Zephyr";
            case "sage" -> "Orus";
            default -> abstractVoice;
        };
    }
}
