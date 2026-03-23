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
import me.fengorz.kiwi.domain.ai.config.GeminiApiProperties;
import me.fengorz.kiwi.domain.ai.config.VertexAiCredentialProvider;
import me.fengorz.kiwi.domain.ai.model.request.GeminiRequest;
import me.fengorz.kiwi.domain.ai.service.AiStreamingService;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.function.Consumer;

/**
 * Gemini AI Streaming Service Implementation
 *
 * @author codingByFeng
 */
@Slf4j
@Service("geminiStreamingService")
public class GeminiStreamingServiceImpl implements AiStreamingService {

    private final GeminiApiProperties geminiApiProperties;
    private final AiModeProperties modeProperties;
    private final ObjectMapper objectMapper;
    private final OkHttpClient httpClient;
    private final VertexAiCredentialProvider credentialProvider;

    public GeminiStreamingServiceImpl(GeminiApiProperties geminiApiProperties,
                                       AiModeProperties modeProperties,
                                       ObjectMapper objectMapper,
                                       @Qualifier("aiOkHttpClient") OkHttpClient httpClient,
                                       VertexAiCredentialProvider credentialProvider) {
        this.geminiApiProperties = geminiApiProperties;
        this.modeProperties = modeProperties;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
        this.credentialProvider = credentialProvider;
    }

    @Override
    public void streamCall(String prompt, AiPromptModeEnum promptMode, LanguageEnum targetLanguage,
                           LanguageEnum nativeLanguage, Consumer<String> onChunk, Consumer<Exception> onError,
                           Runnable onComplete) {
        try {
            String systemPrompt = buildPrompt(promptMode, targetLanguage, nativeLanguage);

            GeminiRequest geminiRequest = GeminiRequest.create(
                    systemPrompt,
                    prompt,
                    geminiApiProperties.getTemperature(),
                    geminiApiProperties.getMaxOutputTokens()
            );

            String requestBody = objectMapper.writeValueAsString(geminiRequest);
            String url = buildStreamingUrl();

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Authorization", "Bearer " + credentialProvider.getAccessToken())
                    .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
                    .build();

            log.debug("[GEMINI-STREAMING] Starting stream request to: {}", url);

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    log.error("[GEMINI-STREAMING] Request failed: {}", e.getMessage(), e);
                    onError.accept(e);
                }

                @Override
                public void onResponse(Call call, Response response) {
                    if (!response.isSuccessful()) {
                        String errorMsg = "Gemini streaming request failed with status: " + response.code();
                        try {
                            if (response.body() != null) {
                                errorMsg += " - " + response.body().string();
                            }
                        } catch (IOException ignored) {}
                        log.error("[GEMINI-STREAMING] {}", errorMsg);
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

                        // Gemini SSE streaming returns lines prefixed with "data: "
                        while ((line = reader.readLine()) != null) {
                            line = line.trim();
                            if (line.isEmpty()) continue;

                            // SSE format: lines start with "data: "
                            if (line.startsWith("data: ")) {
                                String jsonData = line.substring(6).trim(); // Remove "data: " prefix

                                // Skip [DONE] marker
                                if ("[DONE]".equals(jsonData)) {
                                    log.debug("[GEMINI-STREAMING] Received [DONE] marker");
                                    continue;
                                }

                                if (!jsonData.isEmpty() && jsonData.startsWith("{")) {
                                    log.debug("[GEMINI-STREAMING] Processing SSE chunk: {}",
                                            jsonData.length() > 200 ? jsonData.substring(0, 200) + "..." : jsonData);
                                    processStreamChunk(jsonData, onChunk, onError);
                                }
                            } else {
                                // Log unexpected line format for debugging
                                log.debug("[GEMINI-STREAMING] Non-SSE line received: {}",
                                        line.length() > 100 ? line.substring(0, 100) + "..." : line);
                            }
                        }

                        onComplete.run();

                    } catch (IOException e) {
                        log.error("[GEMINI-STREAMING] Error reading stream: {}", e.getMessage(), e);
                        onError.accept(e);
                    }
                }
            });

        } catch (Exception e) {
            log.error("[GEMINI-STREAMING] Error initiating stream: {}", e.getMessage(), e);
            onError.accept(e);
        }
    }

    private void processStreamChunk(String jsonData, Consumer<String> onChunk, Consumer<Exception> onError) {
        try {
            JsonNode node = objectMapper.readTree(jsonData);
            JsonNode candidates = node.get("candidates");

            if (candidates != null && candidates.isArray() && !candidates.isEmpty()) {
                JsonNode content = candidates.get(0).get("content");
                if (content != null) {
                    JsonNode parts = content.get("parts");
                    if (parts != null && parts.isArray() && !parts.isEmpty()) {
                        JsonNode textNode = parts.get(0).get("text");
                        if (textNode != null) {
                            String text = textNode.asText();
                            if (text != null && !text.isEmpty()) {
                                log.debug("[GEMINI-STREAMING] Extracted text chunk: {}",
                                        text.length() > 50 ? text.substring(0, 50) + "..." : text);
                                onChunk.accept(text);
                            }
                        } else {
                            log.debug("[GEMINI-STREAMING] No text node in parts: {}", parts);
                        }
                    } else {
                        log.debug("[GEMINI-STREAMING] No parts in content: {}", content);
                    }
                } else {
                    log.debug("[GEMINI-STREAMING] No content in candidate: {}", candidates.get(0));
                }
            } else {
                log.debug("[GEMINI-STREAMING] No candidates in response: {}", jsonData);
            }
        } catch (Exception e) {
            log.warn("[GEMINI-STREAMING] Error parsing chunk: {} - data: {}", e.getMessage(), jsonData);
        }
    }

    private String buildStreamingUrl() {
        return String.format("%s/%s:streamGenerateContent?alt=sse",
                geminiApiProperties.getVertexAiEndpoint(),
                geminiApiProperties.getModel());
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
