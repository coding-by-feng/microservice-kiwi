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
package me.fengorz.kiwi.api.tools;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.R;
import me.fengorz.kiwi.domain.tools.dto.focus.*;
import me.fengorz.kiwi.domain.tools.service.FocusService;
import me.fengorz.kiwi.security.KiwiUser;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Focus Timer Controller
 * Focus session and forest management operations
 *
 * @author codingByFeng
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/todo/focus")
@Tag(name = "Focus Timer", description = "Focus session and forest management")
public class FocusController {

    private final FocusService focusService;

    // ==================== STATS ====================

    @GetMapping("/stats")
    @Operation(summary = "Get focus statistics")
    public R<FocusStatsDTO> getStats(@AuthenticationPrincipal KiwiUser user) {
        return R.ok(focusService.getStats(user.getUserId()));
    }

    // ==================== SESSIONS ====================

    @PostMapping(value = "/sessions", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create a new focus session (start timer)")
    public ResponseEntity<R<Map<String, Object>>> createSession(
            @AuthenticationPrincipal KiwiUser user,
            @RequestBody CreateSessionRequest body) {
        Map<String, Object> result = focusService.createSession(user.getUserId(), body);
        return ResponseEntity.status(HttpStatus.CREATED).body(R.ok(result));
    }

    @PostMapping(value = "/sessions/{sessionId}/complete", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Complete a focus session (timer finished)")
    public R<Map<String, Object>> completeSession(
            @AuthenticationPrincipal KiwiUser user,
            @PathVariable String sessionId,
            @RequestBody(required = false) CompleteSessionRequest body) {
        return R.ok(focusService.completeSession(user.getUserId(), sessionId, body));
    }

    @PostMapping(value = "/sessions/{sessionId}/fail", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Fail a focus session (user left page)")
    public R<Map<String, Object>> failSession(
            @AuthenticationPrincipal KiwiUser user,
            @PathVariable String sessionId,
            @RequestBody(required = false) FailSessionRequest body) {
        return R.ok(focusService.failSession(user.getUserId(), sessionId, body));
    }

    @PostMapping("/sessions/{sessionId}/cancel")
    @Operation(summary = "Cancel a focus session")
    public R<Map<String, Object>> cancelSession(
            @AuthenticationPrincipal KiwiUser user,
            @PathVariable String sessionId) {
        return R.ok(focusService.cancelSession(user.getUserId(), sessionId));
    }

    @GetMapping("/sessions")
    @Operation(summary = "List focus session history")
    public R<Map<String, Object>> listSessions(
            @AuthenticationPrincipal KiwiUser user,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String treeType) {
        return R.ok(focusService.listSessions(user.getUserId(), page, pageSize, status, startDate, endDate, treeType));
    }

    // ==================== FOREST ====================

    @GetMapping("/forest")
    @Operation(summary = "Get planted trees (forest grid)")
    public R<Map<String, Object>> getForest(
            @AuthenticationPrincipal KiwiUser user,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Integer offset) {
        return R.ok(focusService.getForest(user.getUserId(), limit, offset));
    }
}
