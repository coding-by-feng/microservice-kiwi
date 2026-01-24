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
package me.fengorz.kiwi.domain.ai.service.conversation;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.exception.ServiceException;
import me.fengorz.kiwi.common.tts.OpenAiTtsProperties;
import me.fengorz.kiwi.domain.ai.config.ConversationProperties;
import me.fengorz.kiwi.domain.ai.dto.conversation.ConversationGenerateRequest;
import me.fengorz.kiwi.domain.ai.entity.conversation.*;
import me.fengorz.kiwi.domain.ai.mapper.ConversationMapper;
import me.fengorz.kiwi.domain.ai.mapper.ConversationMessageMapper;
import me.fengorz.kiwi.domain.ai.mapper.ConversationSpeakerMapper;
import me.fengorz.kiwi.domain.ai.vo.conversation.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;

/**
 * Main Conversation Service - orchestrates script generation, TTS, and persistence
 *
 * @author codingByFeng
 */
@Slf4j
@Service
public class ConversationService extends ServiceImpl<ConversationMapper, Conversation> {

    private final ConversationScriptService scriptService;
    private final ConversationTtsService ttsService;
    private final ConversationSpeakerMapper speakerMapper;
    private final ConversationMessageMapper messageMapper;
    private final ConversationProperties properties;
    private final Executor taskExecutor;
    private final Executor ttsExecutor;
    private final Semaphore ttsSemaphore;

    public ConversationService(ConversationScriptService scriptService,
                               ConversationTtsService ttsService,
                               ConversationSpeakerMapper speakerMapper,
                               ConversationMessageMapper messageMapper,
                               ConversationProperties properties,
                               @Qualifier("webSocketExecutor") Executor taskExecutor,
                               @Qualifier("ttsExecutor") Executor ttsExecutor) {
        this.scriptService = scriptService;
        this.ttsService = ttsService;
        this.speakerMapper = speakerMapper;
        this.messageMapper = messageMapper;
        this.properties = properties;
        this.taskExecutor = taskExecutor;
        this.ttsExecutor = ttsExecutor;
        this.ttsSemaphore = new Semaphore(properties.getTtsMaxConcurrency());
    }

    /**
     * Result of audio generation for a single message
     */
    private record AudioResult(Long messageId, String audioUrl, int durationMs, boolean success, String error) {}

    private static final String CACHE_NAME = "conversation";

    /**
     * Generate a conversation with SSE streaming
     *
     * @param request the generation request
     * @param userId  the user ID
     * @return SSE emitter for streaming responses
     */
    public SseEmitter generateConversationStream(ConversationGenerateRequest request, Long userId) {
        if (!properties.isEnabled()) {
            throw new ServiceException("Conversation generation is disabled");
        }

        SseEmitter emitter = new SseEmitter(properties.getSseTimeoutMs());

        emitter.onCompletion(() -> log.info("SSE connection completed for user {}", userId));
        emitter.onTimeout(() -> log.warn("SSE connection timed out for user {}", userId));
        emitter.onError(e -> log.error("SSE error for user {}", userId, e));

        taskExecutor.execute(() -> processConversationGeneration(emitter, request, userId));

        return emitter;
    }

    private void processConversationGeneration(SseEmitter emitter, ConversationGenerateRequest request, Long userId) {
        long startTime = System.currentTimeMillis();
        Conversation conversation = null;

        try {
            // 1. Create conversation record
            conversation = createConversation(request, userId);
            updateConversationStatus(conversation, ConversationStatus.GENERATING_SCRIPT);

            // 2. Generate script via AI
            log.info("Generating script for conversation {}", conversation.getId());
            ConversationScriptService.ConversationScript script = scriptService.generateScript(
                    request.getPrompt(),
                    request.getDuration(),
                    request.getSpeakerCount()
            );

            // 3. Update conversation with topic
            conversation.setTopic(script.getTopic());
            conversation.setTotalMessages(script.getMessages().size());
            updateById(conversation);

            // 4. Save speakers and assign voices
            List<String> voices = ttsService.assignVoices(request.getSpeakerCount());
            Map<Integer, ConversationSpeaker> speakerMap = saveSpeakers(conversation.getId(), script.getSpeakers(), voices);

            // 5. Send metadata event
            List<SpeakerVO> speakerVOs = speakerMap.values().stream()
                    .map(SpeakerVO::fromEntity)
                    .collect(Collectors.toList());

            sendEvent(emitter, "metadata", ConversationSseEvent.MetadataPayload.builder()
                    .conversationId(conversation.getId())
                    .topic(script.getTopic())
                    .speakers(speakerVOs)
                    .totalMessageCount(script.getMessages().size())
                    .build());

            // 6. Generate audio for each message concurrently
            updateConversationStatus(conversation, ConversationStatus.GENERATING_AUDIO);

            // Create all message records first
            List<MessageWithSpeaker> messagesWithSpeakers = new ArrayList<>();
            for (ConversationScriptService.Message scriptMessage : script.getMessages()) {
                ConversationSpeaker speaker = speakerMap.get(scriptMessage.getSpeakerIndex());
                if (speaker == null) {
                    log.warn("Speaker not found for index {}", scriptMessage.getSpeakerIndex());
                    continue;
                }

                ConversationMessage message = createMessage(
                        conversation.getId(),
                        speaker.getId(),
                        scriptMessage.getSequence(),
                        scriptMessage.getText()
                );
                message.setAudioStatusEnum(AudioStatus.GENERATING);
                messageMapper.updateById(message);

                messagesWithSpeakers.add(new MessageWithSpeaker(message, speaker));
            }

            final Long conversationId = conversation.getId();
            final OpenAiTtsProperties.AccentType accent = request.getAccent();

            // Submit concurrent audio generation tasks with rate limiting
            log.info("Starting concurrent audio generation for {} messages with concurrency limit {}",
                    messagesWithSpeakers.size(), properties.getTtsMaxConcurrency());

            List<CompletableFuture<AudioResult>> futures = messagesWithSpeakers.stream()
                    .map(mws -> CompletableFuture.supplyAsync(
                            () -> generateAudioForMessage(mws, accent, conversationId),
                            ttsExecutor))
                    .toList();

            // Wait for all audio generation to complete
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            // Process results in order and send SSE events
            long totalAudioDuration = 0;
            int completedCount = 0;
            int totalMessages = messagesWithSpeakers.size();

            for (int i = 0; i < totalMessages; i++) {
                AudioResult result = futures.get(i).join();
                MessageWithSpeaker mws = messagesWithSpeakers.get(i);
                ConversationMessage message = mws.message();

                if (result.success()) {
                    // Update message with audio info
                    message.setAudioUrl(result.audioUrl());
                    message.setAudioDurationMs(result.durationMs());
                    message.setAudioStatusEnum(AudioStatus.READY);
                    messageMapper.updateById(message);

                    totalAudioDuration += result.durationMs();
                    completedCount++;

                    // Send message event
                    message.setSpeakerName(mws.speaker().getName());
                    sendEvent(emitter, "message", MessageVO.fromEntity(message));
                } else {
                    // Mark as failed
                    message.setAudioStatusEnum(AudioStatus.FAILED);
                    messageMapper.updateById(message);

                    // Send error but continue with other messages
                    sendEvent(emitter, "error", ConversationSseEvent.ErrorPayload.builder()
                            .message("Failed to generate audio for message " + message.getSequence() + ": " + result.error())
                            .code("TTS_ERROR")
                            .build());
                }

                // Send progress event
                int percentage = ((i + 1) * 100) / totalMessages;
                sendEvent(emitter, "progress", ConversationSseEvent.ProgressPayload.builder()
                        .completed(i + 1)
                        .total(totalMessages)
                        .percentage(percentage)
                        .build());
            }

            // 7. Update conversation as completed
            conversation.setTotalAudioDurationMs(totalAudioDuration);
            updateConversationStatus(conversation, ConversationStatus.COMPLETED);

            // 8. Send complete event
            long generationTime = System.currentTimeMillis() - startTime;
            sendEvent(emitter, "complete", ConversationSseEvent.CompletePayload.builder()
                    .conversationId(conversation.getId())
                    .totalMessages(completedCount)
                    .totalAudioDurationMs(totalAudioDuration)
                    .generationTimeMs(generationTime)
                    .build());

            emitter.complete();
            log.info("Conversation {} generation completed in {}ms with {} successful messages",
                    conversation.getId(), generationTime, completedCount);

        } catch (Exception e) {
            log.error("Conversation generation failed", e);
            if (conversation != null) {
                updateConversationStatus(conversation, ConversationStatus.FAILED);
            }
            try {
                sendEvent(emitter, "error", ConversationSseEvent.ErrorPayload.builder()
                        .message("Conversation generation failed: " + e.getMessage())
                        .code("GENERATION_ERROR")
                        .build());
                emitter.completeWithError(e);
            } catch (Exception ex) {
                log.error("Failed to send error event", ex);
            }
        }
    }

    /**
     * Helper record to associate a message with its speaker
     */
    private record MessageWithSpeaker(ConversationMessage message, ConversationSpeaker speaker) {}

    /**
     * Generate audio for a single message with rate limiting via semaphore
     */
    private AudioResult generateAudioForMessage(MessageWithSpeaker mws,
                                                 OpenAiTtsProperties.AccentType accent,
                                                 Long conversationId) {
        ConversationMessage message = mws.message();
        ConversationSpeaker speaker = mws.speaker();

        try {
            ttsSemaphore.acquire();
            try {
                log.debug("Generating audio for message {} (semaphore acquired)", message.getId());

                // Generate and upload audio
                String audioUrl = ttsService.generateAndUpload(
                        message.getText(),
                        speaker.getVoice(),
                        accent,
                        conversationId,
                        message.getId()
                );

                // Estimate duration
                int audioDuration = ttsService.estimateAudioDuration(message.getText());

                log.debug("Audio generated for message {}: url={}, duration={}ms",
                        message.getId(), audioUrl, audioDuration);

                return new AudioResult(message.getId(), audioUrl, audioDuration, true, null);

            } finally {
                ttsSemaphore.release();
                // Small delay after releasing semaphore to further space out API calls
                if (properties.getTtsDelayMs() > 0) {
                    Thread.sleep(properties.getTtsDelayMs());
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Audio generation interrupted for message {}", message.getId(), e);
            return new AudioResult(message.getId(), null, 0, false, "Interrupted: " + e.getMessage());
        } catch (Exception e) {
            log.error("Failed to generate audio for message {}", message.getId(), e);
            return new AudioResult(message.getId(), null, 0, false, e.getMessage());
        }
    }

    private void sendEvent(SseEmitter emitter, String eventName, Object data) throws IOException {
        ConversationSseEvent event = ConversationSseEvent.builder()
                .eventType(eventName)
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build();
        emitter.send(SseEmitter.event().name(eventName).data(event));
    }

    @Transactional
    protected Conversation createConversation(ConversationGenerateRequest request, Long userId) {
        Conversation conversation = Conversation.builder()
                .userId(userId)
                .prompt(request.getPrompt())
                .accent(request.getAccent().name())
                .durationMinutes(request.getDuration().getMinutes())
                .speakerCount(request.getSpeakerCount())
                .status(ConversationStatus.PENDING.getCode())
                .totalMessages(0)
                .totalAudioDurationMs(0L)
                .isDel("N")
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();
        save(conversation);
        return conversation;
    }

    @Transactional
    protected Map<Integer, ConversationSpeaker> saveSpeakers(Long conversationId,
                                                              List<ConversationScriptService.Speaker> scriptSpeakers,
                                                              List<String> voices) {
        Map<Integer, ConversationSpeaker> speakerMap = new HashMap<>();

        for (int i = 0; i < scriptSpeakers.size(); i++) {
            ConversationScriptService.Speaker scriptSpeaker = scriptSpeakers.get(i);
            String voice = i < voices.size() ? voices.get(i) : voices.get(0);

            ConversationSpeaker speaker = ConversationSpeaker.builder()
                    .conversationId(conversationId)
                    .speakerIndex(scriptSpeaker.getIndex())
                    .name(scriptSpeaker.getName())
                    .voice(voice)
                    .createTime(LocalDateTime.now())
                    .build();
            speakerMapper.insert(speaker);
            speakerMap.put(scriptSpeaker.getIndex(), speaker);
        }

        return speakerMap;
    }

    @Transactional
    protected ConversationMessage createMessage(Long conversationId, Long speakerId,
                                                 int sequence, String text) {
        ConversationMessage message = ConversationMessage.builder()
                .conversationId(conversationId)
                .speakerId(speakerId)
                .sequence(sequence)
                .text(text)
                .audioStatus(AudioStatus.PENDING.getCode())
                .audioDurationMs(0)
                .createTime(LocalDateTime.now())
                .build();
        messageMapper.insert(message);
        return message;
    }

    private void updateConversationStatus(Conversation conversation, ConversationStatus status) {
        conversation.setStatus(status.getCode());
        conversation.setUpdateTime(LocalDateTime.now());
        updateById(conversation);
    }

    /**
     * Get conversation by ID with full details
     */
    @Cacheable(value = CACHE_NAME, key = "'id:' + #id")
    public ConversationVO getConversationById(Long id, Long userId) {
        Conversation conversation = getById(id);
        if (conversation == null || "Y".equals(conversation.getIsDel())) {
            return null;
        }

        // Verify ownership
        if (!conversation.getUserId().equals(userId)) {
            throw new ServiceException("Access denied");
        }

        // Load speakers
        List<ConversationSpeaker> speakers = speakerMapper.selectList(
                new LambdaQueryWrapper<ConversationSpeaker>()
                        .eq(ConversationSpeaker::getConversationId, id)
                        .orderByAsc(ConversationSpeaker::getSpeakerIndex)
        );
        conversation.setSpeakers(speakers);

        // Load messages
        List<ConversationMessage> messages = messageMapper.selectList(
                new LambdaQueryWrapper<ConversationMessage>()
                        .eq(ConversationMessage::getConversationId, id)
                        .orderByAsc(ConversationMessage::getSequence)
        );

        // Attach speaker names to messages
        Map<Long, String> speakerNames = speakers.stream()
                .collect(Collectors.toMap(ConversationSpeaker::getId, ConversationSpeaker::getName));
        messages.forEach(m -> m.setSpeakerName(speakerNames.get(m.getSpeakerId())));

        conversation.setMessages(messages);

        return ConversationVO.fromEntity(conversation);
    }

    /**
     * List user's conversations
     */
    public List<ConversationVO> listUserConversations(Long userId, Boolean favoritedOnly) {
        LambdaQueryWrapper<Conversation> query = new LambdaQueryWrapper<Conversation>()
                .eq(Conversation::getUserId, userId)
                .eq(Conversation::getIsDel, "N")
                .orderByDesc(Conversation::getCreateTime);

        if (Boolean.TRUE.equals(favoritedOnly)) {
            query.eq(Conversation::getFavorited, true);
        }

        List<Conversation> conversations = list(query);
        return conversations.stream()
                .map(ConversationVO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Delete conversation (soft delete)
     */
    @CacheEvict(value = CACHE_NAME, key = "'id:' + #id")
    @Transactional
    public void deleteConversation(Long id, Long userId) {
        Conversation conversation = getById(id);
        if (conversation == null) {
            throw new ServiceException("Conversation not found");
        }

        // Verify ownership
        if (!conversation.getUserId().equals(userId)) {
            throw new ServiceException("Access denied");
        }

        conversation.setIsDel("Y");
        conversation.setUpdateTime(LocalDateTime.now());
        updateById(conversation);
    }

    /**
     * Toggle favorite status for a conversation
     */
    @CacheEvict(value = CACHE_NAME, key = "'id:' + #id")
    @Transactional
    public Boolean toggleFavorite(Long id, Long userId) {
        Conversation conversation = getById(id);
        if (conversation == null || "Y".equals(conversation.getIsDel())) {
            throw new ServiceException("Conversation not found");
        }

        if (!conversation.getUserId().equals(userId)) {
            throw new ServiceException("Access denied");
        }

        conversation.toggleFavorite();
        conversation.setUpdateTime(LocalDateTime.now());
        updateById(conversation);

        return conversation.getFavorited();
    }

    /**
     * Get message audio URL - regenerates if not ready (except when already generating)
     */
    public String getMessageAudioUrl(Long conversationId, Long messageId, Long userId) {
        // Verify conversation ownership
        Conversation conversation = getById(conversationId);
        if (conversation == null || !conversation.getUserId().equals(userId)) {
            throw new ServiceException("Access denied");
        }

        ConversationMessage message = messageMapper.selectById(messageId);
        if (message == null || !message.getConversationId().equals(conversationId)) {
            throw new ServiceException("Message not found");
        }

        AudioStatus currentStatus = message.getAudioStatusEnum();

        // If audio is ready, return the URL
        if (currentStatus == AudioStatus.READY) {
            return message.getAudioUrl();
        }

        // If audio is currently being generated, tell client to wait
        if (currentStatus == AudioStatus.GENERATING) {
            throw new ServiceException("Audio is being generated, please try again shortly");
        }

        // For PENDING or FAILED status, regenerate the audio
        log.info("Regenerating audio for message {} (status: {})", messageId, currentStatus);
        return regenerateMessageAudio(conversation, message);
    }

    /**
     * Regenerate audio for a message
     */
    private String regenerateMessageAudio(Conversation conversation, ConversationMessage message) {
        // Get speaker info for voice
        ConversationSpeaker speaker = speakerMapper.selectById(message.getSpeakerId());
        if (speaker == null) {
            throw new ServiceException("Speaker not found");
        }

        // Mark as generating
        message.setAudioStatusEnum(AudioStatus.GENERATING);
        messageMapper.updateById(message);

        try {
            // Get accent from conversation
            OpenAiTtsProperties.AccentType accent = OpenAiTtsProperties.AccentType.fromCode(conversation.getAccent());

            // Generate and upload audio
            String audioUrl = ttsService.generateAndUpload(
                    message.getText(),
                    speaker.getVoice(),
                    accent,
                    conversation.getId(),
                    message.getId()
            );

            if (audioUrl == null) {
                throw new ServiceException("Failed to generate audio");
            }

            // Estimate duration
            int audioDuration = ttsService.estimateAudioDuration(message.getText());

            // Update message with audio info
            message.setAudioUrl(audioUrl);
            message.setAudioDurationMs(audioDuration);
            message.setAudioStatusEnum(AudioStatus.READY);
            messageMapper.updateById(message);

            log.info("Successfully regenerated audio for message {}", message.getId());
            return audioUrl;

        } catch (Exception e) {
            log.error("Failed to regenerate audio for message {}", message.getId(), e);
            message.setAudioStatusEnum(AudioStatus.FAILED);
            messageMapper.updateById(message);
            throw new ServiceException("Failed to regenerate audio: " + e.getMessage());
        }
    }
}
