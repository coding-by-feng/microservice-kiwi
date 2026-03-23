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

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.enumeration.AiPromptModeEnum;
import me.fengorz.kiwi.common.enumeration.LanguageEnum;
import me.fengorz.kiwi.common.exception.ServiceException;
import me.fengorz.kiwi.domain.ai.config.AiModeProperties;
import me.fengorz.kiwi.domain.ai.config.GeminiApiProperties;
import me.fengorz.kiwi.domain.ai.config.VertexAiCredentialProvider;
import me.fengorz.kiwi.domain.ai.model.BatchResult;
import me.fengorz.kiwi.domain.ai.model.request.GeminiRequest;
import me.fengorz.kiwi.domain.ai.model.response.GeminiResponse;
import me.fengorz.kiwi.domain.ai.service.AiChatService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Gemini AI Service Implementation
 *
 * @author codingByFeng
 */
@Slf4j
@Service("geminiAiService")
public class GeminiAiServiceImpl implements AiChatService {

    private final RestTemplate restTemplate;
    private final RetryTemplate retryTemplate;
    private final Executor batchExecutor;
    private final GeminiApiProperties geminiApiProperties;
    private final AiModeProperties modeProperties;
    private final ObjectMapper objectMapper;
    private final VertexAiCredentialProvider credentialProvider;

    public GeminiAiServiceImpl(@Qualifier("aiRestTemplate") RestTemplate restTemplate,
                               @Qualifier("aiRetryTemplate") RetryTemplate retryTemplate,
                               @Qualifier("aiBatchExecutor") Executor batchExecutor,
                               GeminiApiProperties geminiApiProperties,
                               AiModeProperties modeProperties,
                               ObjectMapper objectMapper,
                               VertexAiCredentialProvider credentialProvider) {
        this.restTemplate = restTemplate;
        this.retryTemplate = retryTemplate;
        this.batchExecutor = batchExecutor;
        this.geminiApiProperties = geminiApiProperties;
        this.modeProperties = modeProperties;
        this.objectMapper = objectMapper;
        this.credentialProvider = credentialProvider;
    }

    @Override
    public String call(String prompt, AiPromptModeEnum promptMode, LanguageEnum language) {
        return call(prompt, promptMode, language, language);
    }

    @Override
    public String call(String prompt, AiPromptModeEnum promptMode, LanguageEnum targetLanguage, LanguageEnum nativeLanguage) {
        String systemPrompt = buildPrompt(promptMode, targetLanguage, nativeLanguage);
        GeminiRequest request = GeminiRequest.create(
                systemPrompt,
                prompt,
                geminiApiProperties.getTemperature(),
                geminiApiProperties.getMaxOutputTokens()
        );
        return callApi(request);
    }

    private String callApi(GeminiRequest geminiRequest) {
        try {
            String requestBody = objectMapper.writeValueAsString(geminiRequest);
            HttpHeaders headers = buildHeaders();
            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

            String url = buildApiUrl(false);

            ResponseEntity<GeminiResponse> response = retryTemplate.execute(context -> {
                log.debug("Attempting Gemini API call (attempt {})", context.getRetryCount() + 1);
                return restTemplate.postForEntity(url, entity, GeminiResponse.class);
            });

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String content = response.getBody().getTextContent();
                if (content == null) {
                    log.error("Gemini API returned empty content");
                    throw new ServiceException("Gemini API returned empty content");
                }
                return content;
            } else {
                log.error("Gemini API call failed: status code: {}; body: {}", response.getStatusCode(), response.getBody());
                throw new ServiceException("Gemini API call failed: " + response.getStatusCode());
            }
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error calling Gemini API", e);
            throw new ServiceException("Error calling Gemini API: " + e.getMessage(), e);
        }
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(credentialProvider.getAccessToken());
        return headers;
    }

    private String buildApiUrl(boolean streaming) {
        String action = streaming ? "streamGenerateContent" : "generateContent";
        return String.format("%s/%s:%s",
                geminiApiProperties.getVertexAiEndpoint(),
                geminiApiProperties.getModel(),
                action);
    }

    @Override
    public String batchCall(List<String> prompts, AiPromptModeEnum promptMode, LanguageEnum language) {
        int batchSize = geminiApiProperties.getThreadPromptsLineSize();
        int totalBatches = (int) Math.ceil((double) prompts.size() / batchSize);

        log.info("[GEMINI] Starting batch call: {} prompts, {} batches, batchSize={}", prompts.size(), totalBatches, batchSize);
        long startTime = System.currentTimeMillis();

        List<CompletableFuture<BatchResult>> futures = new ArrayList<>();

        for (int i = 0; i < prompts.size(); i += batchSize) {
            final int batchIndex = i / batchSize;
            final int startIndex = i;
            final int endIndex = Math.min(i + batchSize, prompts.size());

            CompletableFuture<BatchResult> future = CompletableFuture.supplyAsync(() -> {
                try {
                    List<String> batchPrompts = prompts.subList(startIndex, endIndex);
                    return new BatchResult(batchIndex, processBatch(batchPrompts, promptMode, language));
                } catch (Exception e) {
                    log.error("[GEMINI] Error processing batch {}: {}", batchIndex, e.getMessage(), e);
                    throw new ServiceException("Error processing batch " + batchIndex + ": " + e.getMessage(), e);
                }
            }, batchExecutor);

            futures.add(future);
        }

        String[] results = new String[totalBatches];
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        for (CompletableFuture<BatchResult> future : futures) {
            try {
                BatchResult result = future.get();
                results[result.getBatchIndex()] = result.getContent();
            } catch (Exception e) {
                log.error("[GEMINI] Error getting batch result: {}", e.getMessage(), e);
                throw new ServiceException("Error executing batch: " + e.getMessage(), e);
            }
        }

        StringBuilder finalResult = new StringBuilder();
        for (String result : results) {
            if (result != null) {
                finalResult.append(result).append("\n\n");
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info("[GEMINI] Batch call completed: {} batches in {}ms", totalBatches, duration);

        return finalResult.toString().trim();
    }

    private String processBatch(List<String> batchPrompts, AiPromptModeEnum promptMode, LanguageEnum language) {
        StringBuilder batchContent = new StringBuilder();
        for (String prompt : batchPrompts) {
            batchContent.append(prompt).append("\n\n");
        }

        String systemPrompt = buildPrompt(promptMode, language, language);
        GeminiRequest request = GeminiRequest.create(
                systemPrompt,
                batchContent.toString(),
                geminiApiProperties.getTemperature(),
                geminiApiProperties.getMaxOutputTokens()
        );

        try {
            String requestBody = objectMapper.writeValueAsString(request);
            HttpHeaders headers = buildHeaders();
            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

            String url = buildApiUrl(false);

            log.debug("[GEMINI] Calling API for batch with {} prompts", batchPrompts.size());
            ResponseEntity<GeminiResponse> response = restTemplate.postForEntity(url, entity, GeminiResponse.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody().getTextContent();
            } else {
                log.error("[GEMINI] API batch call failed: status code: {}; body: {}", response.getStatusCode(), response.getBody());
                throw new ServiceException("Gemini API batch call failed: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("[GEMINI] Error processing batch", e);
            throw new ServiceException("Error processing batch: " + e.getMessage(), e);
        }
    }

    @Override
    public String batchCallForYtbAndCache(String ytbUrl, List<String> prompt, AiPromptModeEnum promptMode, LanguageEnum language) {
        return batchCall(prompt, promptMode, language);
    }

    @Override
    public void cleanBatchCallForYtbAndCache(String ytbUrl, AiPromptModeEnum promptMode, LanguageEnum language) {
        // Cache eviction implementation - no-op for now
    }

    @Override
    public String callForYtbAndCache(String ytbUrl, String prompt, AiPromptModeEnum promptMode, LanguageEnum language) {
        return call(prompt, promptMode, language);
    }

    @Override
    public void cleanCallForYtbAndCache(String ytbUrl, AiPromptModeEnum promptMode, LanguageEnum language) {
        // Cache eviction implementation - no-op for now
    }

    private String buildPrompt(AiPromptModeEnum promptMode, LanguageEnum targetLanguage, LanguageEnum nativeLanguage) {
        String promptTemplate = modeProperties.getMode().get(promptMode.getMode());

        if (promptTemplate == null) {
            throw new ServiceException("Prompt template not found for prompt mode: " + promptMode);
        }

        if (LanguageEnum.NONE.equals(targetLanguage) && LanguageEnum.NONE.equals(nativeLanguage)) {
            throw new ServiceException("Both targetLanguage and nativeLanguage cannot be NONE");
        }

        return promptTemplate.replace("#[TL]", targetLanguage.getCode())
                .replace("#[NL]", nativeLanguage.getCode());
    }
}
