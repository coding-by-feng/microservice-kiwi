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
package me.fengorz.kiwi.domain.ai.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.enumeration.AiPromptModeEnum;
import me.fengorz.kiwi.common.enumeration.LanguageEnum;
import me.fengorz.kiwi.domain.ai.config.AiModeProperties;
import me.fengorz.kiwi.domain.ai.config.GrokApiProperties;
import me.fengorz.kiwi.domain.ai.model.request.ChatHttpRequest;
import me.fengorz.kiwi.domain.ai.model.request.Message;
import me.fengorz.kiwi.domain.ai.service.AiStreamingService;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.function.Consumer;

/**
 * Grok AI Streaming Service Implementation
 *
 * @author codingByFeng
 */
@Slf4j
@Service("grokStreamingService")
public class GrokStreamingServiceImpl implements AiStreamingService {

    private final GrokApiProperties grokApiProperties;
    private final AiModeProperties modeProperties;
    private final ObjectMapper objectMapper;
    private final OkHttpClient httpClient;

    public GrokStreamingServiceImpl(GrokApiProperties grokApiProperties,
                                     AiModeProperties modeProperties,
                                     ObjectMapper objectMapper,
                                     @Qualifier("aiOkHttpClient") OkHttpClient httpClient) {
        this.grokApiProperties = grokApiProperties;
        this.modeProperties = modeProperties;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    @Override
    public void streamCall(String prompt, AiPromptModeEnum promptMode, LanguageEnum targetLanguage,
                           LanguageEnum nativeLanguage, Consumer<String> onChunk, Consumer<Exception> onError,
                           Runnable onComplete) {
        try {
            String systemPrompt = buildPrompt(promptMode, targetLanguage, nativeLanguage);

            ChatHttpRequest chatRequest = new ChatHttpRequest(
                    Arrays.asList(
                            new Message("system", systemPrompt),
                            new Message("user", prompt)
                    ),
                    grokApiProperties.getModel()
            );
            chatRequest.setStream(true);

            String requestBody = objectMapper.writeValueAsString(chatRequest);

            Request request = new Request.Builder()
                    .url(grokApiProperties.getEndpoint())
                    .addHeader("Authorization", "Bearer " + grokApiProperties.getKey())
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
                    .build();

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    log.error("[STREAMING] Request failed: {}", e.getMessage(), e);
                    onError.accept(e);
                }

                @Override
                public void onResponse(Call call, Response response) {
                    if (!response.isSuccessful()) {
                        String errorMsg = "Streaming request failed with status: " + response.code();
                        log.error("[STREAMING] {}", errorMsg);
                        onError.accept(new IOException(errorMsg));
                        return;
                    }

                    try (ResponseBody body = response.body()) {
                        if (body == null) {
                            onError.accept(new IOException("Empty response body"));
                            return;
                        }

                        BufferedReader reader = new BufferedReader(new InputStreamReader(body.byteStream()));
                        String line;

                        while ((line = reader.readLine()) != null) {
                            if (line.isEmpty()) continue;
                            if (line.startsWith("data: ")) {
                                String data = line.substring(6).trim();
                                if ("[DONE]".equals(data)) {
                                    break;
                                }
                                processStreamChunk(data, onChunk, onError);
                            }
                        }

                        onComplete.run();

                    } catch (IOException e) {
                        log.error("[STREAMING] Error reading stream: {}", e.getMessage(), e);
                        onError.accept(e);
                    }
                }
            });

        } catch (Exception e) {
            log.error("[STREAMING] Error initiating stream: {}", e.getMessage(), e);
            onError.accept(e);
        }
    }

    private void processStreamChunk(String data, Consumer<String> onChunk, Consumer<Exception> onError) {
        try {
            JsonNode node = objectMapper.readTree(data);
            JsonNode choices = node.get("choices");
            if (choices != null && choices.isArray() && choices.size() > 0) {
                JsonNode delta = choices.get(0).get("delta");
                if (delta != null && delta.has("content")) {
                    String content = delta.get("content").asText();
                    if (content != null && !content.isEmpty()) {
                        onChunk.accept(content);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[STREAMING] Error parsing chunk: {}", e.getMessage());
        }
    }

    private String buildPrompt(AiPromptModeEnum promptMode, LanguageEnum targetLanguage, LanguageEnum nativeLanguage) {
        String promptTemplate = modeProperties.getMode().get(promptMode.getMode());

        if (promptTemplate == null) {
            throw new IllegalArgumentException("Prompt template not found for mode: " + promptMode);
        }

        return promptTemplate.replace("#[TL]", targetLanguage.getCode())
                .replace("#[NL]", nativeLanguage.getCode());
    }
}
