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

import lombok.Getter;

/**
 * Conversation duration options
 *
 * @author codingByFeng
 */
@Getter
public enum DurationOption {

    TWO_MINUTES(2, "2 minutes", 8, 12),
    FIVE_MINUTES(5, "5 minutes", 20, 30),
    TEN_MINUTES(10, "10 minutes", 40, 60);

    private final int minutes;
    private final String description;
    private final int minMessages;
    private final int maxMessages;

    DurationOption(int minutes, String description, int minMessages, int maxMessages) {
        this.minutes = minutes;
        this.description = description;
        this.minMessages = minMessages;
        this.maxMessages = maxMessages;
    }

    public static DurationOption fromMinutes(int minutes) {
        for (DurationOption option : values()) {
            if (option.minutes == minutes) {
                return option;
            }
        }
        return FIVE_MINUTES;
    }
}
