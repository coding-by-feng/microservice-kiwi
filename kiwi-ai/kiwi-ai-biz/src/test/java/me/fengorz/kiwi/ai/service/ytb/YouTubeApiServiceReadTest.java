package me.fengorz.kiwi.ai.service.ytb;

import me.fengorz.kiwi.ai.api.vo.ytb.CaptionResponse;
import me.fengorz.kiwi.ai.api.vo.ytb.ChannelDetailsResponse;
import me.fengorz.kiwi.ai.api.vo.ytb.ChannelVideoResponse;
import me.fengorz.kiwi.ai.api.vo.ytb.VideoDetailsResponse;
import me.fengorz.kiwi.ai.config.YouTubeApiProperties;
import me.fengorz.kiwi.common.sdk.exception.ServiceException;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Integration-style tests for YouTubeApiService using real YouTube Data API v3 (read-only paths).
 *
 * These tests require environment variable YTB_API_KEY to be set.
 * If not set, tests will be skipped.
 */
public class YouTubeApiServiceReadTest {

    private String apiKey;

    @Before
    public void checkApiKey() {
        apiKey = System.getenv("YTB_API_KEY");
        if (apiKey == null || apiKey.trim().isEmpty()) {
            // Also allow system property as fallback in CI
            apiKey = System.getProperty("YTB_API_KEY");
        }
        Assume.assumeTrue("YTB_API_KEY must be set to run read API tests", apiKey != null && !apiKey.trim().isEmpty());
    }

    private YouTubeApiService newService(int timeoutMs, int maxResultsPerPage, int maxVideosPerChannel) {
        SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
        rf.setConnectTimeout(timeoutMs);
        rf.setReadTimeout(timeoutMs);
        RestTemplate rt = new RestTemplate(rf);

        YouTubeApiProperties props = new YouTubeApiProperties();
        props.setKey(apiKey);
        props.setMaxResultsPerPage(maxResultsPerPage);
        props.setMaxVideosPerChannel(maxVideosPerChannel);

        return new YouTubeApiService(rt, props);
    }

    private void assumeNotQuotaOrForbidden(ServiceException e) {
        String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
        if (msg.contains("403") || msg.contains("forbidden") || msg.contains("quota") || msg.contains("rate limit") || msg.contains("429")) {
            Assume.assumeTrue("Skipping due to API quota/forbidden: " + e.getMessage(), false);
        }
    }

    @Test(timeout = 30000)
    public void getVideoDetails_returnsData() {
        YouTubeApiService svc = newService(15000, 10, 50);
        // A well-known public video ID
        String videoId = "dQw4w9WgXcQ";
        try {
            VideoDetailsResponse resp = svc.getVideoDetails(videoId);
            assertNotNull(resp);
            assertEquals(videoId, resp.getVideoId());
            assertNotNull(resp.getTitle());
            assertTrue(resp.getViewCount() >= 0);
        } catch (ServiceException e) {
            assumeNotQuotaOrForbidden(e);
            throw e; // rethrow if other cause
        }
    }

    @Test(timeout = 30000)
    public void getVideoDetails_byFullUrl_returnsData() {
        YouTubeApiService svc = newService(15000, 10, 50);
        String videoUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ";
        try {
            VideoDetailsResponse resp = svc.getVideoDetails(videoUrl);
            assertNotNull(resp);
            assertEquals("dQw4w9WgXcQ", resp.getVideoId());
            assertNotNull(resp.getTitle());
        } catch (ServiceException e) {
            assumeNotQuotaOrForbidden(e);
            throw e;
        }
    }

    @Test(timeout = 30000)
    public void getChannelDetails_byHandle_returnsData() {
        YouTubeApiService svc = newService(15000, 10, 50);
        String handleUrl = "https://www.youtube.com/@GoogleDevelopers";
        try {
            ChannelDetailsResponse resp = svc.getChannelDetails(handleUrl);
            assertNotNull(resp);
            assertNotNull(resp.getChannelId());
            assertNotNull(resp.getTitle());
        } catch (ServiceException e) {
            assumeNotQuotaOrForbidden(e);
            throw e;
        }
    }

    @Test(timeout = 30000)
    public void getChannelDetails_byChannelIdUrl_returnsData() {
        YouTubeApiService svc = newService(15000, 10, 50);
        String channelUrl = "https://www.youtube.com/channel/UC_x5XG1OV2P6uZZ5FSM9Ttw"; // Google Developers
        try {
            ChannelDetailsResponse resp = svc.getChannelDetails(channelUrl);
            assertNotNull(resp);
            assertEquals("UC_x5XG1OV2P6uZZ5FSM9Ttw", resp.getChannelId());
            assertNotNull(resp.getTitle());
        } catch (ServiceException e) {
            assumeNotQuotaOrForbidden(e);
            throw e;
        }
    }

    @Test(timeout = 40000)
    public void getChannelVideos_respectsLimit() {
        // Keep small limits to avoid long runs
        YouTubeApiService svc = newService(20000, 5, 7);
        String handleUrl = "https://www.youtube.com/@GoogleDevelopers";
        try {
            List<ChannelVideoResponse> videos = svc.getChannelVideos(handleUrl);
            assertNotNull(videos);
            assertFalse(videos.isEmpty());
            assertTrue(videos.size() <= 10);
        } catch (ServiceException e) {
            assumeNotQuotaOrForbidden(e);
            throw e;
        }
    }

    @Test(timeout = 30000)
    public void getVideoCaptions_andAttemptDownload_expectDownloadFailsWithApiKey() {
        YouTubeApiService svc = newService(15000, 10, 50);
        String videoId = "dQw4w9WgXcQ"; // any video with possible captions
        try {
            List<CaptionResponse> captions = svc.getVideoCaptions(videoId);
            assertNotNull(captions);
            // We can't guarantee captions exist for the sample video; if none, just return
            if (captions.isEmpty()) {
                return;
            }
            String captionId = captions.get(0).getId();
            try {
                svc.downloadCaption(captionId);
                fail("Expected ServiceException for caption download without OAuth");
            } catch (ServiceException expected) {
                // OK, download requires OAuth; however, skip if forbidden/quota
                assumeNotQuotaOrForbidden(expected);
            }
        } catch (ServiceException e) {
            assumeNotQuotaOrForbidden(e);
            throw e;
        }
    }

    @Test(timeout = 15000, expected = ServiceException.class)
    public void getVideoDetails_invalidId_throws() {
        YouTubeApiService svc = newService(10000, 5, 5);
        String invalid = "notAValidId";
        try {
            svc.getVideoDetails(invalid);
        } catch (ServiceException e) {
            // If it's quota/forbidden, skip; otherwise rethrow to satisfy expected exception
            assumeNotQuotaOrForbidden(e);
            throw e;
        }
    }
}
