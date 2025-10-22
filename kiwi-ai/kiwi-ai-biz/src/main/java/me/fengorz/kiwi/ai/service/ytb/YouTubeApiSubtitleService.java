package me.fengorz.kiwi.ai.service.ytb;

import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.ai.api.vo.ytb.CaptionResponse;
import me.fengorz.kiwi.ai.config.YouTubeApiProperties;
import me.fengorz.kiwi.common.sdk.exception.ServiceException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Service focusing on subtitle (caption) download via YouTube Data API.
 * Notes:
 * - Downloading caption content requires OAuth 2.0 with a token authorized for the video owner.
 * - API requires: Authorization: Bearer <ACCESS_TOKEN>, scope typically https://www.googleapis.com/auth/youtube.force-ssl
 * - Use alt=media to receive file bytes and tfmt to choose format (srt or ttml). Optional tlang for translation.
 */
@Slf4j
@Service
public class YouTubeApiSubtitleService {

    private final RestTemplate restTemplate;
    private final YouTubeApiProperties properties;
    private final YouTubeApiService youTubeApiService;

    public YouTubeApiSubtitleService(
            @Qualifier("aiRestTemplate") RestTemplate restTemplate,
            YouTubeApiProperties properties,
            YouTubeApiService youTubeApiService) {
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.youTubeApiService = youTubeApiService;
    }

    /**
     * Download a caption track content by captionId with OAuth token.
     *
     * Contract:
     * - Inputs: captionId (required), accessToken (required), tfmt (optional: srt or ttml), tlang (optional ISO-639-1).
     * - Output: caption file content as String (UTF-8).
     * - Errors: ServiceException wrapping 4xx/5xx, with helpful hints for 401/403.
     */
    public String downloadCaption(String captionId, String accessToken, String tfmt, String tlang) {
        if (!StringUtils.hasText(captionId)) {
            throw new ServiceException("captionId must not be empty");
        }
        if (!StringUtils.hasText(accessToken)) {
            throw new ServiceException("OAuth access token is required to download captions");
        }
        String format = hasTextOrDefault(tfmt, "srt");
        if (!("srt".equalsIgnoreCase(format) || "ttml".equalsIgnoreCase(format))) {
            throw new ServiceException("Unsupported caption format: " + tfmt + ". Use 'srt' or 'ttml'.");
        }

        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(properties.getBaseUrl() + "/captions/" + captionId)
                .queryParam("alt", "media")
                .queryParam("tfmt", format.toLowerCase());
        if (StringUtils.hasText(tlang)) {
            builder.queryParam("tlang", tlang);
        }
        String url = builder.build(true).toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        // text/plain is typical for subtitle downloads
        headers.setAccept(Arrays.asList(MediaType.TEXT_PLAIN, MediaType.APPLICATION_OCTET_STREAM));

        try {
            ResponseEntity<byte[]> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    byte[].class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return new String(response.getBody(), StandardCharsets.UTF_8);
            }
            throw new ServiceException("Caption download failed: HTTP " + response.getStatusCodeValue());
        } catch (HttpClientErrorException e) {
            HttpStatus status = e.getStatusCode();
            String msg;
            if (status == HttpStatus.UNAUTHORIZED) {
                msg = "Unauthorized (401). The access token is missing/expired. Ensure OAuth token with scope 'youtube.force-ssl'.";
            } else if (status == HttpStatus.FORBIDDEN) {
                msg = "Forbidden (403). You must be the video owner or have CMS permissions to download captions.";
            } else if (status == HttpStatus.NOT_FOUND) {
                msg = "Not Found (404). Caption ID might be invalid or you lack access.";
            } else {
                msg = "Caption download failed: " + status + ". Body: " + e.getResponseBodyAsString();
            }
            log.warn("Caption download error: {} - {}", status, e.getResponseBodyAsString());
            throw new ServiceException(msg, e);
        } catch (Exception e) {
            log.error("Unexpected error downloading caption {}", captionId, e);
            throw new ServiceException("Unexpected error downloading caption", e);
        }
    }

    /**
     * Convenience: choose a caption track for a video and download it.
     * Selection rules:
     * - Try to find track with exact language match. If preferAuto is specified, prioritize that kind.
     * - Otherwise choose the first available caption (stable sort: non-draft preferred, then manual over auto).
     */
    public String downloadCaptionByVideo(String videoUrlOrId,
                                         String language,
                                         boolean preferAuto,
                                         String accessToken,
                                         String tfmt,
                                         String tlang) {
        List<CaptionResponse> tracks = youTubeApiService.getVideoCaptions(videoUrlOrId);
        if (tracks == null || tracks.isEmpty()) {
            throw new ServiceException("No caption tracks found for the video.");
        }

        Optional<CaptionResponse> picked = pickCaptionTrack(tracks, language, preferAuto);
        if (!picked.isPresent()) {
            throw new ServiceException("No matching caption track found for language: " + language);
        }
        return downloadCaption(picked.get().getId(), accessToken, tfmt, tlang);
    }

    private Optional<CaptionResponse> pickCaptionTrack(List<CaptionResponse> tracks, String language, boolean preferAuto) {
        // Sort: drafts last, ASR last (manual first)
        List<CaptionResponse> sorted = new ArrayList<>(tracks);
        sorted.sort(Comparator
                .comparing((CaptionResponse c) -> Boolean.TRUE.equals(c.getIsDraft()))
                .thenComparing(c -> "ASR".equalsIgnoreCase(nullToEmpty(c.getTrackKind()))));

        // If language provided, try exact language match first (prefer manual unless preferAuto)
        if (StringUtils.hasText(language)) {
            if (!preferAuto) {
                for (CaptionResponse c : sorted) {
                    if (languageEquals(c.getLanguage(), language) && !"ASR".equalsIgnoreCase(nullToEmpty(c.getTrackKind()))) {
                        return Optional.of(c);
                    }
                }
            }
            // If preferAuto or manual not found, try auto track
            for (CaptionResponse c : sorted) {
                if (languageEquals(c.getLanguage(), language) && "ASR".equalsIgnoreCase(nullToEmpty(c.getTrackKind()))) {
                    return Optional.of(c);
                }
            }
            // Fallback to any track with that language
            for (CaptionResponse c : sorted) {
                if (languageEquals(c.getLanguage(), language)) {
                    return Optional.of(c);
                }
            }
        }
        // Final fallback: first available track
        if (!sorted.isEmpty()) {
            return Optional.of(sorted.get(0));
        }
        return Optional.empty();
    }

    private static boolean languageEquals(String a, String b) {
        if (!StringUtils.hasText(a) || !StringUtils.hasText(b)) return false;
        String aNorm = a.toLowerCase();
        String bNorm = b.toLowerCase();
        if (aNorm.equals(bNorm)) return true;
        // handle forms like en, en-US, en_US
        String aBase = aNorm.split("[-_]")[0];
        String bBase = bNorm.split("[-_]")[0];
        return aBase.equals(bBase);
    }

    private static String hasTextOrDefault(String value, String def) {
        return StringUtils.hasText(value) ? value : def;
    }

    private static String nullToEmpty(String s) { return s == null ? "" : s; }
}
