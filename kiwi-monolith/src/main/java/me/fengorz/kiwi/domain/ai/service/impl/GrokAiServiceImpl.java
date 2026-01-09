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
import me.fengorz.kiwi.domain.ai.config.GrokApiProperties;
import me.fengorz.kiwi.domain.ai.model.BatchResult;
import me.fengorz.kiwi.domain.ai.model.request.ChatHttpRequest;
import me.fengorz.kiwi.domain.ai.model.request.Message;
import me.fengorz.kiwi.domain.ai.model.response.ChatCompletionResponse;
import me.fengorz.kiwi.domain.ai.service.AiChatService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.*;

/**
 * Grok AI Service Implementation
 *
 * @author codingByFeng
 */
@Slf4j
@Service("grokAiService")
public class GrokAiServiceImpl implements AiChatService {

    private final RestTemplate restTemplate;
    private final RetryTemplate retryTemplate;
    private final GrokApiProperties grokApiProperties;
    private final AiModeProperties modeProperties;
    private final ObjectMapper objectMapper;

    public GrokAiServiceImpl(@Qualifier("aiRestTemplate") RestTemplate restTemplate,
                             @Qualifier("aiRetryTemplate") RetryTemplate retryTemplate,
                             GrokApiProperties grokApiProperties,
                             AiModeProperties modeProperties,
                             ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.retryTemplate = retryTemplate;
        this.grokApiProperties = grokApiProperties;
        this.modeProperties = modeProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    public String call(String prompt, AiPromptModeEnum promptMode, LanguageEnum language) {
        HttpHeaders headers = buildHeaders();
        ChatHttpRequest chatHttpRequest = new ChatHttpRequest(
                Arrays.asList(
                        new Message("system", buildPrompt(promptMode, language, language)),
                        new Message("user", prompt)
                ),
                grokApiProperties.getModel()
        );
        return call(headers, chatHttpRequest);
    }

    @Override
    public String call(String prompt, AiPromptModeEnum promptMode, LanguageEnum targetLanguage, LanguageEnum nativeLanguage) {
        HttpHeaders headers = buildHeaders();
        ChatHttpRequest chatHttpRequest = new ChatHttpRequest(
                Arrays.asList(
                        new Message("system", buildPrompt(promptMode, targetLanguage, nativeLanguage)),
                        new Message("user", prompt)
                ),
                grokApiProperties.getModel()
        );
        return call(headers, chatHttpRequest);
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.put("Authorization", Collections.singletonList("Bearer " + grokApiProperties.getKey()));
        return headers;
    }

    private String call(HttpHeaders headers, ChatHttpRequest chatHttpRequest) {
        try {
            String requestBody = objectMapper.writeValueAsString(chatHttpRequest);
            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<ChatCompletionResponse> response = retryTemplate.execute(context -> {
                log.debug("Attempting Grok API call (attempt {})", context.getRetryCount() + 1);
                return restTemplate.postForEntity(grokApiProperties.getEndpoint(), entity, ChatCompletionResponse.class);
            });

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody().getChoices().get(0).getMessage().getContent();
            } else {
                log.error("Grok API call failed: status code: {}; body: {}", response.getStatusCode(), response.getBody());
                throw new ServiceException("Grok API call failed: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error calling Grok API", e);
            throw new ServiceException("Error calling Grok API: " + e.getMessage(), e);
        }
    }

    @Override
    public String batchCall(List<String> prompts, AiPromptModeEnum promptMode, LanguageEnum language) {
        int batchSize = grokApiProperties.getThreadPromptsLineSize();
        int threadPoolSize = grokApiProperties.getThreadPoolSize() != null ?
                grokApiProperties.getThreadPoolSize() : Runtime.getRuntime().availableProcessors();
        int totalBatches = (int) Math.ceil((double) prompts.size() / batchSize);

        String[] results = new String[totalBatches];
        ExecutorService executorService = Executors.newFixedThreadPool(threadPoolSize);
        List<Future<BatchResult>> futures = new ArrayList<>();

        for (int i = 0; i < prompts.size(); i += batchSize) {
            final int batchIndex = i / batchSize;
            final int startIndex = i;
            final int endIndex = Math.min(i + batchSize, prompts.size());

            futures.add(executorService.submit(() -> {
                try {
                    List<String> batchPrompts = prompts.subList(startIndex, endIndex);
                    return new BatchResult(batchIndex, processBatch(batchPrompts, promptMode, language));
                } catch (Exception e) {
                    log.error("Error processing batch {}: {}", batchIndex, e.getMessage(), e);
                    throw new ServiceException("Error processing batch " + batchIndex + ": " + e.getMessage(), e);
                }
            }));
        }

        for (Future<BatchResult> future : futures) {
            try {
                BatchResult result = future.get();
                results[result.getBatchIndex()] = result.getContent();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ServiceException("Batch processing was interrupted", e);
            } catch (ExecutionException e) {
                throw new ServiceException("Error executing batch: " + e.getCause().getMessage(), e.getCause());
            }
        }

        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(grokApiProperties.getThreadTimeoutSecs(), TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }

        StringBuilder finalResult = new StringBuilder();
        for (String result : results) {
            if (result != null) {
                finalResult.append(result).append("\n\n");
            }
        }
        return finalResult.toString().trim();
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

    private String processBatch(List<String> batchPrompts, AiPromptModeEnum promptMode, LanguageEnum language) {
        HttpHeaders headers = buildHeaders();

        StringBuilder batchContent = new StringBuilder();
        for (String prompt : batchPrompts) {
            batchContent.append(prompt).append("\n\n");
        }

        ChatHttpRequest chatHttpRequest = new ChatHttpRequest(
                Arrays.asList(
                        new Message("system", buildPrompt(promptMode, language, language)),
                        new Message("user", batchContent.toString())
                ),
                grokApiProperties.getModel()
        );

        try {
            String requestBody = objectMapper.writeValueAsString(chatHttpRequest);
            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

            log.debug("Calling Grok API for batch with {} prompts", batchPrompts.size());
            ResponseEntity<ChatCompletionResponse> response = restTemplate.postForEntity(
                    grokApiProperties.getEndpoint(), entity, ChatCompletionResponse.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody().getChoices().get(0).getMessage().getContent();
            } else {
                log.error("Grok API batch call failed: status code: {}; body: {}", response.getStatusCode(), response.getBody());
                throw new ServiceException("Grok API batch call failed: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error processing batch", e);
            throw new ServiceException("Error processing batch: " + e.getMessage(), e);
        }
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
