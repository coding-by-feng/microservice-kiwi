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
package me.fengorz.kiwi.service;

import me.fengorz.kiwi.BaseIntegrationTest;
import me.fengorz.kiwi.common.enumeration.AiPromptModeEnum;
import me.fengorz.kiwi.common.enumeration.LanguageEnum;
import me.fengorz.kiwi.domain.ai.service.AiChatService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Real API Tests for Grok AI Service
 *
 * These tests actually call the Grok AI API. They are disabled by default
 * and only run when the TEST_GROK_API_KEY environment variable is set.
 *
 * To run these tests:
 * 1. Set the environment variable: export TEST_GROK_API_KEY=your-actual-api-key
 * 2. Run: mvn test -Dtest=GrokAiServiceRealApiTest -Dspring.profiles.active=integration
 *
 * @author codingByFeng
 */
@DisplayName("Grok AI Service - Real API Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@EnabledIfEnvironmentVariable(named = "TEST_GROK_API_KEY", matches = ".+")
class GrokAiServiceRealApiTest extends BaseIntegrationTest {

    @Autowired
    @Qualifier("grokAiService")
    private AiChatService aiChatService;

    @Test
    @Order(1)
    @DisplayName("Should translate text using Grok API")
    void call_DirectTranslation_ShouldReturnTranslation() {
        // Given
        String prompt = "Hello, how are you today?";

        // When
        String result = aiChatService.call(prompt, AiPromptModeEnum.DIRECTLY_TRANSLATION, LanguageEnum.ZH_CN);

        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());
        System.out.println("Translation result: " + result);
    }

    @Test
    @Order(2)
    @DisplayName("Should explain vocabulary using Grok API")
    void call_VocabularyExplanation_ShouldReturnExplanation() {
        // Given
        String prompt = "ubiquitous";

        // When
        String result = aiChatService.call(prompt, AiPromptModeEnum.VOCABULARY_EXPLANATION,
            LanguageEnum.EN, LanguageEnum.ZH_CN);

        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());
        System.out.println("Vocabulary explanation: " + result);
    }

    @Test
    @Order(3)
    @DisplayName("Should batch translate multiple prompts")
    void batchCall_ShouldReturnBatchTranslations() {
        // Given
        List<String> prompts = Arrays.asList(
            "Good morning",
            "How are you?",
            "Thank you very much"
        );

        // When
        String result = aiChatService.batchCall(prompts,
            AiPromptModeEnum.DIRECTLY_TRANSLATION, LanguageEnum.ZH_CN);

        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());
        System.out.println("Batch translation result: " + result);
    }

    @Test
    @Order(4)
    @DisplayName("Should get synonyms using Grok API")
    void call_Synonym_ShouldReturnSynonyms() {
        // Given
        String prompt = "happy";

        // When
        String result = aiChatService.call(prompt, AiPromptModeEnum.SYNONYM,
            LanguageEnum.EN, LanguageEnum.ZH_CN);

        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());
        System.out.println("Synonyms: " + result);
    }

    @Test
    @Order(5)
    @DisplayName("Should correct grammar using Grok API")
    void call_GrammarCorrection_ShouldReturnCorrection() {
        // Given
        String prompt = "I goed to the store yesterday and buyed some apples.";

        // When
        String result = aiChatService.call(prompt, AiPromptModeEnum.GRAMMAR_CORRECTION, LanguageEnum.EN);

        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());
        System.out.println("Grammar correction: " + result);
    }
}
