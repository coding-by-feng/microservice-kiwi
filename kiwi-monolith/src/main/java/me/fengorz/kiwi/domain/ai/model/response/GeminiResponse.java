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
package me.fengorz.kiwi.domain.ai.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Gemini API Response Model
 *
 * @author codingByFeng
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GeminiResponse {

    @JsonProperty("candidates")
    private List<Candidate> candidates;

    @JsonProperty("usageMetadata")
    private UsageMetadata usageMetadata;

    @JsonProperty("modelVersion")
    private String modelVersion;

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Candidate {
        @JsonProperty("content")
        private Content content;

        @JsonProperty("finishReason")
        private String finishReason;

        @JsonProperty("avgLogprobs")
        private Double avgLogprobs;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Content {
        @JsonProperty("parts")
        private List<Part> parts;

        @JsonProperty("role")
        private String role;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Part {
        @JsonProperty("text")
        private String text;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UsageMetadata {
        @JsonProperty("promptTokenCount")
        private Integer promptTokenCount;

        @JsonProperty("candidatesTokenCount")
        private Integer candidatesTokenCount;

        @JsonProperty("totalTokenCount")
        private Integer totalTokenCount;
    }

    /**
     * Extract the text content from the response
     */
    public String getTextContent() {
        if (candidates != null && !candidates.isEmpty()) {
            Candidate candidate = candidates.get(0);
            if (candidate.getContent() != null && candidate.getContent().getParts() != null
                    && !candidate.getContent().getParts().isEmpty()) {
                return candidate.getContent().getParts().get(0).getText();
            }
        }
        return null;
    }
}
