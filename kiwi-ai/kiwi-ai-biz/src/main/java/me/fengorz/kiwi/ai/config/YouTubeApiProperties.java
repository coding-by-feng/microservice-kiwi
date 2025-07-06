package me.fengorz.kiwi.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.Serializable;

@Data
@Component
@ConfigurationProperties(prefix = "youtube.api")
public class YouTubeApiProperties implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * YouTube Data API v3 key
     */
    private String key;

    /**
     * YouTube Data API v3 base URL
     */
    private String baseUrl = "https://www.googleapis.com/youtube/v3";

    /**
     * Maximum results per page for channel videos
     */
    private Integer maxResultsPerPage = 50;

    /**
     * Default region code for API requests
     */
    private String regionCode = "US";

    /**
     * Request timeout in milliseconds
     */
    private Integer timeoutMs = 30000;

    /**
     * Maximum videos to fetch per channel (for pagination)
     */
    private Integer maxVideosPerChannel = 1000;
}