package me.fengorz.kiwi.common.ytb;

import java.io.InputStream;

/**
 * Common abstraction for accessing YouTube resources either via yt-dlp CLI or YouTube Data API.
 * Implementations should handle caching at a higher layer; this interface focuses on raw access.
 */
public interface YouTubeClient {

    /**
     * Download raw video content as InputStream. Implementations that cannot provide this (API) may throw UnsupportedOperationException.
     */
    InputStream downloadVideo(String videoUrl);

    /**
     * Download (or fetch) subtitles for a video URL.
     */
    YtbSubtitlesResult downloadSubtitles(String videoUrl);

    /**
     * Get the human-readable title for a video.
     */
    String getVideoTitle(String videoUrl);

    /**
     * Attempt to retrieve video publish datetime. May return null if unsupported.
     */
    default java.time.LocalDateTime getVideoPublishedAt(String videoUrl) { return null; }
}
