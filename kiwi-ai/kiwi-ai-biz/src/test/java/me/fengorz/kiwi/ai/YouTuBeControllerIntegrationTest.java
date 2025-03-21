package me.fengorz.kiwi.ai;

import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.ai.api.vo.ytb.YtbSubtitlesVO;
import me.fengorz.kiwi.common.api.R;
import me.fengorz.kiwi.common.sdk.constant.EnvConstants;
import me.fengorz.kiwi.common.sdk.enumeration.LanguageEnum;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;

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

    // Note: URLs are already URL-encoded
    private static final String TEST_URL_AUTO_SUBTITLES = "https%3A%2F%2Fyoutu.be%2F98o_L3jlixw%3Fsi%3DtI5xIUVoQzUEK5yV"; // Short test video
    private static final String TEST_URL_NORMAL_SUBTITLES = "https%3A%2F%2Fwww.youtube.com%2Fwatch%3Fv%3Dq0DMYs4b2Yw"; // Short test video

    private String buildUrl(String endpoint) {
        return "http://localhost:" + port + "/ai/ytb/video/" + endpoint;
    }

    @Test
    @Disabled
    void testDownloadVideo_Success() throws Exception {
        // Use UriComponentsBuilder to properly encode URL parameters
        URI uri = UriComponentsBuilder.fromUriString(buildUrl("download"))
                .queryParam("url", TEST_URL_AUTO_SUBTITLES)
                .build()
                .toUri();

        // Perform the GET request to download the video
        ResponseEntity<byte[]> response = restTemplate.getForEntity(uri, byte[].class);

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
    }

    @Test
    @Disabled
    void testDownloadSubtitlesWithoutLanguageInVtt_Success() {
        String url = buildUrl("subtitles") + "?url=" + TEST_URL_AUTO_SUBTITLES;

        // Define a parameterized type reference for R<YtbSubtitlesVO>
        ParameterizedTypeReference<R<YtbSubtitlesVO>> responseType =
                new ParameterizedTypeReference<R<YtbSubtitlesVO>>() {};

        // Perform the GET request using exchange method to correctly handle the response type
        ResponseEntity<R<YtbSubtitlesVO>> response =
                restTemplate.exchange(url, HttpMethod.GET, null, responseType);

        // Verify the response
        assertEquals(HttpStatus.OK, response.getStatusCode(), "HTTP status should be 200 OK");
        assertNotNull(response.getBody(), "Response body should not be null");
        assertTrue(response.getBody().isSuccess(), "Response should be successful");

        // Get the data from the response as YtbSubtitlesVO
        YtbSubtitlesVO subtitlesVO = response.getBody().getData();
        assertNotNull(subtitlesVO, "Subtitles VO should not be null");

        // Access properties of YtbSubtitlesVO
        assertNotNull(subtitlesVO.getTranslatedOrRetouchedSubtitles(), "Subtitles content should not be null");
        log.info("Downloaded vtt subtitles: {}", subtitlesVO.getTranslatedOrRetouchedSubtitles());
        assertTrue(subtitlesVO.getScrollingSubtitles().contains("thanks and see"));
        assertTrue(subtitlesVO.getTranslatedOrRetouchedSubtitles().contains("thanks and see"));
    }

    @Test
    @Disabled
    void testDownloadSubtitlesWithoutLanguageInSrt_Success() {
        // Use UriComponentsBuilder to properly encode URL parameters
        String url = buildUrl("subtitles") + "?url=" + TEST_URL_NORMAL_SUBTITLES;

        // Define a parameterized type reference for R<YtbSubtitlesVO>
        ParameterizedTypeReference<R<YtbSubtitlesVO>> responseType =
                new ParameterizedTypeReference<R<YtbSubtitlesVO>>() {};

        // Perform the GET request using exchange method to correctly handle the response type
        ResponseEntity<R<YtbSubtitlesVO>> response =
                restTemplate.exchange(url, HttpMethod.GET, null, responseType);

        // Verify the response
        assertEquals(HttpStatus.OK, response.getStatusCode(), "HTTP status should be 200 OK");
        assertNotNull(response.getBody(), "Response body should not be null");
        assertTrue(response.getBody().isSuccess(), "Response should be successful");

        // Get the data from the response as YtbSubtitlesVO
        YtbSubtitlesVO subtitlesVO = response.getBody().getData();
        assertNotNull(subtitlesVO, "Subtitles VO should not be null");

        // Access properties of YtbSubtitlesVO
        assertNotNull(subtitlesVO.getTranslatedOrRetouchedSubtitles(), "Subtitles content should not be null");
        log.info("Downloaded srt scrolling subtitles: {}", subtitlesVO.getScrollingSubtitles());
        log.info("Downloaded srt translated subtitles: {}", subtitlesVO.getTranslatedOrRetouchedSubtitles());
    }

    @Test
    @Disabled
    void testDownloadSubtitlesWithLanguageInVtt_Success() {
        String url = buildUrl("subtitles") + "?url=" + TEST_URL_AUTO_SUBTITLES + "&language=" + LanguageEnum.ZH_CN.getCode();

        // Define a parameterized type reference for R<YtbSubtitlesVO>
        ParameterizedTypeReference<R<YtbSubtitlesVO>> responseType =
                new ParameterizedTypeReference<R<YtbSubtitlesVO>>() {};

        // Perform the GET request using exchange method to correctly handle the response type
        ResponseEntity<R<YtbSubtitlesVO>> response =
                restTemplate.exchange(url, HttpMethod.GET, null, responseType);

        // Verify the response
        assertEquals(HttpStatus.OK, response.getStatusCode(), "HTTP status should be 200 OK");
        assertNotNull(response.getBody(), "Response body should not be null");
        assertTrue(response.getBody().isSuccess(), "Response should be successful");

        // Get the data from the response as YtbSubtitlesVO
        YtbSubtitlesVO subtitlesVO = response.getBody().getData();
        assertNotNull(subtitlesVO, "Subtitles VO should not be null");

        // Access properties of YtbSubtitlesVO
        assertNotNull(subtitlesVO.getTranslatedOrRetouchedSubtitles(), "Subtitles content should not be null");
        // Test content contains expected text - adjust assertion as needed
        // assertTrue(subtitlesVO.getContent().contains("s what you reap when you really put"));
        log.info("Downloaded vtt subtitles with language: {}", subtitlesVO.getTranslatedOrRetouchedSubtitles());
        assertTrue(subtitlesVO.getScrollingSubtitles().contains("thanks and see"));
        assertTrue(subtitlesVO.getTranslatedOrRetouchedSubtitles().contains("thanks and see"));
    }

    @Test
    @Disabled
    void testDownloadSubtitlesWithLanguageInSrt_Success() {
        String url = buildUrl("subtitles") + "?url=" + TEST_URL_NORMAL_SUBTITLES + "&language=" + LanguageEnum.ZH_CN.getCode();

        // Define a parameterized type reference for R<YtbSubtitlesVO>
        ParameterizedTypeReference<R<YtbSubtitlesVO>> responseType =
                new ParameterizedTypeReference<R<YtbSubtitlesVO>>() {};

        // Perform the GET request using exchange method to correctly handle the response type
        ResponseEntity<R<YtbSubtitlesVO>> response =
                restTemplate.exchange(url, HttpMethod.GET, null, responseType);

        // Verify the response
        assertEquals(HttpStatus.OK, response.getStatusCode(), "HTTP status should be 200 OK");
        assertNotNull(response.getBody(), "Response body should not be null");
        assertTrue(response.getBody().isSuccess(), "Response should be successful");

        // Get the data from the response as YtbSubtitlesVO
        YtbSubtitlesVO subtitlesVO = response.getBody().getData();
        assertNotNull(subtitlesVO, "Subtitles VO should not be null");

        // Access properties of YtbSubtitlesVO
        assertNotNull(subtitlesVO.getTranslatedOrRetouchedSubtitles(), "Subtitles content should not be null");
        // Test content contains expected text - adjust assertion as needed
        // assertTrue(subtitlesVO.getContent().contains("s what you reap when you really put"));
        log.info("Downloaded srt scrolling subtitles with language: {}", subtitlesVO.getScrollingSubtitles());
        log.info("Downloaded srt subtitles with language: {}", subtitlesVO.getTranslatedOrRetouchedSubtitles());
    }

    @Test
    void testCleanSubtitles_Success() {
        // Test without language parameter first
        URI uri = UriComponentsBuilder.fromUriString(buildUrl("subtitles"))
                .queryParam("url", TEST_URL_NORMAL_SUBTITLES)
                .build()
                .toUri();

        // Perform the DELETE request
        ResponseEntity<R> response = restTemplate.exchange(
                uri,
                HttpMethod.DELETE,
                null,
                R.class);

        // Verify the response
        assertEquals(HttpStatus.OK, response.getStatusCode(), "HTTP status should be 200 OK");
        assertNotNull(response.getBody(), "Response body should not be null");
        assertTrue(response.getBody().isSuccess(), "Response should be successful");

        // Test with language parameter
        URI uriWithLanguage = UriComponentsBuilder.fromUriString(buildUrl("subtitles"))
                .queryParam("url", TEST_URL_NORMAL_SUBTITLES)
                .queryParam("language", LanguageEnum.ZH_CN.getCode())
                .build()
                .toUri();

        // Perform the DELETE request with language
        ResponseEntity<R> responseWithLanguage = restTemplate.exchange(
                uriWithLanguage,
                HttpMethod.DELETE,
                null,
                R.class);

        // Verify the response
        assertEquals(HttpStatus.OK, responseWithLanguage.getStatusCode(), "HTTP status should be 200 OK");
        assertNotNull(responseWithLanguage.getBody(), "Response body should not be null");
        assertTrue(responseWithLanguage.getBody().isSuccess(), "Response should be successful");

        log.info("Successfully tested cleaning subtitles with and without language parameter");
    }

    @Test
    @Disabled
    void testGetVideoTitle_Success() {
        // Use UriComponentsBuilder to properly encode URL parameters
        URI uri = UriComponentsBuilder.fromUriString(buildUrl("title"))
                .queryParam("url", TEST_URL_AUTO_SUBTITLES)
                .build()
                .toUri();

        // Perform the GET request to get the video title
        ResponseEntity<R> response = restTemplate.getForEntity(uri, R.class);

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
    @Disabled
    void testDownloadVideo_InvalidUrl() {
        // Test with an invalid URL
        String invalidUrl = "https://invalid-url";

        // Use UriComponentsBuilder to properly encode URL parameters
        URI uri = UriComponentsBuilder.fromUriString(buildUrl("download"))
                .queryParam("url", invalidUrl)
                .build()
                .toUri();

        ResponseEntity<InputStreamResource> response = restTemplate.getForEntity(uri, InputStreamResource.class);

        // Verify the response
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode(), "HTTP status should be 500 for invalid URL");
        assertNull(response.getBody(), "Response body should be null for error");
        log.info("Error response for invalid URL");
    }
}