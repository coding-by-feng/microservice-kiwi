package me.fengorz.kason.ai.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Configuration for YouTube services
 */
@Slf4j
@Configuration
public class YouTubeConfig {

    /**
     * Enable YouTube API service by default
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "youtube.api.enabled", havingValue = "true", matchIfMissing = true)
    public String youtubeApiMode() {
        log.info("YouTube API mode enabled");
        return "api";
    }

    /**
     * Fallback to yt-dlp when API is disabled
     */
    @Bean
    @ConditionalOnProperty(name = "youtube.api.enabled", havingValue = "false")
    public String youtubeLegacyMode() {
        log.info("YouTube legacy (yt-dlp) mode enabled");
        return "legacy";
    }
}