package me.fengorz.kiwi.ai;

import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.api.R;
import me.fengorz.kiwi.common.sdk.constant.EnvConstants;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.io.FileOutputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for VideoController using TestRestTemplate to perform real HTTP requests.
 */
@SuppressWarnings("rawtypes")
@Slf4j
@ActiveProfiles(EnvConstants.TEST)
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class YouTuBeControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private static final String TEST_URL = "https://youtu.be/LgUjLcxJxVg?si=l0Y74ZVzzILlcRgq"; // Short test video


    @Test
    @Disabled
    void testDownloadVideo_Success() throws Exception {
        // Perform the GET request to download the video
        String url = "http://localhost:" + port + "/ai/ytb/video/download?url=" + TEST_URL;
        // Use exchange to get the response with the stream
        ResponseEntity<byte[]> response = restTemplate.getForEntity(url, byte[].class);

        // Verify the response
        assertEquals(HttpStatus.OK, response.getStatusCode(), "HTTP status should be 200 OK");
        assertNotNull(response.getBody(), "Response body should not be null");

        // Save the response body (byte array) to a temporary file
        byte[] videoBytes = response.getBody();
        File tempFile = File.createTempFile("test-video-", ".mp4");

        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(videoBytes);  // Write bytes to the temporary file
        }

        // Move the temporary file to the test resources directory
        File outputPath = new File("src/test/resources/" + tempFile.getName());
        if (outputPath.exists()) {
            outputPath.delete(); // Clean up if it already exists
        }
        boolean success = tempFile.renameTo(outputPath);
        assertTrue(success, "Failed to move file to test resources directory");

        // Verify the file
        assertTrue(outputPath.length() > 0, "Downloaded video file should not be empty");
        log.info("Downloaded video file saved to: {}, size: {} bytes", outputPath.getAbsolutePath(), outputPath.length());

        // Clean up (optional)
        // outputPath.deleteOnExit(); // Uncomment if you want to clean up after test
    }

    @Test
    void testDownloadSubtitles_Success() {
        // Perform the GET request to download subtitles
        String url = "http://localhost:" + port + "/ai/ytb/video/subtitles?url=" + TEST_URL;
        ResponseEntity<R> response = restTemplate.getForEntity(url, R.class);

        // Verify the response
        assertEquals(HttpStatus.OK, response.getStatusCode(), "HTTP status should be 200 OK");
        assertNotNull(response.getBody(), "Response body should not be null");
        assertTrue(response.getBody().isSuccess(), "Response should be successful");

        String subtitles = (String) response.getBody().getData();
        assertNotNull(subtitles, "File name should not be null");
        log.info("Downloaded subtitles : {}", subtitles);
    }

    @Test
    void testGetVideoTitle_Success() {
        // Perform the GET request to get the video title
        String url = "http://localhost:" + port + "/ai/ytb/video/title?url=" + TEST_URL;
        ResponseEntity<R> response = restTemplate.getForEntity(url, R.class);

        // Verify the response
        assertEquals(HttpStatus.OK, response.getStatusCode(), "HTTP status should be 200 OK");
        assertNotNull(response.getBody(), "Response body should not be null");
        assertTrue(response.getBody().isSuccess(), "Response should be successful");

        String title = (String) response.getBody().getData();
        assertNotNull(title, "Title should not be null");
        assertFalse(title.isEmpty(), "Title should not be empty");
        log.info("Video title: {}", title);
    }

    @Test
    void testDownloadVideo_InvalidUrl() {
        // Test with an invalid URL
        String invalidUrl = "https://invalid-url";
        String url = "http://localhost:" + port + "/ai/ytb/video/download?url=" + invalidUrl;
        ResponseEntity<InputStreamResource> response = restTemplate.getForEntity(url, InputStreamResource.class);

        // Verify the response
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode(), "HTTP status should be 500 for invalid URL");
        assertNull(response.getBody(), "Response body should be null for error");
        log.info("Error response for invalid URL");
    }
}