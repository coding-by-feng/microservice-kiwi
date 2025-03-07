package me.fengorz.kiwi.ai.grok.service;

import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.ai.AiChatService;
import me.fengorz.kiwi.ai.config.AiModeProperties;
import me.fengorz.kiwi.ai.grok.GrokApiProperties;
import me.fengorz.kiwi.ai.grok.model.request.ChatRequest;
import me.fengorz.kiwi.ai.grok.model.request.Message;
import me.fengorz.kiwi.ai.grok.model.response.ChatCompletionResponse;
import me.fengorz.kiwi.common.sdk.enumeration.AiPromptModeEnum;
import me.fengorz.kiwi.common.sdk.enumeration.LanguageEnum;
import me.fengorz.kiwi.common.sdk.exception.ai.GrokAiException;
import me.fengorz.kiwi.common.sdk.util.json.KiwiJsonUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;

@Slf4j
@Service
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
        String requestBody = KiwiJsonUtils.toJsonStr(chatRequest);

        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<ChatCompletionResponse> response = restTemplate.postForEntity(grokApiProperties.getEndpoint(), entity, ChatCompletionResponse.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            return Objects.requireNonNull(response.getBody()).getChoices().get(0).getMessage().getContent();  // Parse or return the response (e.g., Grok’s response JSON)
        } else {
            log.error("Grok API call failed: status code: {}; body: {}", response.getStatusCode(), response.getBody());
            throw new GrokAiException("Grok API call failed: " + response.getStatusCode());
        }
    }

    @NotNull
    private String buildPrompt(AiPromptModeEnum promptMode, LanguageEnum language) {
        String lang = language.getName();
        if (AiPromptModeEnum.VOCABULARY_EXPLANATION.equals(promptMode)) {
            return String.format(modeProperties.getMode().get(promptMode.getMode()), lang, lang, lang);
        }
        return String.format(modeProperties.getMode().get(promptMode.getMode()), lang);
    }

}

