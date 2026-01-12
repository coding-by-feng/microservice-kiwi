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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.enumeration.LanguageEnum;
import me.fengorz.kiwi.domain.ai.service.YtbSubtitleService;
import me.fengorz.kiwi.ws.model.YtbSubtitleRequest;
import me.fengorz.kiwi.ws.model.YtbSubtitleResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.Executor;

/**
 * YouTube Subtitle SSE Controller
 * Replaces WebSocket with Server-Sent Events for subtitle processing
 *
 * @author codingByFeng
 */
@Slf4j
@RestController
@RequestMapping("/api/ai/sse/ytb")
@Tag(name = "YouTube Subtitle SSE", description = "YouTube subtitle operations using Server-Sent Events")
public class YtbSubtitleSseController {

    private static final String LOG_PREFIX = "[YTB-SUBTITLE-SSE]";
    private static final long SSE_TIMEOUT_MS = 300000L; // 5 minutes

    private final YtbSubtitleService ytbSubtitleService;
    private final Executor taskExecutor;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public YtbSubtitleSseController(YtbSubtitleService ytbSubtitleService,
                                     @Qualifier("webSocketExecutor") Executor taskExecutor) {
        this.ytbSubtitleService = ytbSubtitleService;
        this.taskExecutor = taskExecutor;
    }

    /**
     * Get subtitles with streaming progress updates
     *
     * @param videoUrl    the YouTube video URL
     * @param requestType the request type: "scrolling" or "translated"
     * @param language    the target language (for translated subtitles)
     * @return SSE emitter for streaming subtitle processing updates
     */
    @GetMapping(value = "/subtitle", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Get subtitles with SSE streaming", description = "Stream subtitle processing progress using SSE")
    public SseEmitter getSubtitles(
            @RequestParam("videoUrl") String videoUrl,
            @RequestParam(value = "requestType", defaultValue = "translated") String requestType,
            @RequestParam(value = "language", required = false) String language) {

        log.info("{} Subtitle request - videoUrl: {}, type: {}, language: {}",
                LOG_PREFIX, videoUrl, requestType, language);

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);

        emitter.onCompletion(() -> log.info("{} SSE connection completed", LOG_PREFIX));
        emitter.onTimeout(() -> log.warn("{} SSE connection timed out", LOG_PREFIX));
        emitter.onError(e -> log.error("{} SSE error: {}", LOG_PREFIX, e.getMessage()));

        // Build request object
        YtbSubtitleRequest request = new YtbSubtitleRequest()
                .setVideoUrl(videoUrl)
                .setRequestType(requestType)
                .setLanguage(language);

        // Validate request
        String validationError = validateRequest(request);
        if (validationError != null) {
            log.warn("{} Validation failed: {}", LOG_PREFIX, validationError);
            sendErrorAndComplete(emitter, validationError, "VALIDATION_ERROR", request);
            return emitter;
        }

        // Process subtitle request in background
        taskExecutor.execute(() -> processSubtitleRequest(emitter, request));

        return emitter;
    }

    /**
     * Get subtitles with POST (for complex requests)
     */
    @PostMapping(value = "/subtitle", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Get subtitles with SSE streaming (POST)", description = "Stream subtitle processing with POST request")
    public SseEmitter getSubtitlesPost(@RequestBody YtbSubtitleRequest request) {

        log.info("{} Subtitle POST request - videoUrl: {}, type: {}, language: {}",
                LOG_PREFIX, request.getVideoUrl(), request.getRequestType(), request.getLanguage());

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);

        emitter.onCompletion(() -> log.info("{} SSE connection completed", LOG_PREFIX));
        emitter.onTimeout(() -> log.warn("{} SSE connection timed out", LOG_PREFIX));
        emitter.onError(e -> log.error("{} SSE error: {}", LOG_PREFIX, e.getMessage()));

        // Validate request
        String validationError = validateRequest(request);
        if (validationError != null) {
            log.warn("{} Validation failed: {}", LOG_PREFIX, validationError);
            sendErrorAndComplete(emitter, validationError, "VALIDATION_ERROR", request);
            return emitter;
        }

        // Process subtitle request in background
        taskExecutor.execute(() -> processSubtitleRequest(emitter, request));

        return emitter;
    }

    private String validateRequest(YtbSubtitleRequest request) {
        if (!StringUtils.hasText(request.getVideoUrl())) {
            return "Video URL cannot be empty";
        }

        if (!StringUtils.hasText(request.getRequestType())) {
            return "Request type cannot be empty";
        }

        if (StringUtils.hasText(request.getLanguage()) && !"null".equals(request.getLanguage())) {
            try {
                LanguageEnum.fromCode(request.getLanguage());
            } catch (Exception e) {
                return "Invalid language: " + request.getLanguage();
            }
        }

        return null;
    }

    private void processSubtitleRequest(SseEmitter emitter, YtbSubtitleRequest request) {
        try {
            log.info("{} Starting subtitle processing", LOG_PREFIX);

            long startTime = System.currentTimeMillis();

            // Send started event
            sendEvent(emitter, "started", YtbSubtitleResponse.started("Subtitle processing started", request));

            // Send progress event - Step 1: Fetching
            sendProgressEvent(emitter, 1, 3, "Fetching video information", request);

            String requestType = request.getRequestType() != null ? request.getRequestType().trim().toLowerCase() : "";
            String effectiveLanguage = "scrolling".equals(requestType) ? null : request.getLanguage();

            String result;
            if ("scrolling".equals(requestType)) {
                // Send progress event - Step 2: Processing scrolling subtitles
                sendProgressEvent(emitter, 2, 3, "Processing scrolling subtitles", request);

                var subtitlesResult = ytbSubtitleService.getScrollingSubtitles(request.getVideoUrl());
                result = subtitlesResult != null ? subtitlesResult.toString() : null;
            } else {
                // Send progress event - Step 2: Translating subtitles
                sendProgressEvent(emitter, 2, 3, "Translating subtitles", request);

                result = ytbSubtitleService.getTranslatedSubtitles(request.getVideoUrl(), effectiveLanguage);
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("{} Subtitle processing completed - Duration: {}ms", LOG_PREFIX, duration);

            // Send the result chunk
            if (result != null) {
                sendEvent(emitter, "chunk", YtbSubtitleResponse.chunk(result, request));
            }

            // Send completed event
            sendEvent(emitter, "completed",
                    YtbSubtitleResponse.completed("Subtitle processing completed", request, result, duration));

            emitter.complete();

        } catch (Exception e) {
            log.error("{} Error processing subtitle request: {}", LOG_PREFIX, e.getMessage(), e);
            sendErrorAndComplete(emitter, "Subtitle processing failed: " + e.getMessage(),
                    "PROCESSING_ERROR", request);
        }
    }

    private void sendProgressEvent(SseEmitter emitter, int currentStep, int totalSteps, String description,
                                    YtbSubtitleRequest request) {
        YtbSubtitleResponse response = new YtbSubtitleResponse()
                .setType("progress")
                .setCurrentStep(currentStep)
                .setTotalSteps(totalSteps)
                .setCurrentStepDescription(description)
                .setOriginalRequest(request);
        sendEvent(emitter, "progress", response);
    }

    private void sendEvent(SseEmitter emitter, String eventName, Object data) {
        try {
            String json = objectMapper.writeValueAsString(data);
            emitter.send(SseEmitter.event().name(eventName).data(json, MediaType.APPLICATION_JSON));
        } catch (IOException e) {
            log.error("{} Failed to send event: {}", LOG_PREFIX, e.getMessage());
        }
    }

    private void sendErrorAndComplete(SseEmitter emitter, String message, String errorCode, YtbSubtitleRequest request) {
        try {
            sendEvent(emitter, "error", YtbSubtitleResponse.error(message, errorCode, request));
            emitter.complete();
        } catch (Exception e) {
            log.error("{} Failed to send error: {}", LOG_PREFIX, e.getMessage());
            try {
                emitter.completeWithError(e);
            } catch (Exception ignored) {
            }
        }
    }
}
