package me.fengorz.kason.ai;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kason.ai.api.vo.AiCallHistoryVO;
import me.fengorz.kason.common.api.R;
import me.fengorz.kason.common.sdk.constant.EnvConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for AI call history functionality
 */
@SuppressWarnings("ALL")
@Slf4j
@ActiveProfiles(EnvConstants.TEST)
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = AiApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AiCallHistoryIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void testGetCallHistory_Success() {
        // Build URL for getting call history
        String url = "http://localhost:" + port + "/ai/history?current=1&size=10";

        // Define response type
        ParameterizedTypeReference<R<IPage<AiCallHistoryVO>>> responseType =
                new ParameterizedTypeReference<R<IPage<AiCallHistoryVO>>>() {};

        // Perform the GET request
        ResponseEntity<R<IPage<AiCallHistoryVO>>> response = restTemplate.exchange(
                url, HttpMethod.GET, null, responseType);

        // Verify the response
        assertEquals(HttpStatus.OK, response.getStatusCode(), "HTTP status should be 200 OK");
        assertNotNull(response.getBody(), "Response body should not be null");
        assertTrue(response.getBody().isSuccess(), "Response should be successful");

        // Get the paginated data
        IPage<AiCallHistoryVO> historyPage = response.getBody().getData();
        assertNotNull(historyPage, "History page should not be null");
        assertNotNull(historyPage.getRecords(), "History records should not be null");

        log.info("Retrieved {} AI call history records", historyPage.getRecords().size());
        log.info("Total records: {}, Current page: {}, Page size: {}", 
                historyPage.getTotal(), historyPage.getCurrent(), historyPage.getSize());

        // Log sample records if any exist
        if (!historyPage.getRecords().isEmpty()) {
            AiCallHistoryVO firstRecord = historyPage.getRecords().get(0);
            log.info("Sample record: ID={}, PromptMode={}, TargetLang={}, Timestamp={}", 
                    firstRecord.getId(), firstRecord.getPromptMode(), 
                    firstRecord.getTargetLanguage(), firstRecord.getTimestamp());
        }
    }

    @Test
    void testGetCallHistory_WithCustomPagination() {
        // Test with custom pagination parameters
        String url = "http://localhost:" + port + "/ai/history?current=1&size=5";

        ParameterizedTypeReference<R<IPage<AiCallHistoryVO>>> responseType =
                new ParameterizedTypeReference<R<IPage<AiCallHistoryVO>>>() {};

        ResponseEntity<R<IPage<AiCallHistoryVO>>> response = restTemplate.exchange(
                url, HttpMethod.GET, null, responseType);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());

        IPage<AiCallHistoryVO> historyPage = response.getBody().getData();
        assertEquals(1, historyPage.getCurrent(), "Current page should be 1");
        assertEquals(5, historyPage.getSize(), "Page size should be 5");

        log.info("Custom pagination test - Page: {}, Size: {}, Total: {}", 
                historyPage.getCurrent(), historyPage.getSize(), historyPage.getTotal());
    }

    @Test
    void testGetCallHistory_InvalidPagination() {
        // Test with invalid page number (< 1)
        String url = "http://localhost:" + port + "/ai/history?current=0&size=10";

        ParameterizedTypeReference<R<String>> responseType =
                new ParameterizedTypeReference<R<String>>() {};

        ResponseEntity<R<String>> response = restTemplate.exchange(
                url, HttpMethod.GET, null, responseType);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess(), "Response should not be successful");
        assertTrue(response.getBody().getMsg().contains("Page number must be greater than 0"));

        log.info("Invalid pagination error: {}", response.getBody().getMsg());
    }

    @Test
    void testGetCallHistory_InvalidPageSize() {
        // Test with invalid page size (> 100)
        String url = "http://localhost:" + port + "/ai/history?current=1&size=150";

        ParameterizedTypeReference<R<String>> responseType =
                new ParameterizedTypeReference<R<String>>() {};

        ResponseEntity<R<String>> response = restTemplate.exchange(
                url, HttpMethod.GET, null, responseType);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess(), "Response should not be successful");
        assertTrue(response.getBody().getMsg().contains("Page size must be between 1 and 100"));

        log.info("Invalid page size error: {}", response.getBody().getMsg());
    }

    @Test
    void testGetCallHistory_DefaultPagination() {
        // Test with default pagination (no parameters)
        String url = "http://localhost:" + port + "/ai/history";

        ParameterizedTypeReference<R<IPage<AiCallHistoryVO>>> responseType =
                new ParameterizedTypeReference<R<IPage<AiCallHistoryVO>>>() {};

        ResponseEntity<R<IPage<AiCallHistoryVO>>> response = restTemplate.exchange(
                url, HttpMethod.GET, null, responseType);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());

        IPage<AiCallHistoryVO> historyPage = response.getBody().getData();
        assertEquals(1, historyPage.getCurrent(), "Default current page should be 1");
        assertEquals(20, historyPage.getSize(), "Default page size should be 20");

        log.info("Default pagination test - Page: {}, Size: {}, Total: {}", 
                historyPage.getCurrent(), historyPage.getSize(), historyPage.getTotal());
    }
}