package me.fengorz.kiwi.ai.openai.service;

import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.ai.AiAudioService;
import me.fengorz.kiwi.ai.openai.OpenaiApiProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service("openaiAudioService")
public class OpenaiAudioService implements AiAudioService {

    private final RestTemplate restTemplate;
    private final OpenaiApiProperties openaiApiProperties;
    private final Map<String, StringBuilder> transcriptionSessions = new ConcurrentHashMap<>();

    public OpenaiAudioService(@Qualifier("aiRestTemplate") RestTemplate restTemplate,
                              OpenaiApiProperties openaiApiProperties) {
        this.restTemplate = restTemplate;
        this.openaiApiProperties = openaiApiProperties;
    }

    @Override
    public String transcribeAudio(byte[] audioBytes) {

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + openaiApiProperties.getKey());
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(audioBytes) {
            @Override
            public String getFilename() {
                return "audio.mp3";
            }
        });
        body.add("model", "whisper-1");
        body.add("language", "en");

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.exchange(openaiApiProperties.getSttEndpoint(), HttpMethod.POST, requestEntity, String.class);
        return response.getBody();
    }

}