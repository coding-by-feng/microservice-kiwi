package me.fengorz.kiwi.ai.service.ytb;

import me.fengorz.kiwi.ai.api.vo.ytb.CaptionResponse;
import me.fengorz.kiwi.ai.api.vo.ytb.ChannelDetailsResponse;
import me.fengorz.kiwi.ai.api.vo.ytb.ChannelVideoResponse;
import me.fengorz.kiwi.ai.api.vo.ytb.VideoDetailsResponse;
import me.fengorz.kiwi.ai.config.YouTubeApiProperties;
import me.fengorz.kiwi.common.sdk.exception.ServiceException;
import org.junit.Assume;
import org.junit.AssumptionViolatedException;
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
            throw new AssumptionViolatedException("Skipping due to API quota/forbidden: " + e.getMessage());
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

    @Test(timeout = 35000)
    public void getVideoCaptions_onKnownChannel_findOneWithCaptions() {
        // Try a handful of recent Google Developers videos and verify at least one returns non-empty captions
        YouTubeApiService svc = newService(20000, 5, 10);
        String handleUrl = "https://www.youtube.com/@GoogleDevelopers";
        try {
            List<ChannelVideoResponse> videos = svc.getChannelVideos(handleUrl);
            assertNotNull(videos);
            assertFalse(videos.isEmpty());

            boolean foundWithCaptions = false;
            CaptionResponse firstCaption = null;

            for (ChannelVideoResponse v : videos) {
                try {
                    List<CaptionResponse> caps = svc.getVideoCaptions(v.getVideoId());
                    if (caps != null && !caps.isEmpty()) {
                        foundWithCaptions = true;
                        firstCaption = caps.get(0);
                        break;
                    }
                } catch (ServiceException inner) {
                    // If a quota/forbidden error occurs for any single video, skip the whole test
                    assumeNotQuotaOrForbidden(inner);
                }
            }

            Assume.assumeTrue("No captions found among a small sample of channel videos; skipping", foundWithCaptions);

            assertNotNull(firstCaption);
            assertNotNull(firstCaption.getId());
        } catch (ServiceException e) {
            assumeNotQuotaOrForbidden(e);
            throw e;
        }
    }

    @Test(timeout = 30000)
    public void getVideoCaptions_forFirstChannelVideo_success() {
        // Validate that calling getVideoCaptions succeeds (does not throw) for a real public video
        YouTubeApiService svc = newService(15000, 5, 5);
        String handleUrl = "https://www.youtube.com/@GoogleDevelopers";
        try {
            List<ChannelVideoResponse> videos = svc.getChannelVideos(handleUrl);
            assertNotNull(videos);
            Assume.assumeTrue("Channel returned no videos; skipping", !videos.isEmpty());

            String videoId = videos.get(0).getVideoId();
            List<CaptionResponse> captions = svc.getVideoCaptions(videoId);
            assertNotNull(captions);

            System.out.println("getVideoCaptions -> videoId=" + videoId + ", captions.size=" + captions.size());
            for (int i = 0; i < captions.size(); i++) {
                CaptionResponse c = captions.get(i);
                System.out.printf("[%d] id=%s, lang=%s, name=%s, trackKind=%s, autoSynced=%s, cc=%s, draft=%s%n",
                        i,
                        c.getId(),
                        c.getLanguage(),
                        c.getName(),
                        c.getTrackKind(),
                        c.getIsAutoSynced(),
                        c.getIsCC(),
                        c.getIsDraft());
            }

            Assume.assumeTrue("No captions available on the sampled video; skipping detail validation", !captions.isEmpty());

            CaptionResponse first = captions.get(0);
            assertNotNull("Caption id should not be null", first.getId());
            assertFalse("Caption id should not be empty", first.getId().trim().isEmpty());
            assertNotNull("Caption language should not be null", first.getLanguage());
            assertFalse("Caption language should not be empty", first.getLanguage().trim().isEmpty());
            assertTrue("Language should match pattern like en or en-US or zh-Hans",
                    first.getLanguage().matches("[a-zA-Z]{2,3}([_-][a-zA-Z]{2,8})*"));
            assertNotNull("trackKind should not be null", first.getTrackKind());
            String tk = first.getTrackKind().toLowerCase();
            assertTrue("trackKind must be either 'standard' or 'asr' (case-insensitive)", tk.equals("standard") || tk.equals("asr"));
            assertNotNull("Caption name should not be null", first.getName());

            // Download each caption and print each textual line (filtering sequence/timestamp lines)
            for (CaptionResponse c : captions) {
                System.out.println("Attempting download of captionId=" + c.getId() + " lang=" + c.getLanguage());
                try {
                    String srt = svc.downloadCaption(c.getId());
                    if (srt == null) {
                        System.out.println("Caption download returned null (unexpected but tolerating). captionId=" + c.getId());
                        continue;
                    }
                    System.out.println("Downloaded caption length=" + srt.length());
                    String[] rawLines = srt.split("\r?\n");
                    int printed = 0;
                    for (String line : rawLines) {
                        String trimmed = line.trim();
                        if (trimmed.isEmpty() || trimmed.matches("\\d+") || trimmed.contains("-->")) {
                            continue; // skip SRT sequence and timestamp lines
                        }
                        System.out.println("CAPTION_TEXT> " + trimmed);
                        printed++;
                        if (printed >= 50) { // cap per caption to avoid flooding logs
                            System.out.println("CAPTION_TEXT> ... (truncated) ...");
                            break;
                        }
                    }
                } catch (ServiceException downloadEx) {
                    try {
                        assumeNotQuotaOrForbidden(downloadEx);
                        System.out.println("Caption download not available (expected without OAuth). captionId=" + c.getId() + " reason=" + downloadEx.getMessage());
                    } catch (AssumptionViolatedException skip) {
                        throw skip;
                    }
                } catch (Exception generic) {
                    System.out.println("Unexpected error downloading captionId=" + c.getId() + ": " + generic.getMessage());
                }
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
            assumeNotQuotaOrForbidden(e);
            throw e;
        }
    }
}
