package me.fengorz.kiwi.ai.service.grok;

import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.ai.api.model.request.ChatRequest;
import me.fengorz.kiwi.ai.api.model.request.Message;
import me.fengorz.kiwi.ai.api.model.response.ChatCompletionResponse;
import me.fengorz.kiwi.ai.config.AiModeProperties;
import me.fengorz.kiwi.ai.model.BatchResult;
import me.fengorz.kiwi.ai.service.AiChatService;
import me.fengorz.kiwi.ai.util.AiConstants;
import me.fengorz.kiwi.common.sdk.annotation.cache.KiwiCacheKey;
import me.fengorz.kiwi.common.sdk.annotation.cache.KiwiCacheKeyPrefix;
import me.fengorz.kiwi.common.sdk.constant.CacheConstants;
import me.fengorz.kiwi.common.sdk.enumeration.AiPromptModeEnum;
import me.fengorz.kiwi.common.sdk.enumeration.LanguageEnum;
import me.fengorz.kiwi.common.sdk.exception.ai.GrokAiException;
import me.fengorz.kiwi.common.sdk.util.json.KiwiJsonUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.*;

@Slf4j
@Service("grokAiService")
@KiwiCacheKeyPrefix(AiConstants.CACHE_KEY_PREFIX_GROK.CLASS)
public class GrokAiService implements AiChatService {

    private final RestTemplate restTemplate;
    private final GrokApiProperties grokApiProperties;
    private final AiModeProperties modeProperties;

    public GrokAiService(@Qualifier("aiRestTemplate") RestTemplate restTemplate,
                         GrokApiProperties grokApiProperties, AiModeProperties modeProperties) {
        this.restTemplate = restTemplate;
        this.grokApiProperties = grokApiProperties;
        this.modeProperties = modeProperties;
    }

    @Override
    public String call(String prompt, AiPromptModeEnum promptMode, LanguageEnum language) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.put("Authorization", Collections.singletonList("Bearer " + grokApiProperties.getKey()));

        ChatRequest chatRequest = new ChatRequest(
                Arrays.asList(new Message("system", buildPrompt(promptMode, language)),
                        new Message("user", prompt)), grokApiProperties.getModel());

        // Hypothetical request body (similar to OpenAI’s format)
        return call(headers, chatRequest);
    }

    @Override
    public String call(String prompt, AiPromptModeEnum promptMode, LanguageEnum targetLanguage, LanguageEnum nativeLanguage) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.put("Authorization", Collections.singletonList("Bearer " + grokApiProperties.getKey()));

        ChatRequest chatRequest = new ChatRequest(
                Arrays.asList(new Message("system", buildPrompt(promptMode, targetLanguage, nativeLanguage)),
                        new Message("user", prompt)), grokApiProperties.getModel());

        // Hypothetical request body (similar to OpenAI's format)
        return call(headers, chatRequest);
    }

    private String call(HttpHeaders headers, ChatRequest chatRequest) {
        String requestBody = KiwiJsonUtils.toJsonStr(chatRequest);
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<ChatCompletionResponse> response = restTemplate.postForEntity(grokApiProperties.getEndpoint(), entity, ChatCompletionResponse.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            return Objects.requireNonNull(response.getBody()).getChoices().get(0).getMessage().getContent();
        } else {
            log.error("Grok API call failed: status code: {}; body: {}", response.getStatusCode(), response.getBody());
            throw new GrokAiException("Grok API call failed: " + response.getStatusCode());
        }
    }

    @Override
    public String batchCall(List<String> prompts, AiPromptModeEnum promptMode, LanguageEnum language) {
        // Get batch size from properties (default to 50 if not specified)
        int batchSize = grokApiProperties.getThreadPromptsLineSize();

        // Get thread pool size from properties (default to available processors)
        int threadPoolSize = grokApiProperties.getThreadPoolSize() != null ?
                grokApiProperties.getThreadPoolSize() : Runtime.getRuntime().availableProcessors();

        // Calculate total number of batches
        int totalBatches = (int) Math.ceil((double) prompts.size() / batchSize);

        // Create array to store results in order
        String[] results = new String[totalBatches];

        // Create thread pool
        ExecutorService executorService = Executors.newFixedThreadPool(threadPoolSize);

        // List to keep track of futures
        List<Future<BatchResult>> futures = new ArrayList<>();

        // Submit batch tasks
        for (int i = 0; i < prompts.size(); i += batchSize) {
            final int batchIndex = i / batchSize;
            final int startIndex = i;
            final int endIndex = Math.min(i + batchSize, prompts.size());

            futures.add(executorService.submit(() -> {
                try {
                    // Get current batch of prompts
                    List<String> batchPrompts = prompts.subList(startIndex, endIndex);

                    // Process batch and return result with index for ordering
                    return new BatchResult(batchIndex, processBatch(batchPrompts, promptMode, language));
                } catch (Exception e) {
                    log.error("Error processing batch {}: {}", batchIndex, e.getMessage(), e);
                    throw new GrokAiException("Error processing batch " + batchIndex + ": " + e.getMessage(), e);
                }
            }));
        }

        // Collect results while maintaining order
        for (Future<BatchResult> future : futures) {
            try {
                BatchResult result = future.get();
                results[result.getBatchIndex()] = result.getContent();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new GrokAiException("Batch processing was interrupted", e);
            } catch (ExecutionException e) {
                throw new GrokAiException("Error executing batch: " + e.getCause().getMessage(), e.getCause());
            }
        }

        // Shutdown executor
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(this.grokApiProperties.getThreadTimeoutSecs(), TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }

        // Combine results in order
        StringBuilder finalResult = new StringBuilder();
        for (String result : results) {
            if (result != null) {
                finalResult.append(result).append("\n\n");
            }
        }

        return finalResult.toString().trim();
    }

    @Override
    @KiwiCacheKeyPrefix(AiConstants.CACHE_KEY_PREFIX_GROK.SUBTITLE_TRANSLATION)
    @Cacheable(cacheNames = AiConstants.CACHE_NAMES, keyGenerator = CacheConstants.CACHE_KEY_GENERATOR_BEAN,
            unless = "#result == null")
    public String batchCallForYtbAndCache(@KiwiCacheKey(1) String ytbUrl, List<String> prompt, @KiwiCacheKey(2) AiPromptModeEnum promptMode, @KiwiCacheKey(3) LanguageEnum language) {
        return batchCall(prompt, promptMode, language);
    }

    @Override
    @KiwiCacheKeyPrefix(AiConstants.CACHE_KEY_PREFIX_GROK.SUBTITLE_TRANSLATION)
    @CacheEvict(cacheNames = AiConstants.CACHE_NAMES, keyGenerator = CacheConstants.CACHE_KEY_GENERATOR_BEAN)
    public void cleanBatchCallForYtbAndCache(@KiwiCacheKey(1) String ytbUrl, @KiwiCacheKey(2) AiPromptModeEnum promptMode, @KiwiCacheKey(3) LanguageEnum language) {

    }

    @Override
    @KiwiCacheKeyPrefix(AiConstants.CACHE_KEY_PREFIX_GROK.SUBTITLE_RETOUCH)
    @Cacheable(cacheNames = AiConstants.CACHE_NAMES, keyGenerator = CacheConstants.CACHE_KEY_GENERATOR_BEAN,
            unless = "#result == null")
    public String callForYtbAndCache(@KiwiCacheKey(1) String ytbUrl, String prompt, @KiwiCacheKey(2) AiPromptModeEnum promptMode, @KiwiCacheKey(3) LanguageEnum language) {
        return call(prompt, promptMode, language);
    }

    @Override
    @KiwiCacheKeyPrefix(AiConstants.CACHE_KEY_PREFIX_GROK.SUBTITLE_RETOUCH)
    @CacheEvict(cacheNames = AiConstants.CACHE_NAMES, keyGenerator = CacheConstants.CACHE_KEY_GENERATOR_BEAN)
    public void cleanCallForYtbAndCache(@KiwiCacheKey(1) String ytbUrl, @KiwiCacheKey(2) AiPromptModeEnum promptMode, @KiwiCacheKey(3) LanguageEnum language) {
    }

    /**
     * Process a single batch of prompts
     *
     * @param batchPrompts List of prompts to process in this batch
     * @param promptMode   The prompt mode to use
     * @param language     The language to use
     * @return The processed result
     */
    private String processBatch(List<String> batchPrompts, AiPromptModeEnum promptMode, LanguageEnum language) {
        // Create thread-local headers for safety
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.put("Authorization", Collections.singletonList("Bearer " + grokApiProperties.getKey()));

        // Process each prompt in the batch and collect into a single string
        StringBuilder batchContent = new StringBuilder();
        for (String prompt : batchPrompts) {
            batchContent.append(prompt).append("\n\n");
        }

        // Build request with system prompt and combined user prompt
        ChatRequest chatRequest = new ChatRequest(
                Arrays.asList(new Message("system", buildPrompt(promptMode, language)),
                        new Message("user", batchContent.toString())), grokApiProperties.getModel());

        // Convert request to JSON
        String requestBody = KiwiJsonUtils.toJsonStr(chatRequest);

        // Create HTTP entity with headers and body
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        // Make API call
        log.debug("Calling Grok API for batch with {} prompts", batchPrompts.size());
        ResponseEntity<ChatCompletionResponse> response = restTemplate.postForEntity(
                grokApiProperties.getEndpoint(), entity, ChatCompletionResponse.class);

        // Process response
        if (response.getStatusCode().is2xxSuccessful()) {
            return Objects.requireNonNull(response.getBody()).getChoices().get(0).getMessage().getContent();
        } else {
            log.error("Grok API batch call failed: status code: {}; body: {}", response.getStatusCode(), response.getBody());
            throw new GrokAiException("Grok API batch call failed: " + response.getStatusCode());
        }
    }

    @NotNull
    private String buildPrompt(AiPromptModeEnum promptMode, LanguageEnum language) {
        if (promptMode.getLanguageWildcardCounts() == 0 || LanguageEnum.NONE.equals(language)) {
            return modeProperties.getMode().get(promptMode.getMode());
        }
        Object[] languageWildcards = new Object[promptMode.getLanguageWildcardCounts()];
        for (int i = 0; i < promptMode.getLanguageWildcardCounts(); i++) {
            languageWildcards[i] = language.getCode();
        }
        return String.format(modeProperties.getMode().get(promptMode.getMode()), languageWildcards);
    }

    @NotNull
    private String buildPrompt(AiPromptModeEnum promptMode, LanguageEnum targetLanguage, LanguageEnum nativeLanguage) {
        String promptTemplate = modeProperties.getMode().get(promptMode.getMode());

        if (promptTemplate == null) {
            throw new GrokAiException("Prompt template not found for prompt mode: " + promptMode);
        }

        // Handle cases where languages are NONE or null
        if (LanguageEnum.NONE.equals(targetLanguage) && LanguageEnum.NONE.equals(nativeLanguage)) {
            throw new GrokAiException("Both targetLanguage and nativeLanguage cannot be NONE");
        }

        // Replace placeholders with actual language names
        return promptTemplate.replace("#[TL]", targetLanguage.getCode())
                .replace("#[NL]", nativeLanguage.getCode());
    }

}

