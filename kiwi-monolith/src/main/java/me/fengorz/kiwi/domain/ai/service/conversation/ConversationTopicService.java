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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.enumeration.AiPromptModeEnum;
import me.fengorz.kiwi.common.enumeration.LanguageEnum;
import me.fengorz.kiwi.common.exception.ServiceException;
import me.fengorz.kiwi.domain.ai.dto.conversation.ConversationTopicRequest;
import me.fengorz.kiwi.domain.ai.dto.conversation.DurationOption;
import me.fengorz.kiwi.domain.ai.dto.conversation.TopicCategory;
import me.fengorz.kiwi.domain.ai.dto.conversation.TopicDifficulty;
import me.fengorz.kiwi.domain.ai.service.AiChatService;
import me.fengorz.kiwi.domain.ai.vo.conversation.ConversationTopicVO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Service for generating random conversation topics using AI
 *
 * @author codingByFeng
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationTopicService {

    @Qualifier("aiChatService")
    private final AiChatService aiChatService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Random random = new Random();

    private static final String PROMPT_TEMPLATE = """
            Generate a creative and engaging conversation topic for language learning practice.

            Requirements:
            - Category: %s (%s)
            - Difficulty: %s (%s)
            - Language: %s
            - The topic should be suitable for a conversation between 2-4 people
            - Include a scenario or context that makes the conversation natural
            - Provide 4-6 related vocabulary keywords that learners might use

            IMPORTANT: Output ONLY valid JSON in this exact format, no other text:
            {
              "topic": "A descriptive conversation topic/scenario",
              "suggestedSpeakerCount": 2,
              "suggestedDuration": "FIVE_MINUTES",
              "keywords": ["keyword1", "keyword2", "keyword3", "keyword4"]
            }

            Notes for suggestedDuration: Use "TWO_MINUTES" for simple topics, "FIVE_MINUTES" for moderate topics, "TEN_MINUTES" for complex discussions.
            Notes for suggestedSpeakerCount: Use 2 for personal conversations, 3-4 for group discussions or scenarios with multiple roles.
            """;

    private static final String CUSTOM_PROMPT_TEMPLATE = """
            Based on the user's idea, generate a refined conversation topic for language learning practice.

            User's idea: %s

            Requirements:
            - Difficulty: %s (%s)
            - Language: %s
            - Expand and refine the user's idea into a specific, engaging scenario
            - The topic should be suitable for a conversation between 2-4 people
            - Provide 4-6 related vocabulary keywords that learners might use

            IMPORTANT: Output ONLY valid JSON in this exact format, no other text:
            {
              "topic": "A descriptive conversation topic/scenario based on the user's idea",
              "suggestedSpeakerCount": 2,
              "suggestedDuration": "FIVE_MINUTES",
              "keywords": ["keyword1", "keyword2", "keyword3", "keyword4"]
            }

            Notes for suggestedDuration: Use "TWO_MINUTES" for simple topics, "FIVE_MINUTES" for moderate topics, "TEN_MINUTES" for complex discussions.
            Notes for suggestedSpeakerCount: Use 2 for personal conversations, 3-4 for group discussions or scenarios with multiple roles.
            """;

    /**
     * Generate a random conversation topic based on the request parameters
     *
     * @param request the topic generation request
     * @return generated conversation topic
     */
    public ConversationTopicVO generateRandomTopic(ConversationTopicRequest request) {
        TopicCategory category = request.getCategory() != null ? request.getCategory() : TopicCategory.LIFESTYLE;
        TopicDifficulty difficulty = request.getDifficulty() != null ? request.getDifficulty() : TopicDifficulty.INTERMEDIATE;
        String language = request.getLanguage() != null ? request.getLanguage() : "en";
        String customPrompt = request.getPrompt();

        String formattedPrompt;
        if (customPrompt != null && !customPrompt.isBlank()) {
            log.info("Generating topic from custom prompt: {}, difficulty: {}, language: {}",
                    customPrompt.substring(0, Math.min(50, customPrompt.length())), difficulty.getCode(), language);

            formattedPrompt = String.format(CUSTOM_PROMPT_TEMPLATE,
                    customPrompt,
                    difficulty.getCode(),
                    difficulty.getDescription(),
                    language);
        } else {
            log.info("Generating random topic for category: {}, difficulty: {}, language: {}",
                    category.getCode(), difficulty.getCode(), language);

            formattedPrompt = String.format(PROMPT_TEMPLATE,
                    category.getCode(),
                    category.getDescription(),
                    difficulty.getCode(),
                    difficulty.getDescription(),
                    language);
        }

        try {
            LanguageEnum languageEnum = LanguageEnum.fromCode(language);
            String aiResponse = aiChatService.call(formattedPrompt, AiPromptModeEnum.TOPIC_GENERATION, languageEnum);

            log.debug("AI response: {}", aiResponse);

            return parseTopicResponse(aiResponse, category, difficulty);
        } catch (Exception e) {
            log.error("Failed to generate conversation topic", e);
            throw new ServiceException("Failed to generate conversation topic: " + e.getMessage());
        }
    }

    private ConversationTopicVO parseTopicResponse(String aiResponse, TopicCategory category, TopicDifficulty difficulty) {
        try {
            String jsonContent = extractJson(aiResponse);
            JsonNode root = objectMapper.readTree(jsonContent);

            String topic = root.path("topic").asText("A casual conversation about daily life");
            int speakerCount = root.path("suggestedSpeakerCount").asInt(2);
            String duration = root.path("suggestedDuration").asText("FIVE_MINUTES");

            // Parse keywords
            List<String> keywords = new ArrayList<>();
            JsonNode keywordsNode = root.path("keywords");
            if (keywordsNode.isArray()) {
                for (JsonNode keyword : keywordsNode) {
                    keywords.add(keyword.asText());
                }
            }

            // Validate and adjust values
            speakerCount = Math.max(2, Math.min(4, speakerCount));
            duration = validateDuration(duration);

            log.info("Successfully parsed topic: {} with {} speakers, {} duration",
                    topic.substring(0, Math.min(50, topic.length())), speakerCount, duration);

            return ConversationTopicVO.builder()
                    .topic(topic)
                    .category(category.getCode())
                    .difficulty(difficulty.getCode())
                    .suggestedSpeakerCount(speakerCount)
                    .suggestedDuration(duration)
                    .keywords(keywords)
                    .build();

        } catch (JsonProcessingException e) {
            log.error("Failed to parse AI response as JSON: {}", aiResponse, e);
            throw new ServiceException("Failed to parse topic response: Invalid JSON format");
        }
    }

    private String extractJson(String response) {
        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return response.substring(start, end + 1);
        }
        return response;
    }

    private String validateDuration(String duration) {
        try {
            DurationOption.valueOf(duration);
            return duration;
        } catch (IllegalArgumentException e) {
            return "FIVE_MINUTES";
        }
    }
}
