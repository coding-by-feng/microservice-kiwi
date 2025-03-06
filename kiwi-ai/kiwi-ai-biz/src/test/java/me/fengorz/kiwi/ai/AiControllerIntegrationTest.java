package me.fengorz.kiwi.ai;

import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.ai.api.vo.DirectlyTranslationVO;
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
    void testDirectlyTranslate_Success() {
        // Test data
        String originalText = "Hello, world!";
        String language = LanguageEnum.EN.getCode();
        String translatedText = "你好，世界！";

        // Perform the GET request using TestRestTemplate
        String url = "http://localhost:" + port + "/ai/directly-translate/" + language + "/" + originalText;
        ResponseEntity<R> response = restTemplate.getForEntity(url, R.class);

        // Verify the response
        assertEquals(HttpStatus.OK, response.getStatusCode(), "HTTP status should be 200 OK");
        assertNotNull(response.getBody(), "Response body should not be null");
        assertTrue(response.getBody().isSuccess(), "Response should be successful");

        log.info("HTTP body is: {}", response.getBody());

        DirectlyTranslationVO vo = KiwiJsonUtils.fromObjectToJson(response.getBody().getData(), DirectlyTranslationVO.class);
        assertNotNull(vo, "VO should not be null");
        assertEquals(originalText, vo.getOriginalText(), "Original text should match");
        assertEquals(language, vo.getLanguageCode(), "Language code should match");

    }

    @Test
    void testDirectlyTranslate_InvalidLanguage() {
        // Test data
        String originalText = "Hello, world!";
        String language = "INVALID";

        // Perform the GET request using TestRestTemplate
        String url = "http://localhost:" + port + "/ai/directly-translate/" + language + "/" + originalText;
        ResponseEntity<R> response = restTemplate.getForEntity(url, R.class);

        // Verify the response
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(), "HTTP status should be 400 BAD_REQUEST");
    }

}