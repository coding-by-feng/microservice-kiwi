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
package me.fengorz.kiwi.domain.ai.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Gemini API Request Model
 * Follows Google's Gemini API format
 *
 * @author codingByFeng
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeminiRequest {

    @JsonProperty("contents")
    private List<Content> contents;

    @JsonProperty("systemInstruction")
    private Content systemInstruction;

    @JsonProperty("generationConfig")
    private GenerationConfig generationConfig;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Content {
        @JsonProperty("role")
        private String role;

        @JsonProperty("parts")
        private List<Part> parts;

        public static Content ofUser(String text) {
            return Content.builder()
                    .role("user")
                    .parts(Collections.singletonList(Part.ofText(text)))
                    .build();
        }

        public static Content ofModel(String text) {
            return Content.builder()
                    .role("model")
                    .parts(Collections.singletonList(Part.ofText(text)))
                    .build();
        }

        public static Content ofSystem(String text) {
            return Content.builder()
                    .parts(Collections.singletonList(Part.ofText(text)))
                    .build();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Part {
        @JsonProperty("text")
        private String text;

        public static Part ofText(String text) {
            return Part.builder().text(text).build();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GenerationConfig {
        @JsonProperty("temperature")
        private Double temperature;

        @JsonProperty("maxOutputTokens")
        private Integer maxOutputTokens;

        @JsonProperty("topP")
        private Double topP;

        @JsonProperty("topK")
        private Integer topK;
    }

    /**
     * Create a simple request with system instruction and user prompt
     */
    public static GeminiRequest create(String systemPrompt, String userPrompt, Double temperature, Integer maxTokens) {
        List<Content> contents = new ArrayList<>();
        contents.add(Content.ofUser(userPrompt));

        return GeminiRequest.builder()
                .contents(contents)
                .systemInstruction(Content.ofSystem(systemPrompt))
                .generationConfig(GenerationConfig.builder()
                        .temperature(temperature)
                        .maxOutputTokens(maxTokens)
                        .build())
                .build();
    }
}
