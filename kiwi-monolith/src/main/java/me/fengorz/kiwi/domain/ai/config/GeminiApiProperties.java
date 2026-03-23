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
package me.fengorz.kiwi.domain.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.Serializable;

/**
 * Gemini API Properties Configuration (Vertex AI)
 *
 * @author codingByFeng
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai.gemini.api")
public class GeminiApiProperties implements Serializable {

    private static final long serialVersionUID = -367572969083407340L;

    /**
     * GCP Project ID for Vertex AI
     */
    private String projectId;

    /**
     * GCP region for Vertex AI (e.g., us-central1, europe-west1)
     */
    private String location = "us-central1";

    /**
     * Gemini model name (e.g., gemini-2.5-flash, gemini-2.0-flash, gemini-1.5-pro)
     */
    private String model = "gemini-2.5-flash";

    /**
     * Number of prompts per batch for batch processing
     */
    private Integer threadPromptsLineSize = 50;

    /**
     * Thread pool size for batch processing (defaults to CPU cores if null)
     */
    private Integer threadPoolSize;

    /**
     * Timeout in seconds for batch processing
     */
    private Integer threadTimeoutSecs = 300;

    /**
     * Temperature for response generation (0.0 to 2.0)
     */
    private Double temperature = 1.0;

    /**
     * Maximum output tokens
     */
    private Integer maxOutputTokens = 8192;

    /**
     * Build the Vertex AI base endpoint URL
     */
    public String getVertexAiEndpoint() {
        return String.format("https://%s-aiplatform.googleapis.com/v1/projects/%s/locations/%s/publishers/google/models",
                location, projectId, location);
    }
}
