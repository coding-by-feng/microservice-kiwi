package me.fengorz.kiwi.ai.config;

import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.ai.service.ytb.YouTuBeApiHelper;
import me.fengorz.kiwi.common.ytb.YouTuBeHelper;
import me.fengorz.kiwi.common.ytb.YouTubeClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Provides a single YouTubeClient bean that can switch between API and yt-dlp modes
 * by setting property: youtube.mode=api | yt-dlp
 * Default: api
 */
@Slf4j
@Configuration
public class YouTubeClientConfig {

    @Bean
    @Primary
    @ConditionalOnProperty(name = "youtube.mode", havingValue = "api", matchIfMissing = true)
    public YouTubeClient apiYouTubeClient(YouTuBeApiHelper helper) {
        log.info("YouTubeClient configured for API mode");
        return helper; // helper implements interface
    }

    @Bean
    @Primary
    @ConditionalOnProperty(name = "youtube.mode", havingValue = "yt-dlp")
    public YouTubeClient ytDlpYouTubeClient(YouTuBeHelper helper) {
        log.info("YouTubeClient configured for yt-dlp mode");
        return helper; // helper implements interface
    }
}

