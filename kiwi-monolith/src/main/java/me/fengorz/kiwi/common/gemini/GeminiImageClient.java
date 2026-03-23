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
package me.fengorz.kiwi.common.gemini;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.exception.ServiceException;
import me.fengorz.kiwi.domain.ai.config.VertexAiCredentialProvider;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

/**
 * Gemini Image Generation Client
 * Uses Google Gemini Imagen API to generate images
 *
 * @author codingByFeng
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "kiwi.gemini.enabled", havingValue = "true", matchIfMissing = false)
public class GeminiImageClient {

    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json");

    private final OkHttpClient httpClient;
    private final GeminiImageProperties properties;
    private final ObjectMapper objectMapper;
    private final VertexAiCredentialProvider credentialProvider;

    public GeminiImageClient(GeminiImageProperties properties, VertexAiCredentialProvider credentialProvider) {
        this.properties = properties;
        this.credentialProvider = credentialProvider;
        this.objectMapper = new ObjectMapper();
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();

        log.info("Gemini Image Client initialized with Vertex AI, model: {}", properties.getImageModel());
    }

    /**
     * Generate an image from a text prompt
     *
     * @param prompt the text prompt describing the image
     * @return byte array of the generated image (PNG format)
     */
    public byte[] generateImage(String prompt) {
        if (StringUtils.isBlank(prompt)) {
            throw new ServiceException("Image prompt cannot be empty");
        }

        String url = properties.getVertexAiEndpoint();

        String jsonPayload = buildJsonPayload(prompt);

        RequestBody body = RequestBody.create(JSON_MEDIA_TYPE, jsonPayload);
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + credentialProvider.getAccessToken())
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "No error body";
                log.error("Gemini image generation failed with status {}: {}", response.code(), errorBody);
                throw new ServiceException("Gemini image generation failed: " + response.code());
            }

            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                throw new ServiceException("Gemini image generation response body is empty");
            }

            String responseJson = responseBody.string();
            return extractImageBytes(responseJson);

        } catch (IOException e) {
            log.error("Gemini image API call failed", e);
            throw new ServiceException("Gemini image API call failed: " + e.getMessage());
        }
    }

    private String buildJsonPayload(String prompt) {
        return String.format("""
            {
                "instances": [
                    {
                        "prompt": "%s"
                    }
                ],
                "parameters": {
                    "sampleCount": %d,
                    "aspectRatio": "1:1",
                    "safetyFilterLevel": "block_some",
                    "personGeneration": "allow_adult"
                }
            }
            """,
                escapeJson(prompt),
                properties.getNumberOfImages());
    }

    private byte[] extractImageBytes(String responseJson) {
        try {
            JsonNode root = objectMapper.readTree(responseJson);
            JsonNode predictions = root.get("predictions");

            if (predictions == null || !predictions.isArray() || predictions.isEmpty()) {
                throw new ServiceException("No image generated in Gemini response");
            }

            JsonNode firstPrediction = predictions.get(0);
            JsonNode bytesBase64 = firstPrediction.get("bytesBase64Encoded");

            if (bytesBase64 == null) {
                throw new ServiceException("No base64 image data in Gemini response");
            }

            String base64String = bytesBase64.asText();
            byte[] imageBytes = Base64.getDecoder().decode(base64String);

            log.debug("Successfully generated image: {} bytes", imageBytes.length);
            return imageBytes;

        } catch (IOException e) {
            log.error("Failed to parse Gemini response", e);
            throw new ServiceException("Failed to parse Gemini image response: " + e.getMessage());
        }
    }

    private String escapeJson(String text) {
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
