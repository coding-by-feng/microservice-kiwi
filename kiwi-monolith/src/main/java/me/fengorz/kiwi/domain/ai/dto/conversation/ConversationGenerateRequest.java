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
package me.fengorz.kiwi.domain.ai.dto.conversation;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import me.fengorz.kiwi.common.tts.OpenAiTtsProperties;

/**
 * Conversation generation request DTO
 *
 * @author codingByFeng
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationGenerateRequest {

    /**
     * Topic description / prompt for the conversation
     */
    @NotBlank(message = "Prompt cannot be empty")
    private String prompt;

    /**
     * English accent: US, UK, AU, IN
     */
    @NotNull(message = "Accent must be specified")
    private OpenAiTtsProperties.AccentType accent;

    /**
     * Target conversation duration
     */
    @NotNull(message = "Duration must be specified")
    private DurationOption duration;

    /**
     * Number of speakers (2-4)
     */
    @NotNull(message = "Speaker count must be specified")
    @Min(value = 2, message = "Minimum 2 speakers required")
    @Max(value = 4, message = "Maximum 4 speakers allowed")
    private Integer speakerCount;
}
