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
<<<<<<<< HEAD:kiwi-monolith/src/main/java/me/fengorz/kiwi/domain/tools/dto/focus/CreateSessionRequest.java
package me.fengorz.kiwi.domain.tools.dto.focus;

import lombok.Data;

/**
 * Request DTO for creating a focus session
 *
 * @author codingByFeng
 */
@Data
public class CreateSessionRequest {

    private Integer duration;

    private String treeType;

    private Integer potentialPoints;
========
package me.fengorz.kiwi.domain.tools.dto;

import lombok.Data;

import java.util.Map;

@Data
public class AnalyticsSummaryResponse {
    private Map<String, Object> data;
>>>>>>>> bdd29dca841417badfb46f6e52424b773f3abdc7:kiwi-monolith/src/main/java/me/fengorz/kiwi/domain/tools/dto/AnalyticsSummaryResponse.java
}
