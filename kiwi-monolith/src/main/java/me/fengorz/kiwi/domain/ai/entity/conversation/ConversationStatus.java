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
package me.fengorz.kiwi.domain.ai.entity.conversation;

import lombok.Getter;

/**
 * Conversation generation status
 *
 * @author codingByFeng
 */
@Getter
public enum ConversationStatus {

    PENDING("PENDING", "Waiting to start"),
    GENERATING_SCRIPT("GENERATING_SCRIPT", "AI is generating conversation script"),
    GENERATING_AUDIO("GENERATING_AUDIO", "TTS is generating audio files"),
    COMPLETED("COMPLETED", "All audio generation completed"),
    FAILED("FAILED", "Generation failed");

    private final String code;
    private final String description;

    ConversationStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static ConversationStatus fromCode(String code) {
        if (code == null) {
            return PENDING;
        }
        for (ConversationStatus status : values()) {
            if (status.code.equalsIgnoreCase(code)) {
                return status;
            }
        }
        return PENDING;
    }
}
