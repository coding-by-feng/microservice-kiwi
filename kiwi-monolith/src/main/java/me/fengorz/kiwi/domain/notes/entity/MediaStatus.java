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
package me.fengorz.kiwi.domain.notes.entity;

import lombok.Getter;

/**
 * Media generation status for notes audio and image
 *
 * @author codingByFeng
 */
@Getter
public enum MediaStatus {
    NONE("NONE"),
    PENDING("PENDING"),
    GENERATING("GENERATING"),
    READY("READY"),
    FAILED("FAILED");

    private final String code;

    MediaStatus(String code) {
        this.code = code;
    }

    public static MediaStatus fromCode(String code) {
        if (code == null) {
            return NONE;
        }
        for (MediaStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return NONE;
    }
}
