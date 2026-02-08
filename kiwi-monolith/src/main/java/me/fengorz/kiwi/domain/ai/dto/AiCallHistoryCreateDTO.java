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
package me.fengorz.kiwi.domain.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * DTO for creating AI Call History
 *
 * @author codingByFeng
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class AiCallHistoryCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String aiUrl;

    @NotBlank(message = "Prompt is required")
    private String prompt;

    @NotBlank(message = "Prompt mode is required")
    private String promptMode;

    private String targetLanguage;

    private String nativeLanguage;

    private String aiResponse;
}
