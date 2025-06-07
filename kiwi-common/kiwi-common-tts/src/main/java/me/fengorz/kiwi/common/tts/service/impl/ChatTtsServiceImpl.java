package me.fengorz.kiwi.common.tts.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.sdk.exception.tts.TtsException;
import me.fengorz.kiwi.common.tts.service.TtsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * ChatTTS Service Implementation
 *
 * @Author Kason Zhan
 */
@Slf4j
@Service("chatTtsService")
public class ChatTtsServiceImpl implements TtsService {

    @Value("${tts.chattts.api.base-url}")
    private String chatTtsBaseUrl;

    @Value("${tts.chattts.api.timeout:30}")
    private int timeoutSeconds;

    @Value("${tts.chattts.api.retry-count:3}")
    private int retryCount;

    private RestTemplate restTemplate;
    private ObjectMapper objectMapper;

    @PostConstruct
    public void init() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();

        // Configure timeout
        this.restTemplate.setRequestFactory(new org.springframework.http.client.SimpleClientHttpRequestFactory());
        ((org.springframework.http.client.SimpleClientHttpRequestFactory) this.restTemplate.getRequestFactory())
                .setConnectTimeout((int) TimeUnit.SECONDS.toMillis(timeoutSeconds));
        ((org.springframework.http.client.SimpleClientHttpRequestFactory) this.restTemplate.getRequestFactory())
                .setReadTimeout((int) TimeUnit.SECONDS.toMillis(timeoutSeconds));

        log.info("ChatTTS Service initialized with base URL: {}", chatTtsBaseUrl);
    }

    @Override
    public byte[] speechEnglish(String text) throws TtsException {
        if (text == null || text.trim().isEmpty()) {
            throw new TtsException("Text cannot be null or empty");
        }

        log.debug("Generating English speech for text: {}", text.length() > 50 ?
                text.substring(0, 50) + "..." : text);

        return generateSpeech(text);
    }

    @Override
    public byte[] speechChinese(String text) throws TtsException {
        if (text == null || text.trim().isEmpty()) {
            throw new TtsException("Text cannot be null or empty");
        }

        log.debug("Generating Chinese speech for text: {}", text.length() > 50 ?
                text.substring(0, 50) + "..." : text);

        return generateSpeech(text);
    }

    /**
     * Generate speech from text using ChatTTS API
     *
     * @param text The text to convert to speech
     * @return Audio data as byte array
     * @throws TtsException If generation fails
     */
    private byte[] generateSpeech(String text) throws TtsException {
        String url = chatTtsBaseUrl + "/tts";

        // Prepare request body
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("text", text);

        // Set headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Lists.newArrayList(MediaType.parseMediaType("audio/wav")));

        HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);

        // Retry logic
        Exception lastException = null;
        for (int attempt = 1; attempt <= retryCount; attempt++) {
            try {
                log.debug("Attempting to call ChatTTS API (attempt {}/{})", attempt, retryCount);

                ResponseEntity<byte[]> response = restTemplate.exchange(
                        url,
                        HttpMethod.POST,
                        request,
                        byte[].class
                );

                if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                    byte[] audioData = response.getBody();
                    log.debug("Successfully generated speech, audio size: {} bytes", audioData.length);
                    return audioData;
                } else {
                    throw new TtsException("ChatTTS API returned empty response or error status: " +
                            response.getStatusCode());
                }

            } catch (Exception e) {
                lastException = e;
                log.warn("ChatTTS API call failed (attempt {}/{}): {}",
                        attempt, retryCount, e.getMessage());

                if (attempt < retryCount) {
                    try {
                        // Wait before retry (exponential backoff)
                        TimeUnit.SECONDS.sleep(1);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new TtsException("Thread interrupted during retry wait", ie);
                    }
                }
            }
        }

        // All retries failed
        String errorMsg = String.format("ChatTTS API call failed after %d attempts", retryCount);
        log.error(errorMsg, lastException);
        throw new TtsException(errorMsg, lastException);
    }

    @Override
    public boolean hasValidApiKey() {
        return true;
    }


}