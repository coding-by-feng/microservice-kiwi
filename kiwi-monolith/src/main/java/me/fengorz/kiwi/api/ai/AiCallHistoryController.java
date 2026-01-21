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
package me.fengorz.kiwi.api.ai;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import me.fengorz.kiwi.common.R;
import me.fengorz.kiwi.domain.ai.entity.AiCallHistory;
import me.fengorz.kiwi.domain.ai.service.AiCallHistoryService;
import me.fengorz.kiwi.security.KiwiUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AI Call History REST Controller
 *
 * @author codingByFeng
 */
@RestController
@RequestMapping("/api/ai/history")
@RequiredArgsConstructor
@Tag(name = "AI Call History", description = "AI call history management operations")
public class AiCallHistoryController {

    private final AiCallHistoryService historyService;

    @GetMapping("/{id}")
    @Operation(summary = "Get history by ID")
    public R<AiCallHistory> getById(@PathVariable Long id) {
        return historyService.findById(id)
                .map(R::ok)
                .orElse(R.failed("History not found"));
    }

    @GetMapping
    @Operation(summary = "Get current user's history with pagination")
    public R<Page<AiCallHistory>> getMyHistory(
            @AuthenticationPrincipal KiwiUser user,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size) {
        Page<AiCallHistory> page = new Page<>(current, size);
        return R.ok(historyService.findByUserId(user.getUserId().longValue(), page));
    }

    @GetMapping("/favorites")
    @Operation(summary = "Get current user's favorite history")
    public R<List<AiCallHistory>> getMyFavorites(@AuthenticationPrincipal KiwiUser user) {
        return R.ok(historyService.findFavoritesByUserId(user.getUserId().longValue()));
    }

    @GetMapping("/archived")
    @Operation(summary = "Get current user's archived history")
    public R<Page<AiCallHistory>> getMyArchived(
            @AuthenticationPrincipal KiwiUser user,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size) {
        Page<AiCallHistory> page = new Page<>(current, size);
        return R.ok(historyService.findArchivedByUserId(user.getUserId().longValue(), page));
    }

    @GetMapping("/mode/{promptMode}")
    @Operation(summary = "Get history by prompt mode")
    public R<Page<AiCallHistory>> getByPromptMode(
            @AuthenticationPrincipal KiwiUser user,
            @PathVariable String promptMode,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size) {
        Page<AiCallHistory> page = new Page<>(current, size);
        return R.ok(historyService.findByPromptMode(user.getUserId().longValue(), promptMode, page));
    }

    @PostMapping
    @Operation(summary = "Create a new history item")
    public R<AiCallHistory> create(
            @AuthenticationPrincipal KiwiUser user,
            @RequestBody AiCallHistory request) {
        AiCallHistory history = historyService.logCall(
                user.getUserId().longValue(),
                request.getAiUrl(),
                request.getPrompt(),
                request.getPromptMode(),
                request.getTargetLanguage(),
                request.getNativeLanguage());
        return R.ok(history);
    }

    @PostMapping("/{id}/favorite")
    @Operation(summary = "Toggle favorite status")
    public R<Void> toggleFavorite(@PathVariable Long id) {
        historyService.toggleFavorite(id);
        return R.ok();
    }

    @PostMapping("/{id}/archive")
    @Operation(summary = "Archive history item")
    public R<Void> archive(@PathVariable Long id) {
        historyService.archive(id);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete history item")
    public R<Void> delete(@PathVariable Long id) {
        boolean deleted = historyService.deleteHistory(id);
        return deleted ? R.ok() : R.failed("History item not found");
    }
}
