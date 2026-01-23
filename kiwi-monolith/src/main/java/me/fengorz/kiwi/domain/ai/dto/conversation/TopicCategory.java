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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * Conversation topic categories
 *
 * @author codingByFeng
 */
@Getter
public enum TopicCategory {

    LIFESTYLE("lifestyle", "Daily life and personal topics"),
    TRAVEL("travel", "Travel and exploration topics"),
    FOOD("food", "Food, cooking, and dining topics"),
    WORK("work", "Professional and workplace topics"),
    HOBBIES("hobbies", "Hobbies and leisure activities"),
    HEALTH("health", "Health and wellness topics");

    private final String code;
    private final String description;

    TopicCategory(String code, String description) {
        this.code = code;
        this.description = description;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    @JsonCreator
    public static TopicCategory fromCode(String code) {
        if (code == null) {
            return LIFESTYLE;
        }
        for (TopicCategory category : values()) {
            if (category.code.equalsIgnoreCase(code)) {
                return category;
            }
        }
        return LIFESTYLE;
    }
}
