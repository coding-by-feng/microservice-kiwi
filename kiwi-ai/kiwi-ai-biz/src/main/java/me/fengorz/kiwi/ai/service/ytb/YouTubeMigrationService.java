package me.fengorz.kiwi.ai.service.ytb;

import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.ytb.YouTuBeHelper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Migration service to handle transition from yt-dlp to YouTube API
 * Provides backward compatibility and gradual migration
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "youtube.migration.enabled", havingValue = "true")
public class YouTubeMigrationService {

    private final YouTuBeHelper legacyHelper;
    private final YouTuBeHelperV2 apiHelper;
    
    public YouTubeMigrationService(YouTuBeHelper legacyHelper, YouTuBeHelperV2 apiHelper) {
        this.legacyHelper = legacyHelper;
        this.apiHelper = apiHelper;
    }

    /**
     * Fallback method that tries API first, then falls back to yt-dlp
     */
    public String getVideoTitleWithFallback(String videoUrl) {
        try {
            log.info("Attempting to get video title using YouTube API");
            return apiHelper.getVideoTitle(videoUrl);
        } catch (Exception e) {
            log.warn("YouTube API failed, falling back to yt-dlp: {}", e.getMessage());
            try {
                return legacyHelper.getVideoTitle(videoUrl);
            } catch (Exception fallbackError) {
                log.error("Both API and yt-dlp failed", fallbackError);
                throw new RuntimeException("Failed to get video title with both methods", fallbackError);
            }
        }
    }

    /**
     * Hybrid approach for channel name extraction
     */
    public String extractChannelNameWithFallback(String channelUrl) {
        try {
            log.info("Attempting to extract channel name using YouTube API");
            return apiHelper.extractChannelNameWithYtDlp(channelUrl);
        } catch (Exception e) {
            log.warn("YouTube API failed, falling back to yt-dlp: {}", e.getMessage());
            try {
                return legacyHelper.extractChannelNameWithYtDlp(channelUrl);
            } catch (Exception fallbackError) {
                log.error("Both API and yt-dlp failed", fallbackError);
                throw new RuntimeException("Failed to extract channel name with both methods", fallbackError);
            }
        }
    }
}