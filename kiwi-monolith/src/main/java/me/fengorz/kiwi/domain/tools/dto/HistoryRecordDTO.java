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
package me.fengorz.kiwi.domain.tools.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class HistoryRecordDTO {
    private String id;
    private String userId;
    private String taskId;
    private String title;
    private String description;
    private Integer successPoints;
    private Integer failPoints;
    private String status;
    private Integer pointsApplied;
    private LocalDateTime completedAt;
}
