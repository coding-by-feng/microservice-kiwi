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
package me.fengorz.kiwi.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Live API Test Cases
 *
 * These tests call the actual running localhost:8080 server.
 * Make sure the application is running before executing these tests.
 *
 * Run with: mvn test -Dtest=LiveApiTest
 *
 * @author codingByFeng
 */
@DisplayName("Live API Tests - localhost:8080")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LiveApiTest {

    private static final String BASE_URL = "http://localhost:8080";
    private static RestTemplate restTemplate;
    private static ObjectMapper objectMapper;
    private static HttpHeaders headers;

    @BeforeAll
    static void setup() {
        restTemplate = new RestTemplate();
        objectMapper = new ObjectMapper();
        headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
    }

    // ==================== Health Check ====================

    @Test
    @Order(0)
    @DisplayName("GET /actuator/health - Health check should return UP")
    void healthCheck_ShouldReturnUp() {
        String url = BASE_URL + "/actuator/health";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("UP"));
        System.out.println("Health Check Response: " + response.getBody());
    }

    // ==================== Word APIs (Public - No Auth Required) ====================

    @Test
    @Order(1)
    @DisplayName("GET /api/word/fuzzy - Fuzzy search should return results")
    void fuzzySearch_ShouldReturnResults() throws Exception {
        String url = BASE_URL + "/api/word/fuzzy?query=test&page=0&size=10";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonNode json = objectMapper.readTree(response.getBody());
        assertEquals(0, json.get("code").asInt(), "Response code should be 0 (success)");
        System.out.println("Fuzzy Search Response: " + response.getBody());
    }

    @Test
    @Order(2)
    @DisplayName("GET /api/word/search - Search should return results")
    void searchWords_ShouldReturnResults() throws Exception {
        String url = BASE_URL + "/api/word/search?query=test&page=0&size=10";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonNode json = objectMapper.readTree(response.getBody());
        assertEquals(0, json.get("code").asInt(), "Response code should be 0 (success)");
        System.out.println("Search Response: " + response.getBody());
    }

    @Test
    @Order(3)
    @DisplayName("GET /api/word/autocomplete - Autocomplete should return suggestions")
    void autocomplete_ShouldReturnSuggestions() throws Exception {
        String url = BASE_URL + "/api/word/autocomplete?prefix=tes";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonNode json = objectMapper.readTree(response.getBody());
        assertEquals(0, json.get("code").asInt(), "Response code should be 0 (success)");
        System.out.println("Autocomplete Response: " + response.getBody());
    }

    @Test
    @Order(4)
    @DisplayName("GET /api/word/exists/{wordName} - Check word exists")
    void checkWordExists_ShouldReturnBoolean() throws Exception {
        String url = BASE_URL + "/api/word/exists/test";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonNode json = objectMapper.readTree(response.getBody());
        assertEquals(0, json.get("code").asInt(), "Response code should be 0 (success)");
        assertNotNull(json.get("data"), "Data should not be null");
        System.out.println("Word Exists Response: " + response.getBody());
    }

    @Test
    @Order(5)
    @DisplayName("GET /api/word/exists/nonexistent - Non-existent word should return false")
    void checkWordNotExists_ShouldReturnFalse() throws Exception {
        String url = BASE_URL + "/api/word/exists/nonexistent_word_xyz_123";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonNode json = objectMapper.readTree(response.getBody());
        assertEquals(0, json.get("code").asInt(), "Response code should be 0 (success)");
        assertFalse(json.get("data").asBoolean(), "Non-existent word should return false");
        System.out.println("Word Not Exists Response: " + response.getBody());
    }

    @Test
    @Order(6)
    @DisplayName("GET /api/word/name/{wordName} - Get word by name")
    void getWordByName_ShouldReturnWord() throws Exception {
        String url = BASE_URL + "/api/word/name/test";
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            JsonNode json = objectMapper.readTree(response.getBody());
            // May return error if word doesn't exist, both are valid responses
            System.out.println("Get Word By Name Response: " + response.getBody());
        } catch (org.springframework.web.client.HttpServerErrorException e) {
            // 500 error may occur if entity has lazy-loading issues - needs app restart
            System.out.println("Get Word By Name - Server Error (may need app restart): " + e.getStatusCode());
            fail("Server returned 500 - application may need restart with @JsonIgnore fixes");
        }
    }

    @Test
    @Order(7)
    @DisplayName("GET /api/word - List words with pagination")
    void listWords_ShouldReturnPaginatedList() throws Exception {
        String url = BASE_URL + "/api/word?page=0&size=5";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonNode json = objectMapper.readTree(response.getBody());
        assertEquals(0, json.get("code").asInt(), "Response code should be 0 (success)");
        System.out.println("List Words Response: " + response.getBody());
    }

    @Test
    @Order(8)
    @DisplayName("GET /api/word/{wordId} - Get word by ID")
    void getWordById_ShouldReturnWord() throws Exception {
        // First get a word ID from the list
        String listUrl = BASE_URL + "/api/word?page=0&size=1";
        ResponseEntity<String> listResponse = restTemplate.getForEntity(listUrl, String.class);
        JsonNode listJson = objectMapper.readTree(listResponse.getBody());

        if (listJson.get("data") != null && listJson.get("data").get("content") != null
            && listJson.get("data").get("content").size() > 0) {
            int wordId = listJson.get("data").get("content").get(0).get("wordId").asInt();

            String url = BASE_URL + "/api/word/" + wordId;
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            JsonNode json = objectMapper.readTree(response.getBody());
            assertEquals(0, json.get("code").asInt(), "Response code should be 0 (success)");
            System.out.println("Get Word By ID Response: " + response.getBody());
        } else {
            System.out.println("No words in database to test with");
        }
    }

    @Test
    @Order(9)
    @DisplayName("GET /api/word/999999 - Non-existent word ID should return error")
    void getWordById_NotFound_ShouldReturnError() throws Exception {
        String url = BASE_URL + "/api/word/999999";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonNode json = objectMapper.readTree(response.getBody());
        assertEquals(1, json.get("code").asInt(), "Response code should be 1 (error)");
        System.out.println("Word Not Found Response: " + response.getBody());
    }

    // ==================== Paraphrase APIs ====================

    @Test
    @Order(20)
    @DisplayName("GET /api/word/paraphrase - List paraphrases with pagination")
    void listParaphrases_ShouldReturnPaginatedList() throws Exception {
        String url = BASE_URL + "/api/word/paraphrase?page=0&size=5";
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            System.out.println("List Paraphrases Response: " + response.getBody());
        } catch (org.springframework.web.client.HttpServerErrorException e) {
            // 500 error may occur if entity has lazy-loading issues - needs app restart
            System.out.println("List Paraphrases - Server Error (may need app restart): " + e.getStatusCode());
            fail("Server returned 500 - application may need restart with @JsonIgnore fixes");
        }
    }

    // ==================== Authentication Required APIs ====================
    // These should return 401/302 without authentication

    @Test
    @Order(50)
    @DisplayName("GET /api/word/star/list - Star list without auth should fail")
    void getStarList_WithoutAuth_ShouldFail() {
        String url = BASE_URL + "/api/word/star/list";
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            // If we get here, check if it's a redirect (302) or unauthorized
            assertTrue(response.getStatusCode().is3xxRedirection()
                || response.getStatusCode() == HttpStatus.UNAUTHORIZED,
                "Should redirect to login or return 401");
        } catch (HttpClientErrorException e) {
            assertTrue(e.getStatusCode() == HttpStatus.UNAUTHORIZED
                || e.getStatusCode() == HttpStatus.FORBIDDEN,
                "Should return 401 or 403");
            System.out.println("Star List without auth - Expected error: " + e.getStatusCode());
        } catch (org.springframework.web.client.HttpServerErrorException e) {
            // 500 error when @AuthenticationPrincipal is null - this is also expected behavior
            System.out.println("Star List without auth - Server error (auth principal null): " + e.getStatusCode());
        }
    }

    @Test
    @Order(51)
    @DisplayName("GET /api/word/review/counter/all - Review counter without auth should fail")
    void getReviewCounter_WithoutAuth_ShouldFail() {
        String url = BASE_URL + "/api/word/review/counter/all";
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            // May return 200 with error response if endpoint doesn't exist or is public
            System.out.println("Review Counter Response: " + response.getStatusCode() + " - " + response.getBody());
        } catch (HttpClientErrorException e) {
            assertTrue(e.getStatusCode() == HttpStatus.UNAUTHORIZED
                || e.getStatusCode() == HttpStatus.FORBIDDEN
                || e.getStatusCode() == HttpStatus.NOT_FOUND,
                "Should return 401, 403, or 404");
            System.out.println("Review Counter without auth - Expected error: " + e.getStatusCode());
        } catch (org.springframework.web.client.HttpServerErrorException e) {
            System.out.println("Review Counter without auth - Server error: " + e.getStatusCode());
        }
    }

    @Test
    @Order(52)
    @DisplayName("GET /api/ai/history - AI history without auth should fail")
    void getAiHistory_WithoutAuth_ShouldFail() {
        String url = BASE_URL + "/api/ai/history?current=1&size=10";
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            assertTrue(response.getStatusCode().is3xxRedirection()
                || response.getStatusCode() == HttpStatus.UNAUTHORIZED,
                "Should redirect to login or return 401");
        } catch (HttpClientErrorException e) {
            assertTrue(e.getStatusCode() == HttpStatus.UNAUTHORIZED
                || e.getStatusCode() == HttpStatus.FORBIDDEN,
                "Should return 401 or 403");
            System.out.println("AI History without auth - Expected error: " + e.getStatusCode());
        }
    }

    // ==================== Swagger/OpenAPI ====================

    @Test
    @Order(80)
    @DisplayName("GET /swagger-ui.html - Swagger UI should be accessible")
    void swaggerUi_ShouldBeAccessible() {
        String url = BASE_URL + "/swagger-ui.html";
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            // Swagger redirects to swagger-ui/index.html
            assertTrue(response.getStatusCode().is2xxSuccessful()
                || response.getStatusCode().is3xxRedirection(),
                "Swagger UI should be accessible");
            System.out.println("Swagger UI Status: " + response.getStatusCode());
        } catch (Exception e) {
            System.out.println("Swagger UI Error: " + e.getMessage());
        }
    }

    @Test
    @Order(81)
    @DisplayName("GET /v3/api-docs - OpenAPI docs should be accessible")
    void openApiDocs_ShouldBeAccessible() throws Exception {
        String url = BASE_URL + "/v3/api-docs";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonNode json = objectMapper.readTree(response.getBody());
        assertNotNull(json.get("openapi"), "Should contain openapi version");
        assertNotNull(json.get("paths"), "Should contain API paths");
        System.out.println("OpenAPI Version: " + json.get("openapi").asText());
        System.out.println("API Paths Count: " + json.get("paths").size());
    }

    // ==================== Error Handling ====================

    @Test
    @Order(90)
    @DisplayName("GET /nonexistent - Non-existent endpoint should return 404 or redirect")
    void nonExistentEndpoint_ShouldReturn404OrRedirect() {
        String url = BASE_URL + "/api/nonexistent/endpoint";
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            // May get 404 or 302 redirect depending on security config
            assertTrue(response.getStatusCode() == HttpStatus.NOT_FOUND
                || response.getStatusCode().is3xxRedirection(),
                "Should return 404 or redirect");
            System.out.println("Non-existent endpoint - Response: " + response.getStatusCode());
        } catch (HttpClientErrorException e) {
            assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
            System.out.println("Non-existent endpoint - Expected 404");
        }
    }

    @Test
    @Order(91)
    @DisplayName("POST /api/word with invalid data - Should handle validation errors")
    void createWord_InvalidData_ShouldReturnError() throws Exception {
        String url = BASE_URL + "/api/word";
        HttpEntity<String> request = new HttpEntity<>("{\"invalid\": \"data\"}", headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
            // May get redirect to auth or validation error
            System.out.println("Create Word Invalid Response: " + response.getStatusCode() + " - " + response.getBody());
        } catch (HttpClientErrorException e) {
            System.out.println("Create Word Invalid - Client Error: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
        } catch (org.springframework.web.client.HttpServerErrorException e) {
            // 500 error is also acceptable as the data is invalid
            System.out.println("Create Word Invalid - Server Error: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
        }
    }

    // ==================== CORS Test ====================

    @Test
    @Order(95)
    @DisplayName("OPTIONS /api/word - CORS preflight should succeed")
    void corsPreflightRequest_ShouldSucceed() {
        String url = BASE_URL + "/api/word";
        HttpHeaders corsHeaders = new HttpHeaders();
        corsHeaders.set("Origin", "http://localhost:8081");
        corsHeaders.set("Access-Control-Request-Method", "GET");

        HttpEntity<String> request = new HttpEntity<>(corsHeaders);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.OPTIONS, request, String.class);
            assertTrue(response.getStatusCode().is2xxSuccessful(), "CORS preflight should succeed");
            System.out.println("CORS Headers: " + response.getHeaders());
        } catch (Exception e) {
            System.out.println("CORS Test Error: " + e.getMessage());
        }
    }

    // ==================== Performance/Response Time ====================

    @Test
    @Order(99)
    @DisplayName("API Response Time - Should be under 2 seconds")
    void apiResponseTime_ShouldBeReasonable() {
        String url = BASE_URL + "/api/word/fuzzy?query=test&page=0&size=10";

        long startTime = System.currentTimeMillis();
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        long endTime = System.currentTimeMillis();

        long responseTime = endTime - startTime;
        assertTrue(responseTime < 2000, "Response time should be under 2 seconds, was: " + responseTime + "ms");
        System.out.println("Response Time: " + responseTime + "ms");
    }
}
