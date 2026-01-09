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

import me.fengorz.kiwi.BaseIntegrationTest;
import me.fengorz.kiwi.domain.word.entity.WordMain;
import me.fengorz.kiwi.domain.word.service.WordMainService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration Tests for WordMainController
 *
 * These tests connect to a real test database and test the complete
 * request-response cycle including database operations.
 *
 * @author codingByFeng
 */
@DisplayName("Word Main Controller Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class WordMainControllerIntegrationTest extends BaseIntegrationTest {

    private static final String API_BASE = "/api/word";

    @Autowired
    private WordMainService wordMainService;

    private WordMain testWord;

    @BeforeEach
    void setUpTestData() {
        // Use existing word from database - we can't create new ones since word_id is not auto-increment
        testWord = wordMainService.findByWordName("test")
            .orElseGet(() -> wordMainService.list().stream().findFirst().orElse(null));
        assertNotNull(testWord, "Test word must exist in database");
    }

    @Test
    @Order(1)
    @DisplayName("GET /api/word/{wordId} - Should return word by ID")
    void getWordById_ShouldReturnWord() throws Exception {
        mockMvc.perform(get(API_BASE + "/" + testWord.getWordId())
                .contentType(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.wordName").value(testWord.getWordName()));
    }

    @Test
    @Order(2)
    @DisplayName("GET /api/word/{wordId} - Should return 404 for non-existent word")
    void getWordById_NotFound_ShouldReturnError() throws Exception {
        mockMvc.perform(get(API_BASE + "/999999")
                .contentType(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(1))
            .andExpect(jsonPath("$.msg").value("Word not found"));
    }

    @Test
    @Order(3)
    @DisplayName("GET /api/word/name/{wordName} - Should return word by name")
    void getWordByName_ShouldReturnWord() throws Exception {
        mockMvc.perform(get(API_BASE + "/name/integration-test-word")
                .contentType(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.wordId").value(testWord.getWordId()));
    }

    @Test
    @Order(4)
    @DisplayName("GET /api/word - Should return paginated word list")
    void listWords_ShouldReturnPaginatedList() throws Exception {
        mockMvc.perform(get(API_BASE)
                .param("page", "0")
                .param("size", "10")
                .contentType(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.content").isArray())
            .andExpect(jsonPath("$.data.totalElements").isNumber());
    }

    @Test
    @Order(5)
    @DisplayName("GET /api/word/search - Should search words by query")
    void searchWords_ShouldReturnMatchingWords() throws Exception {
        mockMvc.perform(get(API_BASE + "/search")
                .param("query", "integration")
                .param("page", "0")
                .param("size", "10")
                .contentType(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.content[*].wordName", hasItem(containsString("integration"))));
    }

    @Test
    @Order(6)
    @DisplayName("GET /api/word/autocomplete - Should return autocomplete suggestions")
    void autocomplete_ShouldReturnSuggestions() throws Exception {
        mockMvc.perform(get(API_BASE + "/autocomplete")
                .param("prefix", "integ")
                .contentType(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @Order(7)
    @DisplayName("GET /api/word/exists/{wordName} - Should check if word exists")
    void checkWordExists_ShouldReturnTrue() throws Exception {
        mockMvc.perform(get(API_BASE + "/exists/integration-test-word")
                .contentType(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    @Order(8)
    @DisplayName("GET /api/word/exists/{wordName} - Should return false for non-existent word")
    void checkWordExists_NotFound_ShouldReturnFalse() throws Exception {
        mockMvc.perform(get(API_BASE + "/exists/non-existent-word-xyz123")
                .contentType(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data").value(false));
    }

    @Test
    @Order(9)
    @DisplayName("POST /api/word - Should create new word")
    @Disabled("word_id is not auto-increment in legacy database - cannot create new words")
    void createWord_ShouldCreateAndReturnWord() throws Exception {
        // Disabled because word_id is not auto-increment
    }

    @Test
    @Order(10)
    @DisplayName("POST /api/word - Should fail for duplicate word")
    void createWord_Duplicate_ShouldFail() throws Exception {
        WordMain duplicateWord = new WordMain();
        duplicateWord.setWordName(testWord.getWordName());  // Already exists
        duplicateWord.setInfoType(0);

        mockMvc.perform(post(API_BASE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(duplicateWord)))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(1))
            .andExpect(jsonPath("$.msg").value("Word already exists"));
    }

    @Test
    @Order(11)
    @DisplayName("PUT /api/word/{wordId} - Should update existing word")
    void updateWord_ShouldUpdateAndReturnWord() throws Exception {
        testWord.setInfoType(1);

        mockMvc.perform(put(API_BASE + "/" + testWord.getWordId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(testWord)))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.infoType").value(1));
    }

    @Test
    @Order(12)
    @DisplayName("PUT /api/word/{wordId} - Should fail for non-existent word")
    void updateWord_NotFound_ShouldFail() throws Exception {
        WordMain nonExistentWord = new WordMain();
        nonExistentWord.setWordName("non-existent");
        nonExistentWord.setInfoType(0);

        mockMvc.perform(put(API_BASE + "/999999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(nonExistentWord)))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(1))
            .andExpect(jsonPath("$.msg").value("Word not found"));
    }

    @Test
    @Order(13)
    @DisplayName("GET /api/word/fuzzy - Should perform fuzzy search")
    void fuzzySearch_ShouldReturnResults() throws Exception {
        mockMvc.perform(get(API_BASE + "/fuzzy")
                .param("query", "test")
                .param("page", "0")
                .param("size", "10")
                .contentType(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @Order(14)
    @DisplayName("POST /api/word/cache/evict/{wordName} - Should evict cache")
    void evictCache_ShouldSucceed() throws Exception {
        mockMvc.perform(post(API_BASE + "/cache/evict/integration-test-word")
                .contentType(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @Order(100)  // Run last
    @DisplayName("DELETE /api/word/{wordId} - Should delete word")
    @Disabled("word_id is not auto-increment in legacy database - cannot create words to delete")
    void deleteWord_ShouldSucceed() throws Exception {
        // Disabled because we cannot create words to delete
    }
}
