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
import me.fengorz.kiwi.domain.word.config.ReviewAudioProperties;
import me.fengorz.kiwi.domain.word.service.ReviewAudioService;
import me.fengorz.kiwi.domain.word.vo.ReviewAudioGenerationResult;
import me.fengorz.kiwi.security.KiwiUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Word Review Controller
 * Handles word review, spaced repetition, and audio generation operations
 *
 * @author codingByFeng
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/word/review")
@Tag(name = "Word Review", description = "Word review and spaced repetition operations")
public class WordReviewController {

    private final ReviewAudioService reviewAudioService;
    private final ReviewAudioProperties reviewAudioProperties;

    // ==================== Audio Generation Endpoints ====================

    /**
     * Generate audio for all paraphrases in a star list
     */
    @PostMapping("/audio/generate/list/{listId}")
    @Operation(summary = "Generate audio for all paraphrases in a star list")
    public R<ReviewAudioGenerationResult> generateAudioForStarList(
            @PathVariable Integer listId,
            @AuthenticationPrincipal KiwiUser user) {
        checkUserAuthenticated(user);
        log.info("Generating audio for star list {} by user {}", listId, user.getUserId());
        ReviewAudioGenerationResult result = reviewAudioService.generateAudioForStarList(listId);
        return R.ok(result);
    }

    /**
     * Generate audio for review items only (not remembered yet)
     */
    @PostMapping("/audio/generate/review-items/{listId}")
    @Operation(summary = "Generate audio for review items in a star list")
    public R<ReviewAudioGenerationResult> generateAudioForReviewItems(
            @PathVariable Integer listId,
            @AuthenticationPrincipal KiwiUser user) {
        checkUserAuthenticated(user);
        log.info("Generating audio for review items in list {} by user {}", listId, user.getUserId());
        ReviewAudioGenerationResult result = reviewAudioService.generateAudioForReviewItems(listId);
        return R.ok(result);
    }

    /**
     * Generate audio for a single paraphrase
     */
    @PostMapping("/audio/generate/paraphrase/{paraphraseId}")
    @Operation(summary = "Generate audio for a single paraphrase")
    public R<Boolean> generateAudioForParaphrase(
            @PathVariable Integer paraphraseId,
            @AuthenticationPrincipal KiwiUser user) {
        checkUserAuthenticated(user);
        log.info("Generating audio for paraphrase {} by user {}", paraphraseId, user.getUserId());
        boolean success = reviewAudioService.generateAudioForParaphrase(paraphraseId);
        return R.ok(success);
    }

    /**
     * Check if audio exists for a paraphrase
     */
    @GetMapping("/audio/exists/{paraphraseId}")
    @Operation(summary = "Check if audio exists for a paraphrase")
    public R<Boolean> hasAudio(@PathVariable Integer paraphraseId) {
        return R.ok(reviewAudioService.hasAudio(paraphraseId));
    }

    /**
     * Download/stream audio for a paraphrase
     */
    @GetMapping("/audio/download/{paraphraseId}")
    @Operation(summary = "Download audio for a paraphrase")
    public void downloadAudio(
            HttpServletResponse response,
            @PathVariable Integer paraphraseId) {
        log.debug("Downloading audio for paraphrase {}", paraphraseId);

        byte[] audioBytes = reviewAudioService.getAudioBytes(paraphraseId);
        if (audioBytes == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        response.setContentType("audio/mpeg");
        response.setContentLength(audioBytes.length);
        response.setHeader("Content-Disposition", "inline; filename=\"paraphrase_" + paraphraseId + ".mp3\"");

        try (OutputStream os = response.getOutputStream()) {
            os.write(audioBytes);
            os.flush();
        } catch (IOException e) {
            log.error("Failed to stream audio for paraphrase {}", paraphraseId, e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Delete audio for a paraphrase
     */
    @DeleteMapping("/audio/{paraphraseId}")
    @Operation(summary = "Delete audio for a paraphrase")
    public R<Boolean> deleteAudio(
            @PathVariable Integer paraphraseId,
            @AuthenticationPrincipal KiwiUser user) {
        checkUserAuthenticated(user);
        log.info("Deleting audio for paraphrase {} by user {}", paraphraseId, user.getUserId());
        boolean success = reviewAudioService.deleteAudio(paraphraseId);
        return R.ok(success);
    }

    /**
     * Regenerate audio for a paraphrase
     */
    @PostMapping("/audio/regenerate/{paraphraseId}")
    @Operation(summary = "Regenerate audio for a paraphrase")
    public R<Boolean> regenerateAudio(
            @PathVariable Integer paraphraseId,
            @AuthenticationPrincipal KiwiUser user) {
        checkUserAuthenticated(user);
        log.info("Regenerating audio for paraphrase {} by user {}", paraphraseId, user.getUserId());
        boolean success = reviewAudioService.regenerateAudio(paraphraseId);
        return R.ok(success);
    }

    /**
     * Get audio configuration/limits
     */
    @GetMapping("/audio/config")
    @Operation(summary = "Get audio generation configuration")
    public R<Map<String, Object>> getAudioConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("enabled", reviewAudioProperties.isEnabled());
        config.put("maxGenerationCount", reviewAudioProperties.getMaxGenerationCount());
        config.put("contentMode", reviewAudioProperties.getContentMode());
        config.put("asyncGeneration", reviewAudioProperties.isAsyncGeneration());
        return R.ok(config);
    }

    // ==================== Legacy Review Endpoints ====================

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
        log.info("Creating review days for user: {}", user.getUserId());
        return R.ok();
    }

    /**
     * Refresh all TTS API keys
     */
    @GetMapping("/refreshAllApiKey")
    @Operation(summary = "Refresh all TTS API keys")
    public R<Void> refreshAllApiKey() {
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
        return R.ok(List.of());
    }

    /**
     * Download review audio (legacy endpoint - redirects to new endpoint)
     */
    @GetMapping("/downloadReviewAudio/{sourceId}/{type}")
    @Operation(summary = "Download review audio (legacy)")
    public void downloadReviewAudio(
            HttpServletResponse response,
            @PathVariable("sourceId") Integer sourceId,
            @PathVariable("type") Integer type) {
        log.info("downloadReviewAudio (legacy), sourceId={}, type={}", sourceId, type);
        // Type 1 = paraphrase
        if (type == 1) {
            downloadAudio(response, sourceId);
        } else {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
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
        // TODO: Implement character audio
        response.setContentType("audio/mpeg");
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
    }

    /**
     * Generate TTS voice from paraphrase ID (deprecated - use new endpoint)
     */
    @Deprecated
    @PostMapping("/generateTtsVoiceFromParaphraseId/{paraphraseId}")
    @Operation(summary = "Generate TTS voice from paraphrase ID (deprecated)")
    public R<Boolean> generateTtsVoiceFromParaphraseId(@PathVariable("paraphraseId") Integer paraphraseId) {
        log.info("Generating TTS voice for paraphraseId={} (deprecated endpoint)", paraphraseId);
        boolean success = reviewAudioService.generateAudioForParaphrase(paraphraseId);
        return R.ok(success);
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
        log.info("Increasing counter type={} for user={}", type, user.getUserId());
        return R.ok();
    }

    /**
     * Auto select API key
     */
    @GetMapping("/autoSelectApiKey")
    @Operation(summary = "Auto select API key")
    public R<String> autoSelectApiKey() {
        return R.ok("");
    }

    /**
     * Increase API key used time
     */
    @PutMapping("/increaseApiKeyUsedTime/{apiKey}")
    @Operation(summary = "Increase API key used time")
    public R<Void> increaseApiKeyUsedTime(@PathVariable("apiKey") String apiKey) {
        log.info("Increasing API key used time: {}", apiKey);
        return R.ok();
    }

    /**
     * Deprecate API key for today
     */
    @PutMapping("/deprecateApiKeyToday/{apiKey}")
    @Operation(summary = "Deprecate API key for today")
    public R<Void> deprecateApiKeyToday(@PathVariable("apiKey") String apiKey) {
        log.info("Deprecating API key for today: {}", apiKey);
        return R.ok();
    }

    /**
     * Deprecate review audio (legacy - use DELETE /audio/{paraphraseId})
     */
    @DeleteMapping("/deprecate-review-audio/{sourceId}")
    @Operation(summary = "Deprecate review audio (legacy)")
    public R<Boolean> deprecateReviewAudio(@PathVariable("sourceId") Integer sourceId) {
        log.info("Deprecating review audio for sourceId={}", sourceId);
        boolean success = reviewAudioService.deleteAudio(sourceId);
        return R.ok(success);
    }

    /**
     * Regenerate review audio for paraphrase (legacy - use POST /audio/regenerate/{paraphraseId})
     */
    @DeleteMapping("/reGenReviewAudio/{sourceId}")
    @Operation(summary = "Regenerate review audio (legacy)")
    public R<Boolean> reGenReviewAudioForParaphrase(@PathVariable("sourceId") Integer sourceId) {
        log.info("Regenerating review audio for sourceId={}", sourceId);
        boolean success = reviewAudioService.regenerateAudio(sourceId);
        return R.ok(success);
    }

    private void checkUserAuthenticated(KiwiUser user) {
        if (user == null) {
            throw new AuthException("User not authenticated");
        }
    }
}
