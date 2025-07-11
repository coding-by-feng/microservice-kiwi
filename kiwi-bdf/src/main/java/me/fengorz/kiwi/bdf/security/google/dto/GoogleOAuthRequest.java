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

package me.fengorz.kiwi.bdf.security.google.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

/**
 * Google OAuth request DTOs
 * 
 * @Author Kason Zhan
 * @Date 2025-06-16
 */
public class GoogleOAuthRequest {

    @Data
    @NoArgsConstructor
    public static class TokenRequest {
        @NotBlank(message = "Access token is required")
        private String accessToken;
    }

    @Data
    @NoArgsConstructor
    public static class RefreshTokenRequest {
        @NotBlank(message = "Refresh token is required")
        private String refreshToken;
    }

    @Data
    @NoArgsConstructor
    public static class AuthorizeRequest {
        private String state;
    }
}