package me.fengorz.kiwi.common.ytb;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class YouTuBeHelperTest {

    private static final Logger log = LoggerFactory.getLogger(YouTuBeHelperTest.class);

    @Test
    @Disabled
    void extractChannelNameWithYtDlp_Success() throws Exception {
        // Arrange
        String channelUrl = "https://www.youtube.com/c/ScientificAmerican";
        String expectedChannelName = "Scientific American";

        // Act
        String result = new YouTuBeHelper().extractChannelNameWithYtDlp(channelUrl);

        // Assert
        assertEquals(expectedChannelName, result);
    }

    @Test
    @Disabled
    void testTitle() throws Exception {
        String videoTitle = new YouTuBeHelper().getVideoTitle("https://www.youtube.com/watch?v=hS-KO9RRmSc");

        // Assert
        assertEquals("Nintendo Direct Switch 2 Highlights : Everything Announced in 15 Minutes",
                videoTitle);
    }

    @Test
    @Disabled
    void extractAllVideoLinks_Success() throws Exception {
        // Arrange
        String channelUrl = "https://www.youtube.com/c/ScientificAmerican";
        String expectedChannelName = "Scientific American";

        // Act
        List<String> videoLinks = new YouTuBeHelper().extractAllVideoLinks(channelUrl);

        log.info("Extracting all video links from channel: {}", channelUrl);
        log.info("Video links: {}", videoLinks);

        // Assert
        assertFalse(videoLinks.isEmpty());
    }

    @Test
    void testDownloadSubtitles() throws Exception {
        YouTuBeHelper helper = new YouTuBeHelper();

        // Inject configuration values
        setField(helper, "downloadPath", System.getProperty("java.io.tmpdir"));
        setField(helper, "subtitlesLangs", "en");
        setField(helper, "largeSubtitlesThreshold", 1000);
        // command has default "yt-dlp" but we can ensure it
        setField(helper, "command", "yt-dlp");

        String videoUrl = "https://www.youtube.com/watch?v=xawn4C43TWo";
        log.info("Downloading subtitles for: {}", videoUrl);

        YtbSubtitlesResult result = helper.downloadSubtitles(videoUrl);

        log.info("Download Result: {}", result);
        assertNotNull(result);
        assertNotNull(result.getScrollingSubtitles());
        assertFalse(result.getScrollingSubtitles().isEmpty());

        // Verify that we got some content
        log.info("Subtitle content length: {}", result.getScrollingSubtitles().length());
        log.info("Subtitle preview: {}",
                result.getScrollingSubtitles().substring(0, Math.min(2000, result.getScrollingSubtitles().length())));
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

}