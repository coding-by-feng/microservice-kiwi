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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.R;
import me.fengorz.kiwi.common.dfs.DfsService;
import me.fengorz.kiwi.domain.ai.config.ConversationProperties;
import me.fengorz.kiwi.domain.ai.dto.conversation.ConversationGenerateRequest;
import me.fengorz.kiwi.domain.ai.service.conversation.ConversationService;
import me.fengorz.kiwi.domain.ai.vo.conversation.ConversationVO;
import me.fengorz.kiwi.security.KiwiUser;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.InputStream;
import java.util.List;

/**
 * Conversation Generation REST Controller with SSE streaming
 *
 * @author codingByFeng
 */
@Slf4j
@RestController
@RequestMapping("/api/ai/conversation")
@RequiredArgsConstructor
@Tag(name = "Conversation", description = "AI conversation generation with multi-speaker TTS")
public class ConversationController {

    private final ConversationService conversationService;
    private final ConversationProperties properties;
    private final DfsService dfsService;

    /**
     * Generate a conversation with SSE streaming
     * Each message is streamed as soon as its audio is ready
     */
    @PostMapping(value = "/generate/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Generate conversation with SSE streaming")
    public SseEmitter generateConversation(
            @AuthenticationPrincipal KiwiUser user,
            @RequestBody @Validated ConversationGenerateRequest request) {

        log.info("Starting conversation generation for user {} with prompt: {}",
                user.getUserId(), request.getPrompt());

        return conversationService.generateConversationStream(request, user.getUserId().longValue());
    }

    /**
     * Get conversation by ID with full details
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get conversation by ID")
    public R<ConversationVO> getConversation(
            @AuthenticationPrincipal KiwiUser user,
            @PathVariable Long id) {

        ConversationVO conversation = conversationService.getConversationById(id, user.getUserId().longValue());
        if (conversation == null) {
            return R.failed("Conversation not found");
        }
        return R.ok(conversation);
    }

    /**
     * List user's conversations
     */
    @GetMapping("/list")
    @Operation(summary = "List user's conversations")
    public R<List<ConversationVO>> listConversations(@AuthenticationPrincipal KiwiUser user) {
        return R.ok(conversationService.listUserConversations(user.getUserId().longValue()));
    }

    /**
     * Delete conversation (soft delete)
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete conversation")
    public R<Void> deleteConversation(
            @AuthenticationPrincipal KiwiUser user,
            @PathVariable Long id) {

        conversationService.deleteConversation(id, user.getUserId().longValue());
        return R.ok(null);
    }

    /**
     * Stream audio file for a message
     */
    @GetMapping("/{conversationId}/audio/{messageId}")
    @Operation(summary = "Stream audio for a message")
    public ResponseEntity<Resource> streamAudio(
            @AuthenticationPrincipal KiwiUser user,
            @PathVariable Long conversationId,
            @PathVariable Long messageId) {

        String audioUrl = conversationService.getMessageAudioUrl(
                conversationId, messageId, user.getUserId().longValue());

        if (audioUrl == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            // Parse FTP path (format: groupName/path)
            String[] parts = audioUrl.split("/", 2);
            String groupName = parts.length > 1 ? parts[0] : "";
            String path = parts.length > 1 ? parts[1] : audioUrl;

            InputStream audioStream = dfsService.downloadStream(groupName, path);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("audio/mpeg"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"message_" + messageId + ".mp3\"")
                    .body(new InputStreamResource(audioStream));
        } catch (Exception e) {
            log.error("Failed to stream audio for message {}", messageId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get conversation generation config
     */
    @GetMapping("/config")
    @Operation(summary = "Get conversation generation config")
    public R<ConversationConfigVO> getConfig() {
        return R.ok(ConversationConfigVO.builder()
                .enabled(properties.isEnabled())
                .maxDailyGenerations(properties.getMaxDailyGenerations())
                .maxSpeakers(properties.getMaxSpeakers())
                .minSpeakers(properties.getMinSpeakers())
                .build());
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ConversationConfigVO {
        private boolean enabled;
        private int maxDailyGenerations;
        private int maxSpeakers;
        private int minSpeakers;
    }
}
