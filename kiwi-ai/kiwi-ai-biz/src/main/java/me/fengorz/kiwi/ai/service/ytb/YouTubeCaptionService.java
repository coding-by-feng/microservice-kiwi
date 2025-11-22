package me.fengorz.kiwi.ai.service.ytb;

import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.ai.config.YouTubeOAuthConfig;
import me.fengorz.kiwi.common.sdk.exception.ServiceException;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Service for handling YouTube caption operations with OAuth support
 */
@Slf4j
@Service
public class YouTubeCaptionService {

    private final RestTemplate restTemplate;
    private final YouTubeOAuthConfig oauthConfig;

    public YouTubeCaptionService(RestTemplate restTemplate, YouTubeOAuthConfig oauthConfig) {
        this.restTemplate = restTemplate;
        this.oauthConfig = oauthConfig;
    }

    /**
     * Download caption content with OAuth token
     * This requires implementing OAuth flow for user authorization
     */
    public String downloadCaptionWithOAuth(String captionId, String accessToken) {
        if (!oauthConfig.isEnabled()) {
            throw new ServiceException("OAuth is not enabled for caption download");
        }

        String url = "https://www.googleapis.com/youtube/v3/captions/" + captionId + "?fmt=srt";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setAccept(MediaType.parseMediaTypes("text/plain, application/json"));

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                return response.getBody();
            } else {
                throw new ServiceException("Failed to download caption: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error downloading caption with OAuth", e);
            throw new ServiceException("Failed to download caption content", e);
        }
    }

    /**
     * Generate OAuth authorization URL for caption access
     */
    public String generateAuthorizationUrl(String state) {
        if (!oauthConfig.isEnabled()) {
            throw new ServiceException("OAuth is not configured");
        }

        return "https://accounts.google.com/o/oauth2/v2/auth" +
                "?client_id=" + oauthConfig.getClientId() +
                "&redirect_uri=" + oauthConfig.getRedirectUri() +
                "&scope=" + oauthConfig.getScope() +
                "&response_type=code" +
                "&access_type=offline" +
                "&state=" + state;
    }
}