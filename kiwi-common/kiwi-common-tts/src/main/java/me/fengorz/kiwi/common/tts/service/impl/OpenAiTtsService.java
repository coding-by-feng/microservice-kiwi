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

package me.fengorz.kiwi.common.tts.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.sdk.exception.tts.TtsException;
import me.fengorz.kiwi.common.tts.enumeration.TtsSourceEnum;
import me.fengorz.kiwi.common.tts.model.OpenAiTtsProperties;
import me.fengorz.kiwi.common.tts.service.TtsService;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * OpenAI TTS Service Implementation
 * Uses OpenAI's Text-to-Speech API (tts-1 or tts-1-hd models)
 * Pricing: $15/1M chars (tts-1) or $30/1M chars (tts-1-hd)
 *
 * @Author Kason Zhan
 * @Date 2026/1/11
 */
@Slf4j
@Service("openAiTtsService")
@RequiredArgsConstructor
public class OpenAiTtsService implements TtsService {

    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final String HEADER_CONTENT_TYPE = "Content-Type";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json");

    private final OpenAiTtsProperties properties;
    private final OkHttpClient httpClient;

    @Override
    public byte[] speechEnglish(String text) throws TtsException {
        return synthesize(text, properties.getEnglishVoice(), "en");
    }

    @Override
    public byte[] speechChinese(String text) throws TtsException {
        return synthesize(text, properties.getChineseVoice(), "zh");
    }

    private byte[] synthesize(String text, String voice, String languageCode) throws TtsException {
        if (StringUtils.isBlank(text)) {
            throw new TtsException("%s TTS text must not be blank.", TtsSourceEnum.OPENAI.getSource());
        }
        if (StringUtils.isBlank(properties.getApiKey())) {
            throw new TtsException("%s TTS apiKey is not configured.", TtsSourceEnum.OPENAI.getSource());
        }
        if (StringUtils.isBlank(voice)) {
            throw new TtsException("%s TTS voice is not configured for language %s.",
                    TtsSourceEnum.OPENAI.getSource(), languageCode);
        }

        Request request = buildRequest(text, voice);
        return executeRequest(request, voice, languageCode);
    }

    private Request buildRequest(String text, String voice) {
        JSONObject payload = new JSONObject();
        payload.put("model", properties.getModel());
        payload.put("input", text);
        payload.put("voice", voice);
        payload.put("response_format", properties.getResponseFormat());
        if (properties.getSpeed() != null && properties.getSpeed() != 1.0) {
            payload.put("speed", properties.getSpeed());
        }

        RequestBody body = RequestBody.create(JSON_MEDIA_TYPE, payload.toString());
        return new Request.Builder()
                .url(resolveBaseUrl())
                .addHeader(HEADER_AUTHORIZATION, BEARER_PREFIX + properties.getApiKey())
                .addHeader(HEADER_CONTENT_TYPE, JSON_MEDIA_TYPE.toString())
                .post(body)
                .build();
    }

    private byte[] executeRequest(Request request, String voice, String languageCode) throws TtsException {
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorMsg = extractErrorMessage(response);
                log.error("{} TTS request failed. status={}, voice={}, language={}, error={}",
                        TtsSourceEnum.OPENAI.getSource(), response.code(), voice, languageCode, errorMsg);
                throw new TtsException("%s TTS request failed with status %s: %s",
                        TtsSourceEnum.OPENAI.getSource(), response.code(), errorMsg);
            }

            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                throw new TtsException("%s TTS response body is empty.", TtsSourceEnum.OPENAI.getSource());
            }
            return responseBody.bytes();
        } catch (IOException e) {
            log.error("{} TTS request threw an exception.", TtsSourceEnum.OPENAI.getSource(), e);
            throw new TtsException("%s TTS request failed.", e, TtsSourceEnum.OPENAI.getSource());
        }
    }

    private String resolveBaseUrl() {
        return StringUtils.defaultIfBlank(properties.getBaseUrl(), "https://api.openai.com/v1/audio/speech");
    }

    private String extractErrorMessage(Response response) {
        ResponseBody responseBody = response.body();
        if (responseBody == null) {
            return "<empty response body>";
        }
        try {
            return responseBody.string();
        } catch (IOException e) {
            log.warn("Failed to read OpenAI TTS error response body.", e);
            return "<failed to read error body>";
        }
    }
}
