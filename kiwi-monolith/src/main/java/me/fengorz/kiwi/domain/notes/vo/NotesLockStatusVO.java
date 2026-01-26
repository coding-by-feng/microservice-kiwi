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
package me.fengorz.kiwi.domain.notes.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Notes Lock Status VO
 *
 * @author codingByFeng
 */
@Data
@Builder
public class NotesLockStatusVO {

    /**
     * Whether passcode has been set
     */
    private boolean hasPasscode;

    /**
     * Whether notes are currently locked
     */
    private boolean isLocked;

    /**
     * When the notes were locked
     */
    private LocalDateTime lockTime;

    /**
     * Number of failed unlock attempts
     */
    private Integer failedAttempts;
}
