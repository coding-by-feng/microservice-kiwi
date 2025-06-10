package me.fengorz.kiwi.ai.service.grok;

import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.ai.api.model.request.GrokStreamingRequest;
import me.fengorz.kiwi.ai.api.model.request.Message;
import me.fengorz.kiwi.ai.config.AiModeProperties;
import me.fengorz.kiwi.ai.service.AiStreamingService;
import me.fengorz.kiwi.common.sdk.enumeration.AiPromptModeEnum;
import me.fengorz.kiwi.common.sdk.enumeration.LanguageEnum;
import me.fengorz.kiwi.common.sdk.exception.ai.GrokAiException;
import me.fengorz.kiwi.common.sdk.util.json.KiwiJsonUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
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
    private final GrokApiProperties grokApiProperties;
    private final AiModeProperties modeProperties;

    // Pattern to extract content from SSE data
    private static final Pattern CONTENT_PATTERN = Pattern.compile("\"content\"\\s*:\\s*\"([^\"]+)\"");

    public GrokStreamingService(@Qualifier("aiRestTemplate") RestTemplate restTemplate,
                                GrokApiProperties grokApiProperties,
                                AiModeProperties modeProperties) {
        this.restTemplate = restTemplate;
        this.grokApiProperties = grokApiProperties;
        this.modeProperties = modeProperties;
    }

    @Override
    public void streamCall(String prompt, AiPromptModeEnum promptMode, LanguageEnum language,
                           Consumer<String> onChunk, Consumer<Exception> onError, Runnable onComplete) {
        CompletableFuture.runAsync(() -> {
            try {
                GrokStreamingRequest grokStreamingRequest = new GrokStreamingRequest(
                        Arrays.asList(new Message("system", buildPrompt(promptMode, language)),
                                new Message("user", prompt)),
                        grokApiProperties.getModel(),
                        true // Enable streaming
                );

                streamRequest(grokStreamingRequest, onChunk, onError, onComplete);
            } catch (Exception e) {
                log.error("Error in streaming call: {}", e.getMessage(), e);
                onError.accept(e);
            }
        });
    }

    @Override
    public void streamCall(String prompt, AiPromptModeEnum promptMode, LanguageEnum targetLanguage,
                           LanguageEnum nativeLanguage, Consumer<String> onChunk, Consumer<Exception> onError,
                           Runnable onComplete) {
        CompletableFuture.runAsync(() -> {
            try {
                GrokStreamingRequest grokStreamingRequest = new GrokStreamingRequest(
                        Arrays.asList(new Message("system", buildPrompt(promptMode, targetLanguage, nativeLanguage)),
                                new Message("user", prompt)),
                        grokApiProperties.getModel(),
                        true // Enable streaming
                );

                streamRequest(grokStreamingRequest, onChunk, onError, onComplete);
            } catch (Exception e) {
                log.error("Error in streaming call with two languages: {}", e.getMessage(), e);
                onError.accept(e);
            }
        });
    }

    // Also add this method to log the full streaming response for debugging
    private void streamRequest(GrokStreamingRequest grokStreamingRequest, Consumer<String> onChunk,
                               Consumer<Exception> onError, Runnable onComplete) {
        try {
            String requestBody = KiwiJsonUtils.toJsonStr(grokStreamingRequest);
            log.info("Sending Grok request: {}", requestBody);

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
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(response.getBody(), StandardCharsets.UTF_8))) {

                        String line;
                        int lineNumber = 0;
                        while ((line = reader.readLine()) != null) {
                            lineNumber++;
                            log.debug("Received line {}: {}", lineNumber, line);

                            if (line.trim().isEmpty()) {
                                continue;
                            }

                            // Parse SSE format: data: {...}
                            if (line.startsWith("data: ")) {
                                String data = line.substring(6).trim();
                                log.debug("Processing SSE data: {}", data);

                                // Skip [DONE] marker
                                if ("[DONE]".equals(data)) {
                                    log.info("Received [DONE] marker, ending stream");
                                    break;
                                }

                                try {
                                    // Extract content from the JSON response
                                    String content = extractContent(data);
                                    if (content != null && !content.isEmpty()) {
                                        log.debug("Calling onChunk with content: '{}'", content);
                                        onChunk.accept(content);
                                    } else {
                                        log.warn("Extracted content is null or empty for data: {}", data);
                                    }
                                } catch (Exception e) {
                                    log.warn("Failed to parse streaming chunk: {}", data, e);
                                }
                            } else {
                                log.debug("Received non-data line: {}", line);
                            }
                        }

                        log.info("Stream completed, calling onComplete");
                        onComplete.run();

                    } catch (IOException e) {
                        log.error("Error reading streaming response: {}", e.getMessage(), e);
                        onError.accept(e);
                    }
                } else {
                    throw new GrokAiException("Streaming request failed with status: " + response.getStatusCode());
                }
                return null;
            };

            // Execute the streaming request
            restTemplate.execute(grokApiProperties.getEndpoint(), HttpMethod.POST, requestCallback, responseExtractor);

        } catch (Exception e) {
            log.error("Error in streaming request: {}", e.getMessage(), e);
            onError.accept(e);
        }
    }

    private String extractContent(String jsonData) {
        try {
            log.info("🔍 DEBUG: Raw JSON data: {}", jsonData);

            Matcher matcher = CONTENT_PATTERN.matcher(jsonData);
            if (matcher.find()) {
                String content = matcher.group(1);
                log.info("🔍 DEBUG: Extracted content: '{}'", content);
                return content;
            } else {
                log.warn("🔍 DEBUG: No content match found in: {}", jsonData);
            }
        } catch (Exception e) {
            log.error("🔍 DEBUG: Extract failed", e);
        }
        return null;
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