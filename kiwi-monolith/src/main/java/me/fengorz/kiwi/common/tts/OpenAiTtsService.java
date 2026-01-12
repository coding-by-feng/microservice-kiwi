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

import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.exception.ServiceException;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * OpenAI TTS Service Implementation
 * Uses OpenAI's Text-to-Speech API to generate audio
 * Supports both standard TTS (tts-1) and steerable TTS (gpt-4o-mini-tts) with accent control
 *
 * @author codingByFeng
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "kiwi.tts.provider", havingValue = "openai", matchIfMissing = false)
public class OpenAiTtsService implements TtsService {

    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json");

    private final OkHttpClient httpClient;
    private final OpenAiTtsProperties properties;

    public OpenAiTtsService(OpenAiTtsProperties properties) {
        this.properties = properties;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        log.info("OpenAI TTS Service initialized with model: {}, steerable: {}",
                properties.getModel(), properties.isSteerableTts());
    }

    @Override
    public byte[] speechEnglish(String text) {
        if (properties.isSteerableTts()) {
            return synthesizeWithInstructions(text, properties.getEnglishVoice(), properties.getAccentInstruction());
        }
        return synthesize(text, properties.getEnglishVoice(), null);
    }

    @Override
    public byte[] speechChinese(String text) {
        return synthesize(text, properties.getChineseVoice(), null);
    }

    /**
     * Generate speech with specific voice and accent (for conversation feature)
     *
     * @param text   the text to synthesize
     * @param voice  the voice to use (alloy, echo, fable, etc.)
     * @param accent the accent type (US, UK, AU, IN)
     * @return audio bytes
     */
    public byte[] speechWithAccent(String text, String voice, OpenAiTtsProperties.AccentType accent) {
        if (properties.isSteerableTts() && accent != null) {
            log.info("Generating TTS with accent: {} | voice: {} | instruction: {}",
                    accent.name(), voice, accent.getInstruction());
            return synthesizeWithInstructions(text, voice, accent.getInstruction());
        }
        log.warn("Steerable TTS not enabled or accent is null. Model: {}, Accent: {}",
                properties.getModel(), accent);
        return synthesize(text, voice, null);
    }

    /**
     * Generate speech with custom instructions (for advanced use cases)
     *
     * @param text         the text to synthesize
     * @param voice        the voice to use
     * @param instructions custom instructions for the TTS model
     * @return audio bytes
     */
    public byte[] synthesizeWithInstructions(String text, String voice, String instructions) {
        return synthesize(text, voice, instructions);
    }

    private byte[] synthesize(String text, String voice, String instructions) {
        if (StringUtils.isBlank(text)) {
            log.warn("Empty text provided for TTS");
            return new byte[0];
        }

        if (StringUtils.isBlank(properties.getApiKey())) {
            throw new ServiceException("OpenAI API key is not configured");
        }

        String jsonPayload = buildJsonPayload(text, voice, instructions);

        RequestBody body = RequestBody.create(JSON_MEDIA_TYPE, jsonPayload);
        Request request = new Request.Builder()
                .url(properties.getBaseUrl())
                .addHeader("Authorization", "Bearer " + properties.getApiKey())
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "No error body";
                log.error("OpenAI TTS request failed with status {}: {}", response.code(), errorBody);
                throw new ServiceException("OpenAI TTS request failed: " + response.code());
            }

            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                throw new ServiceException("OpenAI TTS response body is empty");
            }

            byte[] audioBytes = responseBody.bytes();
            log.debug("Successfully generated {} bytes of audio for text length {}", audioBytes.length, text.length());
            return audioBytes;
        } catch (IOException e) {
            log.error("OpenAI TTS API call failed", e);
            throw new ServiceException("OpenAI TTS API call failed: " + e.getMessage());
        }
    }

    private String buildJsonPayload(String text, String voice, String instructions) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"model\":\"").append(properties.getModel()).append("\",");
        json.append("\"input\":\"").append(escapeJson(text)).append("\",");
        json.append("\"voice\":\"").append(voice).append("\",");
        json.append("\"response_format\":\"").append(properties.getResponseFormat()).append("\",");
        json.append("\"speed\":").append(properties.getSpeed());

        // Add instructions for steerable TTS (gpt-4o-mini-tts)
        if (properties.isSteerableTts() && StringUtils.isNotBlank(instructions)) {
            json.append(",\"instructions\":\"").append(escapeJson(instructions)).append("\"");
        }

        json.append("}");
        return json.toString();
    }

    private String escapeJson(String text) {
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
