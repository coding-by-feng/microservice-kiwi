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

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Gemini Image Generation Properties (Vertex AI)
 *
 * @author codingByFeng
 */
@Data
@Component
@ConfigurationProperties(prefix = "kiwi.gemini")
public class GeminiImageProperties {

    /**
     * GCP Project ID for Vertex AI
     */
    private String projectId;

    /**
     * GCP region for Vertex AI (e.g., us-central1)
     */
    private String location = "us-central1";

    /**
     * Gemini image generation model
     */
    private String imageModel = "imagen-4.0-generate-001";

    /**
     * Image width
     */
    private Integer imageWidth = 1024;

    /**
     * Image height
     */
    private Integer imageHeight = 1024;

    /**
     * Number of images to generate
     */
    private Integer numberOfImages = 1;

    /**
     * Whether the feature is enabled
     */
    private boolean enabled = true;

    /**
     * Gemini API key for fallback (generativelanguage.googleapis.com)
     */
    private String apiKey;

    /**
     * Gemini model for image generation fallback (e.g., gemini-2.0-flash-exp)
     */
    private String geminiModel = "imagen-3.0-generate-002";

    /**
     * Build the Vertex AI Imagen predict endpoint URL
     */
    public String getVertexAiEndpoint() {
        return String.format("https://%s-aiplatform.googleapis.com/v1/projects/%s/locations/%s/publishers/google/models/%s:predict",
                location, projectId, location, imageModel);
    }

    /**
     * Build the Gemini API Imagen predict endpoint URL (fallback)
     */
    public String getGeminiApiEndpoint() {
        return String.format("https://generativelanguage.googleapis.com/v1beta/models/%s:predict?key=%s",
                geminiModel, apiKey);
    }
}
