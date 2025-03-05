package me.fengorz.kiwi.ai.api.config.grok.service;

import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.ai.api.config.grok.model.ChatRequest;
import me.fengorz.kiwi.ai.api.config.grok.model.GrokApiModel;
import me.fengorz.kiwi.ai.api.config.grok.model.Message;
import me.fengorz.kiwi.common.sdk.exception.ai.GrokAiException;
import me.fengorz.kiwi.common.sdk.util.json.KiwiJsonUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Collections;

@Slf4j
@Service
public class GrokApiService {

    private final RestTemplate restTemplate;
    private final GrokApiModel grokApiModel;

    public GrokApiService(@Qualifier("aiRestTemplate") RestTemplate restTemplate, GrokApiModel grokApiModel) {
        this.restTemplate = restTemplate;
        this.grokApiModel = grokApiModel;
    }


    public String call(String prompt) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.put("Authorization", Collections.singletonList("Bearer " + grokApiModel.getGrokApiKey()));

        ChatRequest chatRequest = new ChatRequest(
                Arrays.asList(new Message("system", "You're an assistant"),
                        new Message("user", prompt)),
                "grok-2-latest");

        // Hypothetical request body (similar to OpenAI’s format)
        String requestBody = KiwiJsonUtils.toJsonStr(chatRequest);

        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(grokApiModel.getGrokApiEndpoint(), entity, String.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            return response.getBody();  // Parse or return the response (e.g., Grok’s response JSON)
        } else {
            log.error("Grok API call failed: status code: {}; body: {}", response.getStatusCode(), response.getBody());
            throw new GrokAiException("Grok API call failed: " + response.getStatusCode());
        }
    }

}

