package me.fengorz.kason.ai.service.grok;

import lombok.extern.slf4j.Slf4j;
import me.fengorz.kason.ai.api.model.request.GrokStreamingRequest;
import me.fengorz.kason.ai.api.model.request.Message;
import me.fengorz.kason.ai.config.AiModeProperties;
import me.fengorz.kason.ai.service.AiStreamingService;
import me.fengorz.kason.common.sdk.enumeration.AiPromptModeEnum;
import me.fengorz.kason.common.sdk.enumeration.LanguageEnum;
import me.fengorz.kason.common.sdk.exception.ai.GrokAiException;
import me.fengorz.kason.common.sdk.util.json.KasonJsonUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service("grokStreamingService")
public class GrokStreamingService implements AiStreamingService {

    private final RestTemplate restTemplate;
    private final RetryTemplate retryTemplate;
    private final GrokApiProperties grokApiProperties;
    private final AiModeProperties modeProperties;

    // Pattern to extract content from SSE data
    private static final Pattern CONTENT_PATTERN = Pattern.compile("\"content\"\\s*:\\s*\"([^\"]+)\"");

    public GrokStreamingService(@Qualifier("aiRestTemplate") RestTemplate restTemplate,
                                @Qualifier("aiRetryTemplate") RetryTemplate retryTemplate,
                                GrokApiProperties grokApiProperties,
                                AiModeProperties modeProperties) {
        this.restTemplate = restTemplate;
        this.retryTemplate = retryTemplate;
        this.grokApiProperties = grokApiProperties;
        this.modeProperties = modeProperties;
    }

    @Override
    public void streamCall(String prompt, AiPromptModeEnum promptMode, LanguageEnum targetLanguage,
                           LanguageEnum nativeLanguage, Consumer<String> onChunk, Consumer<Exception> onError,
                           Runnable onComplete) {
        CompletableFuture.runAsync(() -> {
            try {
                GrokStreamingRequest grokStreamingRequest = new GrokStreamingRequest(
                        Arrays.asList(new Message("system", buildSystemPrompt(prompt, promptMode, targetLanguage, nativeLanguage)),
                                new Message("user", buildUserPrompt(prompt))),
                        grokApiProperties.getModel(),
                        true // Enable streaming
                );

                streamRequestWithRetry(grokStreamingRequest, onChunk, onError, onComplete);
            } catch (Exception e) {
                log.error("Error in streaming call with two languages: {}", e.getMessage(), e);
                onError.accept(e);
            }
        });
    }

    private static String buildUserPrompt(String prompt) {
        if (prompt.startsWith(AiPromptModeEnum.SELECTION_EXPLANATION.getTag())) {
            return prompt.split(Pattern.quote(AiPromptModeEnum.SPLITTER))[0].replaceFirst(AiPromptModeEnum.SELECTION_EXPLANATION.getTag(), "");
        }
        return prompt;
    }

    /**
     * Stream request with retry logic using RetryTemplate
     */
    private void streamRequestWithRetry(GrokStreamingRequest grokStreamingRequest, Consumer<String> onChunk,
                                        Consumer<Exception> onError, Runnable onComplete) {
        try {
            // Use RetryTemplate to wrap the streaming call
            retryTemplate.execute(context -> {
                log.info("🔄 Attempting Grok streaming call (attempt {})", context.getRetryCount() + 1);
                streamRequest(grokStreamingRequest, onChunk, onError, onComplete);
                return null;
            });
        } catch (Exception e) {
            log.error("❌ All Grok streaming retry attempts failed: {}", e.getMessage(), e);
            onError.accept(e);
        }
    }

    /**
     * Core streaming request method
     */
    private void streamRequest(GrokStreamingRequest grokStreamingRequest, Consumer<String> onChunk,
                               Consumer<Exception> onError, Runnable onComplete) {
        try {
            String requestBody = KasonJsonUtils.toJsonStr(grokStreamingRequest);
            log.debug("Sending Grok streaming request: {}", requestBody);

            // Create request callback to set headers and body
            RequestCallback requestCallback = request -> {
                request.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                request.getHeaders().set("Authorization", "Bearer " + grokApiProperties.getKey());
                request.getHeaders().set("Accept", "text/event-stream");
                try {
                    request.getBody().write(requestBody.getBytes(StandardCharsets.UTF_8));
                } catch (IOException e) {
                    throw new RuntimeException("Failed to write request body", e);
                }
            };

            // Create response extractor to handle streaming response
            ResponseExtractor<Void> responseExtractor = response -> {
                if (response.getStatusCode().is2xxSuccessful()) {
                    log.info("✅ Grok streaming connection established, processing response...");

                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(response.getBody(), StandardCharsets.UTF_8))) {

                        String line;
                        boolean hasReceivedContent = false;

                        while ((line = reader.readLine()) != null) {
                            if (line.trim().isEmpty()) {
                                continue;
                            }

                            // Parse SSE format: data: {...}
                            if (line.startsWith("data: ")) {
                                String data = line.substring(6).trim();

                                // Skip [DONE] marker
                                if ("[DONE]".equals(data)) {
                                    log.info("📋 Received [DONE] marker, ending stream");
                                    break;
                                }

                                try {
                                    // Extract content from the JSON response
                                    String content = extractContent(data);
                                    if (content != null && !content.isEmpty()) {
                                        hasReceivedContent = true;
                                        log.debug("📤 Sending chunk: '{}'", content);
                                        onChunk.accept(content);
                                    }
                                } catch (Exception e) {
                                    log.warn("⚠️ Failed to parse streaming chunk: {}", data, e);
                                    // Don't throw here, just log the warning and continue
                                }
                            }
                        }

                        // Check if we received any content at all
                        if (!hasReceivedContent) {
                            throw new GrokAiException("No content received from Grok streaming API");
                        }

                        log.info("✅ Grok streaming completed successfully");
                        onComplete.run();

                    } catch (IOException e) {
                        log.error("💥 Error reading Grok streaming response: {}", e.getMessage(), e);
                        throw new GrokAiException("Error reading streaming response: " + e.getMessage(), e);
                    }
                } else {
                    String errorMsg = "Grok streaming request failed with status: " + response.getStatusCode();
                    log.error("💥 {}", errorMsg);
                    throw new GrokAiException(errorMsg);
                }
                return null;
            };

            // Execute the streaming request
            restTemplate.execute(grokApiProperties.getEndpoint(), HttpMethod.POST, requestCallback, responseExtractor);

        } catch (Exception e) {
            log.error("💥 Error in Grok streaming request: {}", e.getMessage(), e);
            throw new GrokAiException("Streaming request failed: " + e.getMessage(), e);
        }
    }

    /**
     * Extract content from JSON data with improved error handling
     */
    private String extractContent(String jsonData) {
        try {
            log.debug("🔍 Processing JSON data: {}", jsonData);

            Matcher matcher = CONTENT_PATTERN.matcher(jsonData);
            if (matcher.find()) {
                String content = matcher.group(1);
                log.debug("✅ Extracted content: '{}'", content);
                return content;
            } else {
                log.debug("ℹ️ No content found in JSON data (might be metadata): {}", jsonData);
            }
        } catch (Exception e) {
            log.warn("⚠️ Failed to extract content from JSON: {}", jsonData, e);
        }
        return null;
    }

    @NotNull
    @SuppressWarnings("DuplicatedCode")
    private String buildSystemPrompt(String prompt, AiPromptModeEnum promptMode, LanguageEnum targetLanguage, LanguageEnum nativeLanguage) {
        String promptTemplate = modeProperties.getMode().get(promptMode.getMode());

        if (promptTemplate == null) {
            throw new GrokAiException("Prompt template not found for prompt mode: " + promptMode);
        }

        // Handle cases where languages are NONE or null
        if (LanguageEnum.NONE.equals(targetLanguage) && LanguageEnum.NONE.equals(nativeLanguage)) {
            throw new GrokAiException("Both targetLanguage and nativeLanguage cannot be NONE");
        }

        if (prompt.startsWith(AiPromptModeEnum.SELECTION_EXPLANATION.getTag())) {
            return buildSelectionExplanationPrompt(prompt, targetLanguage, promptTemplate);
        }

        // Replace placeholders with actual language names
        return promptTemplate.replace("#[TL]", targetLanguage.getCode())
                .replace("#[NL]", nativeLanguage.getCode());
    }

    @NotNull
    private static String buildSelectionExplanationPrompt(String prompt, LanguageEnum targetLanguage, String promptTemplate) {
        String[] segments = prompt.replaceFirst(Pattern.quote(AiPromptModeEnum.SELECTION_EXPLANATION.getTag()), "")
                .split(Pattern.quote(AiPromptModeEnum.SPLITTER));
        String processedPrompt = promptTemplate.replace("#[TL]", targetLanguage.getCode());
        for (int i = 0; i < segments.length; i++) {
            String segmentPlaceholder = "#[S" + i + "]";
            processedPrompt = processedPrompt.replace(segmentPlaceholder, segments[i].trim());
        }
        return processedPrompt;
    }
}