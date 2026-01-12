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
package me.fengorz.kiwi.api.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.R;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * One-Time Code Authentication Controller
 * Simple code verification for anonymous access to trusted apps (e.g., Rangi Windows)
 *
 * @author codingByFeng
 */
@Slf4j
@RestController
@RequestMapping("/api/oauth/one-time")
@Tag(name = "One-Time Code Auth", description = "Simple code verification for anonymous access")
public class OneTimeCodeController {

    @Value("${kiwi.auth.one-time-code:}")
    private String configuredCode;

    /**
     * Verify one-time code for anonymous access
     */
    @PostMapping(value = "/verify-code", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Verify one-time code for anonymous access")
    public R<Map<String, Object>> verifyCode(@RequestBody VerifyCodeRequest request) {
        if (!StringUtils.hasText(request.getCode())) {
            return R.failed("Code is required");
        }

        if (!StringUtils.hasText(configuredCode)) {
            log.warn("One-time code not configured. Set KIWI_ONE_TIME_CODE environment variable.");
            return R.failed("One-time authentication not configured");
        }

        if (!configuredCode.equals(request.getCode())) {
            log.warn("Invalid one-time code attempt");
            return R.failed("Invalid code");
        }

        log.info("One-time code verification successful");

        Map<String, Object> response = new HashMap<>();
        response.put("verified", true);
        response.put("message", "Code verified successfully");

        return R.ok(response);
    }

    @Data
    public static class VerifyCodeRequest {
        private String code;
    }
}
