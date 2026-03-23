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
package me.fengorz.kiwi.common.tts;

import lombok.Getter;

/**
 * Supported English accent types for TTS.
 * Provider-agnostic: includes both GCP language codes and OpenAI steerable instructions.
 *
 * @author codingByFeng
 */
@Getter
public enum AccentType {

    US("en-US", "Speak with an American English accent. Use American pronunciation and intonation patterns."),
    UK("en-GB", "Speak with a British English accent. Use British pronunciation, vocabulary and intonation patterns typical of England."),
    AU("en-AU", "Speak with an Australian English accent. Use Australian pronunciation and intonation patterns."),
    IN("en-IN", "Speak with an Indian English accent. Use Indian English pronunciation patterns, with characteristic intonation and rhythm typical of Indian speakers.");

    private final String languageCode;
    private final String instruction;

    AccentType(String languageCode, String instruction) {
        this.languageCode = languageCode;
        this.instruction = instruction;
    }

    public static AccentType fromCode(String code) {
        if (code == null) {
            return US;
        }
        for (AccentType type : values()) {
            if (type.name().equalsIgnoreCase(code)) {
                return type;
            }
        }
        return US;
    }
}
