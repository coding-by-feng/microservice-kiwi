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
package me.fengorz.kiwi.domain.ai.ytb;

import lombok.Getter;

/**
 * Subtitle Type Enumeration
 *
 * @author codingByFeng
 */
@Getter
public enum SubtitleTypeEnum {

    SMALL_AUTO_GENERATED_RETURN_STRING("auto_generated_return_string"),
    LARGE_AUTO_GENERATED_RETURN_LIST("auto_generated_vtt_return_list"),
    SMALL_PROFESSIONAL_RETURN_STRING("small_professional_return_string"),
    LARGE_PROFESSIONAL_RETURN_LIST("large_professional_return_list");

    private final String value;

    SubtitleTypeEnum(String value) {
        this.value = value;
    }
}
