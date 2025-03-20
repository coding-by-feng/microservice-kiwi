package me.fengorz.kiwi.word.biz.controller;

import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.sdk.constant.EnvConstants;
import me.fengorz.kiwi.word.biz.WordBizTestApplication;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for PronunciationController using TestRestTemplate to perform real HTTP requests.
 */
@Slf4j
@ActiveProfiles(EnvConstants.TEST)
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = WordBizTestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class PronunciationControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void testDownloadVoice_Success() throws Exception {
        // Prepare test data: Insert a PronunciationDO into the database
        int pronunciationId = 3932719;

        // Perform the GET request using TestRestTemplate
        String url = "http://localhost:" + port + "/word/pronunciation/downloadVoice/" + pronunciationId;

        ResponseEntity<byte[]> response = restTemplate.getForEntity(url, byte[].class);

        // Verify the response
        assertEquals(HttpStatus.OK, response.getStatusCode(), "HTTP status should be 200 OK");
        assertNotNull(response.getBody(), "Response body should not be null");
        assertTrue(response.getBody().length > 0, "Audio file should not be empty");

        // Verify headers for file download
        HttpHeaders responseHeaders = response.getHeaders();
        assertTrue(Objects.requireNonNull(responseHeaders.getContentType()).toString().contains("audio/mpeg") ||
                        responseHeaders.getContentType().toString().contains("application/octet-stream"),
                "Content-Type should be audio/mpeg or application/octet-stream");
        assertTrue(Objects.requireNonNull(responseHeaders.get("Content-Disposition")).toString().contains("attachment"),
                "Content-Disposition should indicate a file download");

        log.info("Downloaded audio file size: {} bytes", response.getBody().length);
    }

    @Test
    @Disabled
    void testDownloadVoice_NotFound() {
        // Test with a non-existent pronunciationId
        String url = "http://localhost:" + port + "/word/pronunciation/downloadVoice/999";
        ResponseEntity<byte[]> response = restTemplate.getForEntity(url, byte[].class);

        // Verify the response
        assertEquals(HttpStatus.OK, response.getStatusCode(), "HTTP status should be 200 OK (endpoint does not return 404)");
        // Since the endpoint does not return a specific error status, we can only log and check logs
        log.info("Expected log message: 'Required wordPronunciation must not be null!'");
    }

}