package me.fengorz.kiwi.ai;

import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.ai.api.vo.AiResponseVO;
import me.fengorz.kiwi.common.api.R;
import me.fengorz.kiwi.common.sdk.constant.EnvConstants;
import me.fengorz.kiwi.common.sdk.enumeration.LanguageEnum;
import me.fengorz.kiwi.common.sdk.util.json.KiwiJsonUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for AiController using TestRestTemplate to perform real HTTP requests.
 */
@SuppressWarnings("ALL")
@Slf4j
@ActiveProfiles(EnvConstants.TEST)
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = AiApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AiControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void testDirectlyTranslation_Success() {
        // Test data
        String originalText = "Hello, world!";
        String language = LanguageEnum.EN.getCode();
        String translatedText = "你好，世界！";

        // Perform the GET request using TestRestTemplate
        String url = "http://localhost:" + port + "/ai/directly-translation/" + language + "/" + originalText;
        ResponseEntity<R> response = restTemplate.getForEntity(url, R.class);

        // Verify the response
        assertEquals(HttpStatus.OK, response.getStatusCode(), "HTTP status should be 200 OK");
        assertNotNull(response.getBody(), "Response body should not be null");
        assertTrue(response.getBody().isSuccess(), "Response should be successful");

        log.info("HTTP body is: {}", response.getBody());

        AiResponseVO vo = KiwiJsonUtils.fromObjectToJson(response.getBody().getData(), AiResponseVO.class);

        assertNotNull(vo, "VO should not be null");
        assertEquals(originalText, vo.getOriginalText(), "Original text should match");
        assertEquals(language, vo.getLanguageCode(), "Language code should match");

    }

    @Test
    void testDirectlyTranslation_InvalidLanguage() {
        // Test data
        String originalText = "Hello, world!";
        String language = "INVALID";

        // Perform the GET request using TestRestTemplate
        String url = "http://localhost:" + port + "/ai/directly-translation/" + language + "/" + originalText;

        ResponseEntity<R> response = restTemplate.getForEntity(url, R.class);

        // Verify the response
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(), "HTTP status should be 400 BAD_REQUEST");
    }

    @Test
    void testTranslationAndExplanation_Success() {
        // Test data
        String originalText = "Hello, world!";
        String language = LanguageEnum.ZH_CN.getCode();
        String expectedResponse = "Translation: 你好，世界！\nExplanation: ..."; // Example response

        // Perform the GET request
        String url = "http://localhost:" + port + "/ai/translation-and-explanation/" + language + "/" + originalText;
        ResponseEntity<R> response = restTemplate.getForEntity(url, R.class);

        // Verify the response
        assertEquals(HttpStatus.OK, response.getStatusCode(), "HTTP status should be 200 OK");
        assertNotNull(response.getBody(), "Response body should not be null");
        assertTrue(response.getBody().isSuccess(), "Response should be successful");

        AiResponseVO vo = KiwiJsonUtils.fromObjectToJson(response.getBody().getData(), AiResponseVO.class);
        assertNotNull(vo, "VO should not be null");
        assertEquals(originalText, vo.getOriginalText(), "Original text should match");
        assertEquals(language, vo.getLanguageCode(), "Language code should match");
        log.info("Translation and explanation: {}", vo.getResponseText());
    }

    @Test
    void testTranslationAndExplanation_SpecialSymbol_Success() {
        // Test data
        String originalText = "food%20scraps%2Fgreen%20waste";
        String language = LanguageEnum.ZH_CN.getCode();

        // Perform the GET request
        String url = "http://localhost:" + port + "/ai/translation-and-explanation/" + language + "/" + originalText;
        ResponseEntity<R> response = restTemplate.getForEntity(url, R.class);

        // Verify the response
        assertEquals(HttpStatus.OK, response.getStatusCode(), "HTTP status should be 200 OK");
        assertNotNull(response.getBody(), "Response body should not be null");
        assertTrue(response.getBody().isSuccess(), "Response should be successful");

        AiResponseVO vo = KiwiJsonUtils.fromObjectToJson(response.getBody().getData(), AiResponseVO.class);
        assertNotNull(vo, "VO should not be null");
        assertEquals("food scraps/green waste", vo.getOriginalText(), "Original text should match");
        assertEquals(language, vo.getLanguageCode(), "Language code should match");
        log.info("Translation and explanation: {}", vo.getResponseText());
    }

    @Test
    void testTranslationAndExplanation_InvalidLanguage() {
        // Test data
        String originalText = "Hello, world!";
        String language = "INVALID";

        // Perform the GET request
        String url = "http://localhost:" + port + "/ai/translation-and-explanation/" + language + "/" + originalText;
        ResponseEntity<R> response = restTemplate.getForEntity(url, R.class);

        // Verify the response
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(), "HTTP status should be 400 BAD_REQUEST");
        assertNotNull(response.getBody(), "Response body should not be null");
        assertFalse(response.getBody().isSuccess(), "Response should not be successful");
        log.info("Error response: {}", response.getBody().getMsg());
    }

    @Test
    void testGrammarExplanation_Success() {
        // Test data
        String originalText = "I is happy.";
        String language = LanguageEnum.EN.getCode();
        String expectedResponse = "Explanation: The correct sentence should be 'I am happy' because..."; // Example

        // Perform the GET request
        String url = "http://localhost:" + port + "/ai/grammar-explanation/" + language + "/" + originalText;
        ResponseEntity<R> response = restTemplate.getForEntity(url, R.class);

        // Verify the response
        assertEquals(HttpStatus.OK, response.getStatusCode(), "HTTP status should be 200 OK");
        assertNotNull(response.getBody(), "Response body should not be null");
        assertTrue(response.getBody().isSuccess(), "Response should be successful");

        AiResponseVO vo = KiwiJsonUtils.fromObjectToJson(response.getBody().getData(), AiResponseVO.class);
        assertNotNull(vo, "VO should not be null");
        assertEquals(originalText, vo.getOriginalText(), "Original text should match");
        assertEquals(language, vo.getLanguageCode(), "Language code should match");
        assertTrue(vo.getResponseText().contains("Explanation"), "Response should contain grammar explanation");
        log.info("Grammar explanation: {}", vo.getResponseText());
    }

    @Test
    void testGrammarExplanation_InvalidLanguage() {
        // Test data
        String originalText = "I is happy.";
        String language = "INVALID";

        // Perform the GET request
        String url = "http://localhost:" + port + "/ai/grammar-explanation/" + language + "/" + originalText;
        ResponseEntity<R> response = restTemplate.getForEntity(url, R.class);

        // Verify the response
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(), "HTTP status should be 400 BAD_REQUEST");
        assertNotNull(response.getBody(), "Response body should not be null");
        assertFalse(response.getBody().isSuccess(), "Response should not be successful");
        log.info("Error response: {}", response.getBody().getMsg());
    }

    @Test
    void testGrammarCorrection_Success() {
        // Test data
        String originalText = "I is happy.";
        String language = LanguageEnum.EN.getCode();
        String expectedResponse = "Corrected: I am happy."; // Example

        // Perform the GET request
        String url = "http://localhost:" + port + "/ai/grammar-correction/" + language + "/" + originalText;
        ResponseEntity<R> response = restTemplate.getForEntity(url, R.class);

        // Verify the response
        assertEquals(HttpStatus.OK, response.getStatusCode(), "HTTP status should be 200 OK");
        assertNotNull(response.getBody(), "Response body should not be null");
        assertTrue(response.getBody().isSuccess(), "Response should be successful");

        AiResponseVO vo = KiwiJsonUtils.fromObjectToJson(response.getBody().getData(), AiResponseVO.class);
        assertNotNull(vo, "VO should not be null");
        assertEquals(originalText, vo.getOriginalText(), "Original text should match");
        assertEquals(language, vo.getLanguageCode(), "Language code should match");
        log.info("Grammar correction: {}", vo.getResponseText());
    }

    @Test
    void testGrammarCorrection_InvalidLanguage() {
        // Test data
        String originalText = "I is happy.";
        String language = "INVALID";

        // Perform the GET request
        String url = "http://localhost:" + port + "/ai/grammar-correction/" + language + "/" + originalText;
        ResponseEntity<R> response = restTemplate.getForEntity(url, R.class);

        // Verify the response
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(), "HTTP status should be 400 BAD_REQUEST");
        assertNotNull(response.getBody(), "Response body should not be null");
        assertFalse(response.getBody().isSuccess(), "Response should not be successful");
        log.info("Error response: {}", response.getBody().getMsg());
    }

    @Test
    void testVocabularyExplanation_Success() {
        // Test data
        String originalText = "Hope";
        String language = LanguageEnum.EN.getCode();
        String expectedResponse = "Definition: A feeling of expectation..."; // Example

        // Perform the GET request
        String url = "http://localhost:" + port + "/ai/vocabulary-explanation/" + language + "/" + originalText;
        ResponseEntity<R> response = restTemplate.getForEntity(url, R.class);

        // Verify the response
        assertEquals(HttpStatus.OK, response.getStatusCode(), "HTTP status should be 200 OK");
        assertNotNull(response.getBody(), "Response body should not be null");
        assertTrue(response.getBody().isSuccess(), "Response should be successful");

        AiResponseVO vo = KiwiJsonUtils.fromObjectToJson(response.getBody().getData(), AiResponseVO.class);
        assertNotNull(vo, "VO should not be null");
        assertEquals(originalText, vo.getOriginalText(), "Original text should match");
        assertEquals(language, vo.getLanguageCode(), "Language code should match");
        log.info("Vocabulary explanation: {}", vo.getResponseText());
    }

    @Test
    void testVocabularyExplanation_InvalidLanguage() {
        // Test data
        String originalText = "Hope";
        String language = "INVALID";

        // Perform the GET request
        String url = "http://localhost:" + port + "/ai/vocabulary-explanation/" + language + "/" + originalText;
        ResponseEntity<R> response = restTemplate.getForEntity(url, R.class);

        // Verify the response
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(), "HTTP status should be 400 BAD_REQUEST");
        assertNotNull(response.getBody(), "Response body should not be null");
        assertFalse(response.getBody().isSuccess(), "Response should not be successful");
        log.info("Error response: {}", response.getBody().getMsg());
    }

    @Test
    void testAntonym_Success() {
        // Test data
        String originalText = "Happy";
        String language = LanguageEnum.EN.getCode();
        String expectedResponse = "Antonyms: Sad, Unhappy"; // Example response

        // Perform the GET request
        String url = "http://localhost:" + port + "/ai/antonym/" + language + "/" + originalText;
        ResponseEntity<R> response = restTemplate.getForEntity(url, R.class);

        // Verify the response
        assertEquals(HttpStatus.OK, response.getStatusCode(), "HTTP status should be 200 OK");
        assertNotNull(response.getBody(), "Response body should not be null");
        assertTrue(response.getBody().isSuccess(), "Response should be successful");

        AiResponseVO vo = KiwiJsonUtils.fromObjectToJson(response.getBody().getData(), AiResponseVO.class);
        assertNotNull(vo, "VO should not be null");
        assertEquals(originalText, vo.getOriginalText(), "Original text should match");
        assertEquals(language, vo.getLanguageCode(), "Language code should match");
        log.info("Antonym response: {}", vo.getResponseText());
    }

    @Test
    void testAntonym_InvalidLanguage() {
        // Test data
        String originalText = "Happy";
        String language = "INVALID";

        // Perform the GET request
        String url = "http://localhost:" + port + "/ai/antonym/" + language + "/" + originalText;
        ResponseEntity<R> response = restTemplate.getForEntity(url, R.class);

        // Verify the response
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(), "HTTP status should be 400 BAD_REQUEST");
        assertNotNull(response.getBody(), "Response body should not be null");
        assertFalse(response.getBody().isSuccess(), "Response should not be successful");
        log.info("Error response: {}", response.getBody().getMsg());
    }

    @Test
    void testSynonym_Success() {
        // Test data
        String originalText = "Happy";
        String language = LanguageEnum.EN.getCode();
        String expectedResponse = "Synonyms: Joyful, Glad"; // Example response

        // Perform the GET request
        String url = "http://localhost:" + port + "/ai/synonym/" + language + "/" + originalText;
        ResponseEntity<R> response = restTemplate.getForEntity(url, R.class);

        // Verify the response
        assertEquals(HttpStatus.OK, response.getStatusCode(), "HTTP status should be 200 OK");
        assertNotNull(response.getBody(), "Response body should not be null");
        assertTrue(response.getBody().isSuccess(), "Response should be successful");

        AiResponseVO vo = KiwiJsonUtils.fromObjectToJson(response.getBody().getData(), AiResponseVO.class);
        assertNotNull(vo, "VO should not be null");
        assertEquals(originalText, vo.getOriginalText(), "Original text should match");
        assertEquals(language, vo.getLanguageCode(), "Language code should match");
        log.info("Synonym response: {}", vo.getResponseText());
    }

    @Test
    void testSynonym_InvalidLanguage() {
        // Test data
        String originalText = "Happy";
        String language = "INVALID";

        // Perform the GET request
        String url = "http://localhost:" + port + "/ai/synonym/" + language + "/" + originalText;
        ResponseEntity<R> response = restTemplate.getForEntity(url, R.class);

        // Verify the response
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(), "HTTP status should be 400 BAD_REQUEST");
        assertNotNull(response.getBody(), "Response body should not be null");
        assertFalse(response.getBody().isSuccess(), "Response should not be successful");
        log.info("Error response: {}", response.getBody().getMsg());
    }

}