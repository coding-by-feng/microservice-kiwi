/*
 *
 * Copyright [2019~2025] [zhanshifeng]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 *
 */

package me.fengorz.kiwi.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.Set;

/**
 * Google OAuth response DTO
 * 
 * @Author Kason Zhan
 * @Date 2025-06-16
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoogleOAuthResponse {

    private String accessToken;
    private String tokenType;
    private Integer expiresIn;
    private String refreshToken;
    private Set<String> scope;
    private Map<String, Object> userInfo;
    private Map<String, Object> additionalInfo;
}