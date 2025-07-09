package me.fengorz.kiwi.ai;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.ai.api.vo.ytb.CaptionResponse;
import me.fengorz.kiwi.ai.api.vo.ytb.ChannelDetailsResponse;
import me.fengorz.kiwi.ai.api.vo.ytb.VideoDetailsResponse;
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
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for YouTuBeControllerV2 using TestRestTemplate to perform real HTTP requests.
 * This controller uses YouTube Data API v3 instead of yt-dlp.
 */
@SuppressWarnings("rawtypes")
@Slf4j
@ActiveProfiles(EnvConstants.TEST)
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class YouTuBeControllerV2IntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    // Test URLs - using real YouTube URLs for testing
    private static final String TEST_VIDEO_URL;
    private static final String TEST_CHANNEL_URL;
    private static final String TEST_CHANNEL_HANDLE_URL;

    static {
        try {
            // Popular YouTube video with captions
            TEST_VIDEO_URL = URLEncoder.encode("https://www.youtube.com/watch?v=dQw4w9WgXcQ", "UTF-8");
            // Popular YouTube channel
            TEST_CHANNEL_URL = URLEncoder.encode("https://www.youtube.com/channel/UCuAXFkgsw1L7xaCfnd5JJOw", "UTF-8");
            // Channel with handle
            TEST_CHANNEL_HANDLE_URL = URLEncoder.encode("https://www.youtube.com/@YouTube", "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private String buildUrl(String endpoint) {
        return "http://localhost:" + port + "/ai/ytb/v2/" + endpoint;
    }

    @Test
    void testGetVideoDetails_Success() {
        URI uri = UriComponentsBuilder.fromUriString(buildUrl("video/details"))
                .queryParam("url", TEST_VIDEO_URL)
                .build()
                .toUri();

        ParameterizedTypeReference<R<VideoDetailsResponse>> responseType =
                new ParameterizedTypeReference<R<VideoDetailsResponse>>() {};

        ResponseEntity<R<VideoDetailsResponse>> response = restTemplate.exchange(
                uri.toString(), HttpMethod.GET, null, responseType);

        // Verify the response
        assertEquals(HttpStatus.OK, response.getStatusCode(), "HTTP status should be 200 OK");
        assertNotNull(response.getBody(), "Response body should not be null");
        assertTrue(response.getBody().isSuccess(), "Response should be successful");

        VideoDetailsResponse videoDetails = response.getBody().getData();
        assertNotNull(videoDetails, "Video details should not be null");
        assertNotNull(videoDetails.getVideoId(), "Video ID should not be null");
        assertNotNull(videoDetails.getTitle(), "Video title should not be null");
        assertNotNull(videoDetails.getChannelId(), "Channel ID should not be null");
        assertNotNull(videoDetails.getChannelTitle(), "Channel title should not be null");
        assertNotNull(videoDetails.getPublishedAt(), "Publish date should not be null");

        log.info("Video details: ID={}, Title={}, Channel={}, Duration={}, Views={}",
                videoDetails.getVideoId(), videoDetails.getTitle(), videoDetails.getChannelTitle(),
                videoDetails.getDuration(), videoDetails.getViewCount());
    }

    @SneakyThrows
    @Test
    void testGetVideoDetails_InvalidUrl() {
        String invalidUrl = URLEncoder.encode("https://invalid-url", String.valueOf(StandardCharsets.UTF_8));

        URI uri = UriComponentsBuilder.fromUriString(buildUrl("video/details"))
                .queryParam("url", invalidUrl)
                .build()
                .toUri();

        ParameterizedTypeReference<R<String>> responseType =
                new ParameterizedTypeReference<R<String>>() {};

        ResponseEntity<R<String>> response = restTemplate.exchange(
                uri.toString(), HttpMethod.GET, null, responseType);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess(), "Response should not be successful for invalid URL");
        assertTrue(response.getBody().getMsg().contains("Failed to get video details"));

        log.info("Invalid URL error: {}", response.getBody().getMsg());
    }

    @Test
    void testGetChannelDetails_Success() {
        URI uri = UriComponentsBuilder.fromUriString(buildUrl("channel/details"))
                .queryParam("url", TEST_CHANNEL_URL)
                .build()
                .toUri();

        ParameterizedTypeReference<R<ChannelDetailsResponse>> responseType =
                new ParameterizedTypeReference<R<ChannelDetailsResponse>>() {};

        ResponseEntity<R<ChannelDetailsResponse>> response = restTemplate.exchange(
                uri.toString(), HttpMethod.GET, null, responseType);

        assertEquals(HttpStatus.OK, response.getStatusCode(), "HTTP status should be 200 OK");
        assertNotNull(response.getBody(), "Response body should not be null");
        assertTrue(response.getBody().isSuccess(), "Response should be successful");

        ChannelDetailsResponse channelDetails = response.getBody().getData();
        assertNotNull(channelDetails, "Channel details should not be null");
        assertNotNull(channelDetails.getChannelId(), "Channel ID should not be null");
        assertNotNull(channelDetails.getTitle(), "Channel title should not be null");
        assertNotNull(channelDetails.getPublishedAt(), "Publish date should not be null");
        assertNotNull(channelDetails.getUploadsPlaylistId(), "Uploads playlist ID should not be null");

        log.info("Channel details: ID={}, Title={}, Subscribers={}, Videos={}, Views={}",
                channelDetails.getChannelId(), channelDetails.getTitle(),
                channelDetails.getSubscriberCount(), channelDetails.getVideoCount(),
                channelDetails.getViewCount());
    }

    @Test
    void testGetChannelDetails_WithHandle() {
        URI uri = UriComponentsBuilder.fromUriString(buildUrl("channel/details"))
                .queryParam("url", TEST_CHANNEL_HANDLE_URL)
                .build()
                .toUri();

        ParameterizedTypeReference<R<ChannelDetailsResponse>> responseType =
                new ParameterizedTypeReference<R<ChannelDetailsResponse>>() {};

        ResponseEntity<R<ChannelDetailsResponse>> response = restTemplate.exchange(
                uri.toString(), HttpMethod.GET, null, responseType);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess(), "Response should be successful for channel handle");

        ChannelDetailsResponse channelDetails = response.getBody().getData();
        assertNotNull(channelDetails, "Channel details should not be null");
        assertNotNull(channelDetails.getChannelId(), "Channel ID should not be null");

        log.info("Channel details from handle: ID={}, Title={}", 
                channelDetails.getChannelId(), channelDetails.getTitle());
    }

    @SneakyThrows
    @Test
    void testGetChannelDetails_InvalidUrl() {
        String invalidUrl = URLEncoder.encode("https://invalid-channel-url", String.valueOf(StandardCharsets.UTF_8));

        URI uri = UriComponentsBuilder.fromUriString(buildUrl("channel/details"))
                .queryParam("url", invalidUrl)
                .build()
                .toUri();

        ParameterizedTypeReference<R<String>> responseType =
                new ParameterizedTypeReference<R<String>>() {};

        ResponseEntity<R<String>> response = restTemplate.exchange(
                uri.toString(), HttpMethod.GET, null, responseType);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess(), "Response should not be successful for invalid URL");

        log.info("Invalid channel URL error: {}", response.getBody().getMsg());
    }

    @Test
    void testGetVideoCaptions_Success() {
        URI uri = UriComponentsBuilder.fromUriString(buildUrl("video/captions"))
                .queryParam("url", TEST_VIDEO_URL)
                .build()
                .toUri();

        ParameterizedTypeReference<R<List<CaptionResponse>>> responseType =
                new ParameterizedTypeReference<R<List<CaptionResponse>>>() {};

        ResponseEntity<R<List<CaptionResponse>>> response = restTemplate.exchange(
                uri.toString(), HttpMethod.GET, null, responseType);

        assertEquals(HttpStatus.OK, response.getStatusCode(), "HTTP status should be 200 OK");
        assertNotNull(response.getBody(), "Response body should not be null");
        assertTrue(response.getBody().isSuccess(), "Response should be successful");

        List<CaptionResponse> captions = response.getBody().getData();
        assertNotNull(captions, "Captions list should not be null");

        log.info("Found {} captions for video", captions.size());

        if (!captions.isEmpty()) {
            CaptionResponse firstCaption = captions.get(0);
            assertNotNull(firstCaption.getId(), "Caption ID should not be null");
            assertNotNull(firstCaption.getLanguage(), "Caption language should not be null");
            assertNotNull(firstCaption.getName(), "Caption name should not be null");

            log.info("First caption: ID={}, Language={}, Name={}, AutoSynced={}, CC={}",
                    firstCaption.getId(), firstCaption.getLanguage(), firstCaption.getName(),
                    firstCaption.getIsAutoSynced(), firstCaption.getIsCC());
        }
    }

    @SneakyThrows
    @Test
    void testGetVideoCaptions_NoSubtitles() {
        // Use a video URL that likely doesn't have captions
        String videoWithoutCaptions = URLEncoder.encode("https://www.youtube.com/watch?v=test", String.valueOf(StandardCharsets.UTF_8));

        URI uri = UriComponentsBuilder.fromUriString(buildUrl("video/captions"))
                .queryParam("url", videoWithoutCaptions)
                .build()
                .toUri();

        ParameterizedTypeReference<R<List<CaptionResponse>>> responseType =
                new ParameterizedTypeReference<R<List<CaptionResponse>>>() {};

        ResponseEntity<R<List<CaptionResponse>>> response = restTemplate.exchange(
                uri.toString(), HttpMethod.GET, null, responseType);

        // Should still return OK but with empty list or error
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        log.info("Response for video without captions: success={}, message={}",
                response.getBody().isSuccess(), response.getBody().getMsg());
    }

    @Test
    void testDownloadSubtitles_Success() {
        String url = buildUrl("subtitles") + "?url=" + TEST_VIDEO_URL;

        ParameterizedTypeReference<R<YtbSubtitlesVO>> responseType =
                new ParameterizedTypeReference<R<YtbSubtitlesVO>>() {};

        ResponseEntity<R<YtbSubtitlesVO>> response = restTemplate.exchange(
                url, HttpMethod.GET, null, responseType);

        assertEquals(HttpStatus.OK, response.getStatusCode(), "HTTP status should be 200 OK");
        assertNotNull(response.getBody(), "Response body should not be null");
        assertTrue(response.getBody().isSuccess(), "Response should be successful");

        YtbSubtitlesVO subtitlesVO = response.getBody().getData();
        assertNotNull(subtitlesVO, "Subtitles VO should not be null");
        assertNotNull(subtitlesVO.getScrollingSubtitles(), "Scrolling subtitles should not be null");
        assertNotNull(subtitlesVO.getType(), "Subtitle type should not be null");

        log.info("Downloaded subtitles: type={}, scrolling_length={}, translated_length={}",
                subtitlesVO.getType(),
                subtitlesVO.getScrollingSubtitles().length(),
                subtitlesVO.getTranslatedOrRetouchedSubtitles() != null ? 
                    subtitlesVO.getTranslatedOrRetouchedSubtitles().length() : 0);
    }

    @Test
    void testDownloadSubtitles_WithTranslation() {
        String url = buildUrl("subtitles") + "?url=" + TEST_VIDEO_URL + "&language=" + LanguageEnum.ZH_CN.getCode();

        ParameterizedTypeReference<R<YtbSubtitlesVO>> responseType =
                new ParameterizedTypeReference<R<YtbSubtitlesVO>>() {};

        ResponseEntity<R<YtbSubtitlesVO>> response = restTemplate.exchange(
                url, HttpMethod.GET, null, responseType);

        assertEquals(HttpStatus.OK, response.getStatusCode(), "HTTP status should be 200 OK");
        assertNotNull(response.getBody(), "Response body should not be null");
        assertTrue(response.getBody().isSuccess(), "Response should be successful");

        YtbSubtitlesVO subtitlesVO = response.getBody().getData();
        assertNotNull(subtitlesVO, "Subtitles VO should not be null");
        assertNotNull(subtitlesVO.getScrollingSubtitles(), "Scrolling subtitles should not be null");
        assertNotNull(subtitlesVO.getTranslatedOrRetouchedSubtitles(), "Translated subtitles should not be null");

        log.info("Downloaded subtitles with translation: type={}, original_length={}, translated_length={}",
                subtitlesVO.getType(),
                subtitlesVO.getScrollingSubtitles().length(),
                subtitlesVO.getTranslatedOrRetouchedSubtitles().length());
    }

    @Test
    void testCleanSubtitles_Success() {
        URI uri = UriComponentsBuilder.fromUriString(buildUrl("subtitles"))
                .queryParam("url", TEST_VIDEO_URL)
                .build()
                .toUri();

        ResponseEntity<R> response = restTemplate.exchange(
                uri,
                HttpMethod.DELETE,
                null,
                R.class);

        assertEquals(HttpStatus.OK, response.getStatusCode(), "HTTP status should be 200 OK");
        assertNotNull(response.getBody(), "Response body should not be null");
        assertTrue(response.getBody().isSuccess(), "Response should be successful");

        log.info("Successfully cleaned subtitles cache");
    }

    @Test
    void testCleanSubtitles_WithLanguage() {
        URI uri = UriComponentsBuilder.fromUriString(buildUrl("subtitles"))
                .queryParam("url", TEST_VIDEO_URL)
                .queryParam("language", LanguageEnum.ZH_CN.getCode())
                .build()
                .toUri();

        ResponseEntity<R> response = restTemplate.exchange(
                uri,
                HttpMethod.DELETE,
                null,
                R.class);

        assertEquals(HttpStatus.OK, response.getStatusCode(), "HTTP status should be 200 OK");
        assertNotNull(response.getBody(), "Response body should not be null");
        assertTrue(response.getBody().isSuccess(), "Response should be successful");

        log.info("Successfully cleaned subtitles cache with language");
    }

    @Test
    void testGetVideoTitle_Success() {
        URI uri = UriComponentsBuilder.fromUriString(buildUrl("title"))
                .queryParam("url", TEST_VIDEO_URL)
                .build()
                .toUri();

        ParameterizedTypeReference<R<String>> responseType =
                new ParameterizedTypeReference<R<String>>() {};

        ResponseEntity<R<String>> response = restTemplate.exchange(
                uri.toString(), HttpMethod.GET, null, responseType);

        assertEquals(HttpStatus.OK, response.getStatusCode(), "HTTP status should be 200 OK");
        assertNotNull(response.getBody(), "Response body should not be null");
        assertTrue(response.getBody().isSuccess(), "Response should be successful");

        String title = response.getBody().getData();
        assertNotNull(title, "Title should not be null");
        assertFalse(title.isEmpty(), "Title should not be empty");

        log.info("Video title: {}", title);
    }

    @SneakyThrows
    @Test
    void testGetVideoTitle_InvalidUrl() {
        String invalidUrl = URLEncoder.encode("https://invalid-video-url", String.valueOf(StandardCharsets.UTF_8));

        URI uri = UriComponentsBuilder.fromUriString(buildUrl("title"))
                .queryParam("url", invalidUrl)
                .build()
                .toUri();

        ParameterizedTypeReference<R<String>> responseType =
                new ParameterizedTypeReference<R<String>>() {};

        ResponseEntity<R<String>> response = restTemplate.exchange(
                uri.toString(), HttpMethod.GET, null, responseType);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess(), "Response should not be successful for invalid URL");
        assertTrue(response.getBody().getMsg().contains("Failed to get video title"));

        log.info("Invalid video URL error: {}", response.getBody().getMsg());
    }

    @Test
    @Disabled("Requires valid YouTube API key and internet connection")
    void testApiIntegration_FullWorkflow() {
        // Test complete workflow: channel details -> video captions -> subtitle download
        
        // 1. Get channel details
        log.info("Step 1: Getting channel details");
        URI channelUri = UriComponentsBuilder.fromUriString(buildUrl("channel/details"))
                .queryParam("url", TEST_CHANNEL_URL)
                .build()
                .toUri();

        ParameterizedTypeReference<R<ChannelDetailsResponse>> channelResponseType =
                new ParameterizedTypeReference<R<ChannelDetailsResponse>>() {};

        ResponseEntity<R<ChannelDetailsResponse>> channelResponse = restTemplate.exchange(
                channelUri.toString(), HttpMethod.GET, null, channelResponseType);

        assertTrue(channelResponse.getBody().isSuccess());
        ChannelDetailsResponse channelDetails = channelResponse.getBody().getData();
        
        // 2. Get video details
        log.info("Step 2: Getting video details");
        URI videoUri = UriComponentsBuilder.fromUriString(buildUrl("video/details"))
                .queryParam("url", TEST_VIDEO_URL)
                .build()
                .toUri();

        ParameterizedTypeReference<R<VideoDetailsResponse>> videoResponseType =
                new ParameterizedTypeReference<R<VideoDetailsResponse>>() {};

        ResponseEntity<R<VideoDetailsResponse>> videoResponse = restTemplate.exchange(
                videoUri.toString(), HttpMethod.GET, null, videoResponseType);

        assertTrue(videoResponse.getBody().isSuccess());
        VideoDetailsResponse videoDetails = videoResponse.getBody().getData();

        // 3. Get video captions
        log.info("Step 3: Getting video captions");
        URI captionsUri = UriComponentsBuilder.fromUriString(buildUrl("video/captions"))
                .queryParam("url", TEST_VIDEO_URL)
                .build()
                .toUri();

        ParameterizedTypeReference<R<List<CaptionResponse>>> captionsResponseType =
                new ParameterizedTypeReference<R<List<CaptionResponse>>>() {};

        ResponseEntity<R<List<CaptionResponse>>> captionsResponse = restTemplate.exchange(
                captionsUri.toString(), HttpMethod.GET, null, captionsResponseType);

        assertTrue(captionsResponse.getBody().isSuccess());
        List<CaptionResponse> captions = captionsResponse.getBody().getData();

        // 4. Download subtitles if available
        if (!captions.isEmpty()) {
            log.info("Step 4: Downloading subtitles");
            String subtitlesUrl = buildUrl("subtitles") + "?url=" + TEST_VIDEO_URL;

            ParameterizedTypeReference<R<YtbSubtitlesVO>> subtitlesResponseType =
                    new ParameterizedTypeReference<R<YtbSubtitlesVO>>() {};

            ResponseEntity<R<YtbSubtitlesVO>> subtitlesResponse = restTemplate.exchange(
                    subtitlesUrl, HttpMethod.GET, null, subtitlesResponseType);

            assertTrue(subtitlesResponse.getBody().isSuccess());
            YtbSubtitlesVO subtitles = subtitlesResponse.getBody().getData();
            
            assertNotNull(subtitles);
            log.info("Successfully completed full workflow");
        }
    }

    @SneakyThrows
    @Test
    void testApiErrorHandling() {
        // Test various error scenarios
        
        // 1. Empty URL
        String emptyUrl = "";
        URI uri1 = UriComponentsBuilder.fromUriString(buildUrl("video/details"))
                .queryParam("url", emptyUrl)
                .build()
                .toUri();

        ResponseEntity<R> response1 = restTemplate.getForEntity(uri1, R.class);
        assertFalse(response1.getBody().isSuccess());

        // 2. Malformed URL
        String malformedUrl = URLEncoder.encode("not-a-url", String.valueOf(StandardCharsets.UTF_8));
        URI uri2 = UriComponentsBuilder.fromUriString(buildUrl("video/details"))
                .queryParam("url", malformedUrl)
                .build()
                .toUri();

        ResponseEntity<R> response2 = restTemplate.getForEntity(uri2, R.class);
        assertFalse(response2.getBody().isSuccess());

        log.info("Error handling tests completed successfully");
    }
}