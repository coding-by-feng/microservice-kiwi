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
import me.fengorz.kiwi.common.tts.model.DeepgramTtsProperties;
import me.fengorz.kiwi.common.tts.service.TtsBaseService;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * @Author Kason Zhan
 * @Date 2024/7/13
 */
@Slf4j
@Service("deepgramAura2TtsService")
@RequiredArgsConstructor
public class DeepgramAura2TtsService implements TtsBaseService {

    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final String HEADER_CONTENT_TYPE = "Content-Type";
    private static final String TOKEN_PREFIX = "Token ";
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json");

    private final DeepgramTtsProperties properties;
    private final OkHttpClient httpClient;

    @Override
    public byte[] speechEnglish(String text) throws TtsException {
        return synthesize(text, properties.getEnglishModel(), "en");
    }

    @Override
    public byte[] speechChinese(String text) throws TtsException {
        return synthesize(text, properties.getChineseModel(), "zh");
    }

    private byte[] synthesize(String text, String model, String languageCode) throws TtsException {
        if (StringUtils.isBlank(text)) {
            throw new TtsException("%s TTS text must not be blank.", TtsSourceEnum.DEEPGRAM.getSource());
        }
        if (StringUtils.isBlank(properties.getApiKey())) {
            throw new TtsException("%s TTS apiKey is not configured.", TtsSourceEnum.DEEPGRAM.getSource());
        }
        if (StringUtils.isBlank(model)) {
            throw new TtsException("%s TTS model is not configured for language %s.",
                    TtsSourceEnum.DEEPGRAM.getSource(), languageCode);
        }

        Request request = buildRequest(text, model);
        return executeRequest(request, model, languageCode);
    }

    private Request buildRequest(String text, String model) {
        JSONObject payload = new JSONObject();
        payload.put("model", model);
        payload.put("text", text);

        RequestBody body = RequestBody.create(JSON_MEDIA_TYPE, payload.toString());
        return new Request.Builder()
                .url(resolveBaseUrl())
                .addHeader(HEADER_AUTHORIZATION, TOKEN_PREFIX + properties.getApiKey())
                .addHeader(HEADER_CONTENT_TYPE, JSON_MEDIA_TYPE.toString())
                .post(body)
                .build();
    }

    private byte[] executeRequest(Request request, String model, String languageCode) throws TtsException {
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorMsg = extractErrorMessage(response);
                log.error("{} TTS request failed. status={}, model={}, language={}, error={}",
                        TtsSourceEnum.DEEPGRAM.getSource(), response.code(), model, languageCode, errorMsg);
                throw new TtsException("%s TTS request failed with status %s.", TtsSourceEnum.DEEPGRAM.getSource(),
                        response.code());
            }

            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                throw new TtsException("%s TTS response body is empty.", TtsSourceEnum.DEEPGRAM.getSource());
            }
            return responseBody.bytes();
        } catch (IOException e) {
            log.error("{} TTS request threw an exception.", TtsSourceEnum.DEEPGRAM.getSource(), e);
            throw new TtsException("%s TTS request failed.", e, TtsSourceEnum.DEEPGRAM.getSource());
        }
    }

    private String resolveBaseUrl() {
        return StringUtils.defaultIfBlank(properties.getBaseUrl(), "https://api.deepgram.com/v1/speak");
    }

    private String extractErrorMessage(Response response) {
        ResponseBody responseBody = response.body();
        if (responseBody == null) {
            return "<empty response body>";
        }
        try {
            return responseBody.string();
        } catch (IOException e) {
            log.warn("Failed to read Deepgram error response body.", e);
            return "<failed to read error body>";
        }
    }
}
