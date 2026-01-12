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

import me.fengorz.kiwi.BaseIntegrationTest;
import me.fengorz.kiwi.domain.ai.entity.AiCallHistory;
import me.fengorz.kiwi.domain.ai.service.AiCallHistoryService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration Tests for AI Controller
 *
 * Tests the AI-related API endpoints.
 *
 * @author codingByFeng
 */
@DisplayName("AI Controller Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AiControllerIntegrationTest extends BaseIntegrationTest {

    private static final String API_BASE = "/api/ai";
    private static final String HISTORY_API_BASE = "/api/ai/history";

    @Autowired
    private AiCallHistoryService historyService;

    private AiCallHistory testHistory;

    @BeforeEach
    void setUpTestData() {
        // Create test AI call history record
        testHistory = historyService.list().stream()
            .filter(h -> "integration-test".equals(h.getPromptMode()))
            .findFirst()
            .orElseGet(() -> {
                AiCallHistory history = new AiCallHistory();
                history.setUserId(1L);
                history.setPromptMode("integration-test");
                history.setPrompt("Test prompt");
                history.setAiUrl("https://api.x.ai/v1/chat/completions");
                history.setTargetLanguage("ZH_CN");
                history.setNativeLanguage("EN");
                history.setCreateTime(LocalDateTime.now());
                historyService.save(history);
                return history;
            });
    }

    @Test
    @Order(1)
    @DisplayName("GET /api/ai/history - Should return AI call history list")
    void getAiCallHistory_ShouldReturnList() throws Exception {
        mockMvc.perform(get(HISTORY_API_BASE)
                .param("page", "0")
                .param("size", "10")
                .contentType(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @Order(2)
    @DisplayName("GET /api/ai/history/{id} - Should return specific history record")
    void getAiCallHistoryById_ShouldReturnRecord() throws Exception {
        mockMvc.perform(get(HISTORY_API_BASE + "/" + testHistory.getId())
                .contentType(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.promptMode").value("integration-test"));
    }

    @Test
    @Order(3)
    @DisplayName("GET /api/ai/history/user/{userId} - Should return user's AI call history")
    void getAiCallHistoryByUserId_ShouldReturnUserHistory() throws Exception {
        mockMvc.perform(get(HISTORY_API_BASE + "/user/1")
                .param("page", "0")
                .param("size", "10")
                .contentType(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @Order(4)
    @DisplayName("POST /api/ai/history - Should create new AI call history record")
    void createAiCallHistory_ShouldCreateRecord() throws Exception {
        AiCallHistory newHistory = new AiCallHistory();
        newHistory.setUserId(1L);
        newHistory.setPromptMode("test-create");
        newHistory.setPrompt("New test prompt");
        newHistory.setAiUrl("https://api.x.ai/v1/chat/completions");
        newHistory.setTargetLanguage("ZH_CN");

        mockMvc.perform(post(HISTORY_API_BASE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(newHistory)))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.promptMode").value("test-create"));
    }

    @Test
    @Order(100)
    @DisplayName("DELETE /api/ai/history/{id} - Should delete AI call history record")
    void deleteAiCallHistory_ShouldSucceed() throws Exception {
        // Create a record specifically for deletion
        AiCallHistory historyToDelete = new AiCallHistory();
        historyToDelete.setUserId(1L);
        historyToDelete.setPromptMode("to-delete");
        historyToDelete.setPrompt("Delete this prompt");
        historyToDelete.setAiUrl("https://api.x.ai/v1/chat/completions");
        historyToDelete.setTargetLanguage("ZH_CN");
        historyToDelete.setCreateTime(LocalDateTime.now());
        historyService.save(historyToDelete);

        mockMvc.perform(delete(HISTORY_API_BASE + "/" + historyToDelete.getId())
                .contentType(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0));
    }
}
