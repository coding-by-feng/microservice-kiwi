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
package me.fengorz.kiwi.api.word;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.R;
import me.fengorz.kiwi.common.exception.AuthException;
import me.fengorz.kiwi.security.KiwiUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Word Review Controller
 * Aligned with original microservice WordReviewController
 *
 * @author codingByFeng
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/word/review")
@Tag(name = "Word Review", description = "Word review and spaced repetition operations")
public class WordReviewController {

    // TODO: Inject ReviewService, TtsService, DfsService when implemented

    /**
     * Get review breakpoint page number for a list
     */
    @GetMapping("/getReviewBreakpointPageNumber/{listId}")
    @Operation(summary = "Get review breakpoint page number")
    public R<Integer> getReviewBreakpointPageNumber(@PathVariable Integer listId) {
        // TODO: Implement with ReviewService.getReviewBreakpointPageNumber(listId)
        return R.ok(0);
    }

    /**
     * Create review days for current user
     */
    @PostMapping("/createTheDays")
    @Operation(summary = "Create review days for current user")
    public R<Void> createTheDays(@AuthenticationPrincipal KiwiUser user) {
        checkUserAuthenticated(user);
        // TODO: Implement with ReviewService.createTheDays(userId)
        log.info("Creating review days for user: {}", user.getUserId());
        return R.ok();
    }

    /**
     * Refresh all TTS API keys
     */
    @GetMapping("/refreshAllApiKey")
    @Operation(summary = "Refresh all TTS API keys")
    public R<Void> refreshAllApiKey() {
        // TODO: Implement with TtsService.refreshAllApiKey()
        log.info("Refreshing all API keys");
        return R.ok();
    }

    /**
     * Get review counter VO by type
     */
    @GetMapping("/getReviewCounterVO/{type}")
    @Operation(summary = "Get review counter by type")
    public R<Map<String, Object>> getReviewCounterVO(
            @PathVariable("type") Integer type,
            @AuthenticationPrincipal KiwiUser user) {
        checkUserAuthenticated(user);
        // TODO: Implement with ReviewService.findReviewCounterVO(userId, type)
        Map<String, Object> counter = new HashMap<>();
        counter.put("type", type);
        counter.put("userId", user.getUserId());
        counter.put("count", 0);
        return R.ok(counter);
    }

    /**
     * Get all review counters for current user
     */
    @GetMapping("/getAllReviewCounterVO")
    @Operation(summary = "Get all review counters for current user")
    public R<List<Map<String, Object>>> getAllReviewCounterVO(@AuthenticationPrincipal KiwiUser user) {
        checkUserAuthenticated(user);
        // TODO: Implement with ReviewService.listReviewCounterVO(userId)
        return R.ok(List.of());
    }

    /**
     * Download review audio
     */
    @GetMapping("/downloadReviewAudio/{sourceId}/{type}")
    @Operation(summary = "Download review audio")
    public void downloadReviewAudio(
            HttpServletResponse response,
            @PathVariable("sourceId") Integer sourceId,
            @PathVariable("type") Integer type) {
        log.info("downloadReviewAudio, sourceId={}, type={}", sourceId, type);
        // TODO: Implement with ReviewService and DfsService
        response.setContentType("audio/mpeg");
    }

    /**
     * Download character review audio
     */
    @GetMapping("/character/downloadReviewAudio/{characterCode}")
    @Operation(summary = "Download character review audio")
    public void downloadCharacterReviewAudio(
            HttpServletResponse response,
            @PathVariable("characterCode") String characterCode) {
        log.info("downloadCharacterReviewAudio, characterCode={}", characterCode);
        // TODO: Implement with ReviewService and DfsService
        response.setContentType("audio/mpeg");
    }

    /**
     * Generate TTS voice from paraphrase ID (deprecated)
     */
    @Deprecated
    @PostMapping("/generateTtsVoiceFromParaphraseId/{paraphraseId}")
    @Operation(summary = "Generate TTS voice from paraphrase ID")
    public R<Void> generateTtsVoiceFromParaphraseId(@PathVariable("paraphraseId") Integer paraphraseId) {
        // TODO: Implement with ReviewService.generateTtsVoiceFromParaphraseId(paraphraseId)
        log.info("Generating TTS voice for paraphraseId={}", paraphraseId);
        return R.ok();
    }

    /**
     * Increase review counter
     */
    @PutMapping("/increaseCounter/{type}")
    @Operation(summary = "Increase review counter")
    public R<Void> increaseCounter(
            @PathVariable("type") Integer type,
            @AuthenticationPrincipal KiwiUser user) {
        checkUserAuthenticated(user);
        // TODO: Implement with ReviewService.increase(type, userId)
        log.info("Increasing counter type={} for user={}", type, user.getUserId());
        return R.ok();
    }

    /**
     * Auto select API key
     */
    @GetMapping("/autoSelectApiKey")
    @Operation(summary = "Auto select API key")
    public R<String> autoSelectApiKey() {
        // TODO: Implement with TtsService.autoSelectApiKey()
        return R.ok("");
    }

    /**
     * Increase API key used time
     */
    @PutMapping("/increaseApiKeyUsedTime/{apiKey}")
    @Operation(summary = "Increase API key used time")
    public R<Void> increaseApiKeyUsedTime(@PathVariable("apiKey") String apiKey) {
        // TODO: Implement with TtsService.increaseApiKeyUsedTime(apiKey)
        log.info("Increasing API key used time: {}", apiKey);
        return R.ok();
    }

    /**
     * Deprecate API key for today
     */
    @PutMapping("/deprecateApiKeyToday/{apiKey}")
    @Operation(summary = "Deprecate API key for today")
    public R<Void> deprecateApiKeyToday(@PathVariable("apiKey") String apiKey) {
        // TODO: Implement with TtsService.deprecateApiKeyToday(apiKey)
        log.info("Deprecating API key for today: {}", apiKey);
        return R.ok();
    }

    /**
     * Deprecate review audio
     */
    @DeleteMapping("/deprecate-review-audio/{sourceId}")
    @Operation(summary = "Deprecate review audio")
    public R<Void> deprecateReviewAudio(@PathVariable("sourceId") Integer sourceId) {
        // TODO: Implement with ReviewService.removeWordReviewAudio(sourceId)
        log.info("Deprecating review audio for sourceId={}", sourceId);
        return R.ok();
    }

    /**
     * Regenerate review audio for paraphrase
     */
    @DeleteMapping("/reGenReviewAudio/{sourceId}")
    @Operation(summary = "Regenerate review audio for paraphrase")
    public R<Void> reGenReviewAudioForParaphrase(@PathVariable("sourceId") Integer sourceId) {
        // TODO: Implement with ReviewService.reGenReviewAudioForParaphrase(sourceId)
        log.info("Regenerating review audio for sourceId={}", sourceId);
        return R.ok();
    }

    private void checkUserAuthenticated(KiwiUser user) {
        if (user == null) {
            throw new AuthException("User not authenticated");
        }
    }
}
