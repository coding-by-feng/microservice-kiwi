package me.fengorz.kiwi.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * OAuth configuration for accessing YouTube caption content
 * Required for downloading actual caption text
 */
@Data
@Component
@ConfigurationProperties(prefix = "youtube.oauth")
public class YouTubeOAuthConfig {
    
    private String clientId;
    private String clientSecret;
    private String redirectUri;
    private String scope = "https://www.googleapis.com/auth/youtube.force-ssl";
    private boolean enabled = false;
}