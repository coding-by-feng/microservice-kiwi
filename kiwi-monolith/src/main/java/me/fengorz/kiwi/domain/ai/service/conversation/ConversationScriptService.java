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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.enumeration.AiPromptModeEnum;
import me.fengorz.kiwi.common.enumeration.LanguageEnum;
import me.fengorz.kiwi.common.exception.ServiceException;
import me.fengorz.kiwi.domain.ai.config.ConversationProperties;
import me.fengorz.kiwi.domain.ai.dto.conversation.DurationOption;
import me.fengorz.kiwi.domain.ai.dto.conversation.TopicGenerationRequest;
import me.fengorz.kiwi.domain.ai.service.AiChatService;
import me.fengorz.kiwi.domain.ai.vo.conversation.TopicGenerationVO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Service for generating conversation scripts using AI
 *
 * @author codingByFeng
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationScriptService {

    @Qualifier("aiChatService")
    private final AiChatService aiChatService;
    private final ConversationProperties conversationProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String PROMPT_TEMPLATE = """
            Generate a natural English conversation between %d people about: %s

            Requirements:
            - CRITICAL: Generate exactly %d to %d messages total (this is required for %d-minute conversation)
            - Each speaker should have a distinct personality and speaking style
            - Include natural conversational elements (greetings, reactions, follow-ups, questions)
            - Make the dialogue feel authentic and engaging
            - Each message should be 1-3 sentences (suitable for TTS audio, ~10 seconds per message)
            - IMPORTANT: For role-play scenarios, use the character/role name as the speaker name (e.g., "Mrs. Henderson" not "Chloe playing Mrs. Henderson")
            - The speaker name must be exactly the same name used when characters address each other in the conversation

            IMPORTANT: Output ONLY valid JSON in this exact format, no other text:
            {
              "topic": "A refined, concise topic title",
              "speakers": [
                {"index": 0, "name": "CharacterName", "personality": "brief personality description"},
                {"index": 1, "name": "CharacterName", "personality": "brief personality description"}
              ],
              "messages": [
                {"speakerIndex": 0, "text": "Message text here"},
                {"speakerIndex": 1, "text": "Response text here"}
              ]
            }
            """;

    /**
     * Generate a conversation script based on the given parameters
     *
     * @param prompt       topic description
     * @param duration     target duration
     * @param speakerCount number of speakers
     * @return generated conversation script
     */
    public ConversationScript generateScript(String prompt, DurationOption duration, int speakerCount) {
        log.info("Generating conversation script for topic: {}, duration: {} min ({}-{} messages), speakers: {}",
                prompt, duration.getMinutes(), duration.getMinMessages(), duration.getMaxMessages(), speakerCount);

        String formattedPrompt = String.format(PROMPT_TEMPLATE,
                speakerCount,
                prompt,
                duration.getMinMessages(),
                duration.getMaxMessages(),
                duration.getMinutes());

        try {
            String aiResponse = aiChatService.call(formattedPrompt, AiPromptModeEnum.CONVERSATION_GENERATION, LanguageEnum.EN);

            log.debug("AI response: {}", aiResponse);

            return parseScript(aiResponse);
        } catch (Exception e) {
            log.error("Failed to generate conversation script", e);
            throw new ServiceException("Failed to generate conversation script: " + e.getMessage());
        }
    }

    /**
     * Generate a conversation topic based on custom prompt or category
     *
     * @param request topic generation request
     * @return generated topic with suggestions
     */
    public TopicGenerationVO generateTopic(TopicGenerationRequest request) {
        String topicInput;
        if (StringUtils.hasText(request.getPrompt())) {
            // Use custom prompt
            topicInput = "User's topic idea: " + request.getPrompt();
            log.info("Generating topic from custom prompt: {}", request.getPrompt());
        } else {
            // Generate random topic from category
            topicInput = "Generate a random topic about: " + request.getCategory().getDescription();
            log.info("Generating random topic for category: {}", request.getCategory());
        }

        String formattedPrompt = String.format(conversationProperties.getTopicGenerationPrompt(),
                topicInput,
                request.getDifficulty().getDescription());

        try {
            String aiResponse = aiChatService.call(formattedPrompt, AiPromptModeEnum.CONVERSATION_GENERATION, LanguageEnum.EN);
            log.debug("Topic generation AI response: {}", aiResponse);

            return parseTopicResponse(aiResponse, request);
        } catch (Exception e) {
            log.error("Failed to generate conversation topic", e);
            throw new ServiceException("Failed to generate conversation topic: " + e.getMessage());
        }
    }

    private TopicGenerationVO parseTopicResponse(String aiResponse, TopicGenerationRequest request) {
        try {
            String jsonContent = extractJson(aiResponse);
            JsonNode root = objectMapper.readTree(jsonContent);

            List<String> keywords = new ArrayList<>();
            JsonNode keywordsNode = root.path("keywords");
            if (keywordsNode.isArray()) {
                for (JsonNode keyword : keywordsNode) {
                    keywords.add(keyword.asText());
                }
            }

            return TopicGenerationVO.builder()
                    .topic(root.path("topic").asText("A conversation practice scenario"))
                    .category(request.getCategory().name())
                    .difficulty(request.getDifficulty().name())
                    .suggestedSpeakerCount(root.path("suggestedSpeakerCount").asInt(2))
                    .suggestedDuration(root.path("suggestedDuration").asText("FIVE_MINUTES"))
                    .keywords(keywords)
                    .build();
        } catch (JsonProcessingException e) {
            log.error("Failed to parse topic generation response: {}", aiResponse, e);
            throw new ServiceException("Failed to parse topic generation response: Invalid JSON format");
        }
    }

    private ConversationScript parseScript(String aiResponse) {
        try {
            // Extract JSON from the response (in case there's extra text)
            String jsonContent = extractJson(aiResponse);

            JsonNode root = objectMapper.readTree(jsonContent);

            ConversationScript script = new ConversationScript();
            script.setTopic(root.path("topic").asText("Untitled Conversation"));

            // Parse speakers
            List<Speaker> speakers = new ArrayList<>();
            JsonNode speakersNode = root.path("speakers");
            if (speakersNode.isArray()) {
                for (JsonNode speakerNode : speakersNode) {
                    Speaker speaker = Speaker.builder()
                            .index(speakerNode.path("index").asInt())
                            .name(speakerNode.path("name").asText())
                            .personality(speakerNode.path("personality").asText())
                            .build();
                    speakers.add(speaker);
                }
            }
            script.setSpeakers(speakers);

            // Parse messages
            List<Message> messages = new ArrayList<>();
            JsonNode messagesNode = root.path("messages");
            if (messagesNode.isArray()) {
                int sequence = 1;
                for (JsonNode messageNode : messagesNode) {
                    Message message = Message.builder()
                            .speakerIndex(messageNode.path("speakerIndex").asInt())
                            .text(messageNode.path("text").asText())
                            .sequence(sequence++)
                            .build();
                    messages.add(message);
                }
            }
            script.setMessages(messages);

            log.info("Successfully parsed conversation script with {} speakers and {} messages",
                    speakers.size(), messages.size());

            return script;
        } catch (JsonProcessingException e) {
            log.error("Failed to parse AI response as JSON: {}", aiResponse, e);
            throw new ServiceException("Failed to parse conversation script: Invalid JSON format");
        }
    }

    private String extractJson(String response) {
        // Find the first { and last } to extract JSON
        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return response.substring(start, end + 1);
        }
        return response;
    }

    /**
     * Conversation script data structure
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConversationScript {
        private String topic;
        private List<Speaker> speakers;
        private List<Message> messages;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Speaker {
        private int index;
        private String name;
        private String personality;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Message {
        private int speakerIndex;
        private String text;
        private int sequence;
    }
}
