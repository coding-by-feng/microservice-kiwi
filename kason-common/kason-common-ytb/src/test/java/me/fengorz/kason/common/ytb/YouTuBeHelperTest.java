package me.fengorz.kason.common.ytb;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@Slf4j
class YouTuBeHelperTest {

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
    void testTitle() throws Exception {
        String videoTitle = new YouTuBeHelper().getVideoTitle("https://www.youtube.com/watch?v=hS-KO9RRmSc");

        // Assert
        assertEquals("Nintendo Switch 2 Features Unveiled at Nintendo Direct: Everything Announced in 15 Minutes", videoTitle);
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

}