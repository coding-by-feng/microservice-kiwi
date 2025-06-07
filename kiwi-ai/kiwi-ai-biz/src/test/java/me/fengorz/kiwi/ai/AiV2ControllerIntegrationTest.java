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
 * Integration test for AiV2Controller using TestRestTemplate to perform real HTTP requests.
 */
@SuppressWarnings("ALL")
@Slf4j
@ActiveProfiles(EnvConstants.TEST)
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = AiApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AiV2ControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private static final String TARGET_LANG = LanguageEnum.EN.getCode();
    private static final String NATIVE_LANG = LanguageEnum.ZH_CN.getCode();

    @Test
    void testDirectlyTranslation_Success() {
        // Test data
        String originalText = "Hello, world!";

        // Perform the GET request using TestRestTemplate
        String url = "http://localhost:" + port + "/ai/v2/directly-translation/" + TARGET_LANG + "/" + NATIVE_LANG + "/" + originalText;
        ResponseEntity<R> response = restTemplate.getForEntity(url, R.class);

        // Verify the response
        assertEquals(HttpStatus.OK, response.getStatusCode(), "HTTP status should be 200 OK");
        assertNotNull(response.getBody(), "Response body should not be null");
        assertTrue(response.getBody().isSuccess(), "Response should be successful");

        log.info("HTTP body is: {}", response.getBody());

        AiResponseVO vo = KiwiJsonUtils.fromObjectToJson(response.getBody().getData(), AiResponseVO.class);

        assertNotNull(vo, "VO should not be null");
        assertEquals(originalText, vo.getOriginalText(), "Original text should match");
        assertEquals(TARGET_LANG, vo.getLanguageCode(), "Language code should match");
    }

    @Test
    void testDirectlyTranslation_InvalidLanguage() {
        // Test data
        String originalText = "Hello, world!";
        String invalidLang = "INVALID";

        // Perform the GET request using TestRestTemplate
        String url = "http://localhost:" + port + "/ai/v2/directly-translation/" + invalidLang + "/" + NATIVE_LANG + "/" + originalText;

        ResponseEntity<R> response = restTemplate.getForEntity(url, R.class);

        // Verify the response
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(), "HTTP status should be 400 BAD_REQUEST");
    }

    @Test
    void testTranslationAndExplanation_Success() {
        // Test data
        String originalText = "Hello, world!";

        // Perform the GET request
        String url = "http://localhost:" + port + "/ai/v2/translation-and-explanation/" + TARGET_LANG + "/" + NATIVE_LANG + "/" + originalText;
        ResponseEntity<R> response = restTemplate.getForEntity(url, R.class);

        // Verify the response
        assertEquals(HttpStatus.OK, response.getStatusCode(), "HTTP status should be 200 OK");
        assertNotNull(response.getBody(), "Response body should not be null");
        assertTrue(response.getBody().isSuccess(), "Response should be successful");

        AiResponseVO vo = KiwiJsonUtils.fromObjectToJson(response.getBody().getData(), AiResponseVO.class);
        assertNotNull(vo, "VO should not be null");
        assertEquals(originalText, vo.getOriginalText(), "Original text should match");
        assertEquals(TARGET_LANG, vo.getLanguageCode(), "Language code should match");
        log.info("Translation and explanation: {}", vo.getResponseText());
    }

    @Test
    void testTranslationAndExplanation_SpecialSymbol_Success() {
        // Test data
        String originalText = "food%20scraps%2Fgreen%20waste";

        // Perform the GET request
        String url = "http://localhost:" + port + "/ai/v2/translation-and-explanation/" + TARGET_LANG + "/" + NATIVE_LANG + "/" + originalText;
        ResponseEntity<R> response = restTemplate.getForEntity(url, R.class);

        // Verify the response
        assertEquals(HttpStatus.OK, response.getStatusCode(), "HTTP status should be 200 OK");
        assertNotNull(response.getBody(), "Response body should not be null");
        assertTrue(response.getBody().isSuccess(), "Response should be successful");

        AiResponseVO vo = KiwiJsonUtils.fromObjectToJson(response.getBody().getData(), AiResponseVO.class);
        assertNotNull(vo, "VO should not be null");
        assertEquals("food scraps/green waste", vo.getOriginalText(), "Original text should match");
        assertEquals(TARGET_LANG, vo.getLanguageCode(), "Language code should match");
        log.info("Translation and explanation: {}", vo.getResponseText());
    }

    @Test
    void testTranslationAndExplanation_InvalidLanguage() {
        // Test data
        String originalText = "Hello, world!";
        String invalidLang = "INVALID";

        // Perform the GET request
        String url = "http://localhost:" + port + "/ai/v2/translation-and-explanation/" + invalidLang + "/" + NATIVE_LANG + "/" + originalText;
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

        // Perform the GET request
        String url = "http://localhost:" + port + "/ai/v2/grammar-explanation/" + TARGET_LANG + "/" + NATIVE_LANG + "/" + originalText;
        ResponseEntity<R> response = restTemplate.getForEntity(url, R.class);

        // Verify the response
        assertEquals(HttpStatus.OK, response.getStatusCode(), "HTTP status should be 200 OK");
        assertNotNull(response.getBody(), "Response body should not be null");
        assertTrue(response.getBody().isSuccess(), "Response should be successful");

        AiResponseVO vo = KiwiJsonUtils.fromObjectToJson(response.getBody().getData(), AiResponseVO.class);
        assertNotNull(vo, "VO should not be null");
        assertEquals(originalText, vo.getOriginalText(), "Original text should match");
        assertEquals(TARGET_LANG, vo.getLanguageCode(), "Language code should match");
        log.info("Grammar explanation: {}", vo.getResponseText());
    }

    @Test
    void testGrammarExplanation_InvalidLanguage() {
        // Test data
        String originalText = "I is happy.";
        String invalidLang = "INVALID";

        // Perform the GET request
        String url = "http://localhost:" + port + "/ai/v2/grammar-explanation/" + invalidLang + "/" + NATIVE_LANG + "/" + originalText;
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

        // Perform the GET request
        String url = "http://localhost:" + port + "/ai/v2/grammar-correction/" + TARGET_LANG + "/" + NATIVE_LANG + "/" + originalText;
        ResponseEntity<R> response = restTemplate.getForEntity(url, R.class);

        // Verify the response
        assertEquals(HttpStatus.OK, response.getStatusCode(), "HTTP status should be 200 OK");
        assertNotNull(response.getBody(), "Response body should not be null");
        assertTrue(response.getBody().isSuccess(), "Response should be successful");

        AiResponseVO vo = KiwiJsonUtils.fromObjectToJson(response.getBody().getData(), AiResponseVO.class);
        assertNotNull(vo, "VO should not be null");
        assertEquals(originalText, vo.getOriginalText(), "Original text should match");
        assertEquals(TARGET_LANG, vo.getLanguageCode(), "Language code should match");
        log.info("Grammar correction: {}", vo.getResponseText());
    }

    @Test
    void testGrammarCorrection_InvalidLanguage() {
        // Test data
        String originalText = "I is happy.";
        String invalidLang = "INVALID";

        // Perform the GET request
        String url = "http://localhost:" + port + "/ai/v2/grammar-correction/" + invalidLang + "/" + NATIVE_LANG + "/" + originalText;
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

        // Perform the GET request
        String url = "http://localhost:" + port + "/ai/v2/vocabulary-explanation/" + TARGET_LANG + "/" + NATIVE_LANG + "/" + originalText;
        ResponseEntity<R> response = restTemplate.getForEntity(url, R.class);

        // Verify the response
        assertEquals(HttpStatus.OK, response.getStatusCode(), "HTTP status should be 200 OK");
        assertNotNull(response.getBody(), "Response body should not be null");
        assertTrue(response.getBody().isSuccess(), "Response should be successful");

        AiResponseVO vo = KiwiJsonUtils.fromObjectToJson(response.getBody().getData(), AiResponseVO.class);
        assertNotNull(vo, "VO should not be null");
        assertEquals(originalText, vo.getOriginalText(), "Original text should match");
        assertEquals(TARGET_LANG, vo.getLanguageCode(), "Language code should match");
        log.info("Vocabulary explanation: {}", vo.getResponseText());
    }

    @Test
    void testVocabularyExplanation_InvalidLanguage() {
        // Test data
        String originalText = "Hope";
        String invalidLang = "INVALID";

        // Perform the GET request
        String url = "http://localhost:" + port + "/ai/v2/vocabulary-explanation/" + invalidLang + "/" + NATIVE_LANG + "/" + originalText;
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

        // Perform the GET request
        String url = "http://localhost:" + port + "/ai/v2/antonym/" + TARGET_LANG + "/" + NATIVE_LANG + "/" + originalText;
        ResponseEntity<R> response = restTemplate.getForEntity(url, R.class);

        // Verify the response
        assertEquals(HttpStatus.OK, response.getStatusCode(), "HTTP status should be 200 OK");
        assertNotNull(response.getBody(), "Response body should not be null");
        assertTrue(response.getBody().isSuccess(), "Response should be successful");

        AiResponseVO vo = KiwiJsonUtils.fromObjectToJson(response.getBody().getData(), AiResponseVO.class);
        assertNotNull(vo, "VO should not be null");
        assertEquals(originalText, vo.getOriginalText(), "Original text should match");
        assertEquals(TARGET_LANG, vo.getLanguageCode(), "Language code should match");
        log.info("Antonym response: {}", vo.getResponseText());
    }

    @Test
    void testAntonym_InvalidLanguage() {
        // Test data
        String originalText = "Happy";
        String invalidLang = "INVALID";

        // Perform the GET request
        String url = "http://localhost:" + port + "/ai/v2/antonym/" + invalidLang + "/" + NATIVE_LANG + "/" + originalText;
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

        // Perform the GET request
        String url = "http://localhost:" + port + "/ai/v2/synonym/" + TARGET_LANG + "/" + NATIVE_LANG + "/" + originalText;
        ResponseEntity<R> response = restTemplate.getForEntity(url, R.class);

        // Verify the response
        assertEquals(HttpStatus.OK, response.getStatusCode(), "HTTP status should be 200 OK");
        assertNotNull(response.getBody(), "Response body should not be null");
        assertTrue(response.getBody().isSuccess(), "Response should be successful");

        AiResponseVO vo = KiwiJsonUtils.fromObjectToJson(response.getBody().getData(), AiResponseVO.class);
        assertNotNull(vo, "VO should not be null");
        assertEquals(originalText, vo.getOriginalText(), "Original text should match");
        assertEquals(TARGET_LANG, vo.getLanguageCode(), "Language code should match");
        log.info("Synonym response: {}", vo.getResponseText());
    }

    @Test
    void testSynonym_InvalidLanguage() {
        // Test data
        String originalText = "Happy";
        String invalidLang = "INVALID";

        // Perform the GET request
        String url = "http://localhost:" + port + "/ai/v2/synonym/" + invalidLang + "/" + NATIVE_LANG + "/" + originalText;
        ResponseEntity<R> response = restTemplate.getForEntity(url, R.class);

        // Verify the response
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(), "HTTP status should be 400 BAD_REQUEST");
        assertNotNull(response.getBody(), "Response body should not be null");
        assertFalse(response.getBody().isSuccess(), "Response should not be successful");
        log.info("Error response: {}", response.getBody().getMsg());
    }

    @Test
    void testMultipleLanguageCombinations() {
        // Test with different language combinations
        String originalText = "Good morning";
        
        // EN -> ZH_CN with ZH_CN explanations
        String url1 = "http://localhost:" + port + "/ai/v2/translation-and-explanation/ZH_CN/EN/" + originalText;
        ResponseEntity<R> response1 = restTemplate.getForEntity(url1, R.class);
        assertEquals(HttpStatus.OK, response1.getStatusCode());
        log.info("EN->ZH_CN translation with EN explanation");
        
        // ZH_CN -> EN with ZH_CN explanations  
        String chineseText = "早上好";
        String url2 = "http://localhost:" + port + "/ai/v2/translation-and-explanation/EN/ZH_CN/" + chineseText;
        ResponseEntity<R> response2 = restTemplate.getForEntity(url2, R.class);
        assertEquals(HttpStatus.OK, response2.getStatusCode());
        log.info("ZH_CN->EN translation with ZH_CN explanation");
    }
}