package me.fengorz.kiwi.ai.service.ytb;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.ai.api.vo.ytb.*;
import me.fengorz.kiwi.ai.config.YouTubeApiProperties;
import me.fengorz.kiwi.common.sdk.exception.ServiceException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class YouTubeApiService {

    private final RestTemplate restTemplate;
    private final YouTubeApiProperties properties;
    private final ObjectMapper objectMapper;
    
    // Patterns for extracting IDs from URLs
    private static final Pattern VIDEO_ID_PATTERN = Pattern.compile(
        "(?:youtube\\.com/watch\\?v=|youtu\\.be/|youtube\\.com/embed/)([a-zA-Z0-9_-]{11})"
    );
    
    private static final Pattern CHANNEL_ID_PATTERN = Pattern.compile(
        "youtube\\.com/channel/([a-zA-Z0-9_-]+)"
    );
    
    private static final Pattern CHANNEL_HANDLE_PATTERN = Pattern.compile(
        "youtube\\.com/@([a-zA-Z0-9_.-]+)"
    );
    
    private static final Pattern CHANNEL_USER_PATTERN = Pattern.compile(
        "youtube\\.com/user/([a-zA-Z0-9_.-]+)"
    );

    public YouTubeApiService(@Qualifier("aiRestTemplate") RestTemplate restTemplate,
                           YouTubeApiProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Ensure API key is present, base URL is trusted, and not an unresolved placeholder.
     */
    private String getApiKeyOrThrow() {
        // Validate base URL to avoid open redirect / untrusted host usage
        String baseUrl = properties.getBaseUrl();
        if (!isTrustedBaseUrl(baseUrl)) {
            log.error("Untrusted YouTube API baseUrl configured: {}", baseUrl);
            throw new ServiceException("Untrusted YouTube API baseUrl configured");
        }

        String key = properties.getKey();
        if (key == null || key.trim().isEmpty() || key.contains("{") || key.contains("}")) {
            log.warn("YouTube API key is missing or unresolved. Check env YTB_API_KEY or property 'youtube.api.key'.");
            throw new ServiceException("YouTube API key is not configured. Set env YTB_API_KEY or property 'youtube.api.key'.");
        }
        return key;
    }

    private boolean isTrustedBaseUrl(String baseUrl) {
        if (baseUrl == null) {
            return false;
        }
        String lower = baseUrl.toLowerCase();
        return lower.startsWith("https://www.googleapis.com/youtube/v3");
    }

    /**
     * Get video details by video URL or ID
     */
    public VideoDetailsResponse getVideoDetails(String videoUrlOrId) {
        log.info("Getting video details for input: {}", videoUrlOrId);

        String videoId = extractVideoId(videoUrlOrId);
        log.debug("Resolved videoId: {} from input: {}", videoId, videoUrlOrId);
        if (videoId == null) {
            log.warn("Invalid video URL or ID: {}", videoUrlOrId);
            throw new ServiceException("Invalid video URL or ID: " + videoUrlOrId);
        }

        String url = UriComponentsBuilder.fromHttpUrl(properties.getBaseUrl() + "/videos")
                .queryParam("part", "snippet,contentDetails,statistics")
                .queryParam("id", videoId)
                .queryParam("key", getApiKeyOrThrow())
                .build()
                .toUriString();
        log.debug("YouTube getVideoDetails URL: {}", sanitizeUrl(url));

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            log.debug("YouTube getVideoDetails response status: {}", response.getStatusCode());

            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode items = root.get("items");
                int size = items != null && items.isArray() ? items.size() : -1;
                log.debug("YouTube getVideoDetails items size: {} for videoId: {}", size, videoId);

                if (items != null && items.isArray() && items.size() > 0) {
                    JsonNode video = items.get(0);
                    JsonNode snippet = video.get("snippet");
                    String title = snippet != null && snippet.has("title") ? snippet.get("title").asText() : null;
                    log.info("Fetched video details. videoId={}, title={}", videoId, title);

                    return VideoDetailsResponse.builder()
                            .videoId(videoId)
                            .title(title)
                            .description(snippet != null && snippet.has("description") ? snippet.get("description").asText() : null)
                            .channelId(snippet != null && snippet.has("channelId") ? snippet.get("channelId").asText() : null)
                            .channelTitle(snippet != null && snippet.has("channelTitle") ? snippet.get("channelTitle").asText() : null)
                            .publishedAt(snippet != null && snippet.has("publishedAt") ? snippet.get("publishedAt").asText() : null)
                            .thumbnails(extractThumbnails(snippet != null ? snippet.get("thumbnails") : null))
                            .duration(video.has("contentDetails") && video.get("contentDetails").has("duration") ? video.get("contentDetails").get("duration").asText() : null)
                            .viewCount(video.has("statistics") && video.get("statistics").has("viewCount") ? video.get("statistics").get("viewCount").asLong() : 0L)
                            .build();
                } else {
                    log.warn("Video not found for id: {}", videoId);
                    throw new ServiceException("Video not found: " + videoId);
                }
            } else {
                log.error("YouTube API error for getVideoDetails: {} -> {}", videoId, response.getStatusCode());
                throw new ServiceException("YouTube API error: " + response.getStatusCode());
            }
        } catch (IOException e) {
            log.error("Error parsing YouTube API response for getVideoDetails, videoId={}", videoId, e);
            throw new ServiceException("Failed to parse video details", e);
        }
    }

    /**
     * Get channel details by channel URL, ID, or handle
     */
    public ChannelDetailsResponse getChannelDetails(String channelUrlOrId) {
        log.info("Getting channel details for input: {}", channelUrlOrId);

        String channelId = extractChannelId(channelUrlOrId);
        log.debug("Resolved channelId (direct/extract): {}", channelId);

        // If we couldn't extract a channel ID, try to search by handle or username
        if (channelId == null) {
            channelId = findChannelByHandle(channelUrlOrId);
            log.debug("Resolved channelId via handle/username search: {}", channelId);
        }

        if (channelId == null) {
            log.warn("Invalid channel URL or ID: {}", channelUrlOrId);
            throw new ServiceException("Invalid channel URL or ID: " + channelUrlOrId);
        }

        String url = UriComponentsBuilder.fromHttpUrl(properties.getBaseUrl() + "/channels")
                .queryParam("part", "snippet,contentDetails,statistics")
                .queryParam("id", channelId)
                .queryParam("key", getApiKeyOrThrow())
                .build()
                .toUriString();
        log.debug("YouTube getChannelDetails URL: {}", sanitizeUrl(url));

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            log.debug("YouTube getChannelDetails response status: {}", response.getStatusCode());

            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode items = root.get("items");
                int size = items != null && items.isArray() ? items.size() : -1;
                log.debug("YouTube getChannelDetails items size: {} for channelId: {}", size, channelId);

                if (items != null && items.isArray() && items.size() > 0) {
                    JsonNode channel = items.get(0);
                    JsonNode snippet = channel.get("snippet");
                    JsonNode statistics = channel.get("statistics");
                    String title = snippet != null && snippet.has("title") ? snippet.get("title").asText() : null;
                    log.info("Fetched channel details. channelId={}, title={}", channelId, title);

                    return ChannelDetailsResponse.builder()
                            .channelId(channelId)
                            .title(title)
                            .description(snippet != null && snippet.has("description") ? snippet.get("description").asText() : null)
                            .customUrl(snippet != null && snippet.has("customUrl") ? snippet.get("customUrl").asText() : null)
                            .publishedAt(snippet != null && snippet.has("publishedAt") ? snippet.get("publishedAt").asText() : null)
                            .thumbnails(extractThumbnails(snippet != null ? snippet.get("thumbnails") : null))
                            .subscriberCount(statistics != null && statistics.has("subscriberCount") ? statistics.get("subscriberCount").asLong() : 0)
                            .videoCount(statistics != null && statistics.has("videoCount") ? statistics.get("videoCount").asLong() : 0)
                            .viewCount(statistics != null && statistics.has("viewCount") ? statistics.get("viewCount").asLong() : 0)
                            .uploadsPlaylistId(channel.has("contentDetails") && channel.get("contentDetails").has("relatedPlaylists") && channel.get("contentDetails").get("relatedPlaylists").has("uploads") ? channel.get("contentDetails").get("relatedPlaylists").get("uploads").asText() : null)
                            .build();
                } else {
                    log.warn("Channel not found: {}", channelId);
                    throw new ServiceException("Channel not found: " + channelId);
                }
            } else {
                log.error("YouTube API error for getChannelDetails: {} -> {}", channelId, response.getStatusCode());
                throw new ServiceException("YouTube API error: " + response.getStatusCode());
            }
        } catch (IOException e) {
            log.error("Error parsing YouTube API response for getChannelDetails, channelId={}", channelId, e);
            throw new ServiceException("Failed to parse channel details", e);
        }
    }

    /**
     * Get all videos from a channel with pagination
     */
    public List<ChannelVideoResponse> getChannelVideos(String channelUrlOrId) {
        log.info("Getting all videos for channel input: {}", channelUrlOrId);

        ChannelDetailsResponse channelDetails = getChannelDetails(channelUrlOrId);
        String uploadsPlaylistId = channelDetails.getUploadsPlaylistId();
        log.debug("Resolved uploadsPlaylistId: {} for channelId: {}", uploadsPlaylistId, channelDetails.getChannelId());

        List<ChannelVideoResponse> allVideos = new ArrayList<>();
        String nextPageToken = null;
        int totalFetched = 0;

        do {
            log.debug("Fetching playlist items. playlistId={}, pageToken={}", uploadsPlaylistId, nextPageToken);
            PlaylistItemsResponse response = getPlaylistItems(uploadsPlaylistId, nextPageToken);
            int pageSize = response.getItems() != null ? response.getItems().size() : 0;
            if (response.getItems() != null) {
                allVideos.addAll(response.getItems());
            }
            nextPageToken = response.getNextPageToken();
            totalFetched += pageSize;

            log.info("Fetched page of videos: pageSize={}, totalFetched={}, nextPageToken={}", pageSize, totalFetched, nextPageToken);

            // Safety check to prevent infinite loops
            if (totalFetched >= properties.getMaxVideosPerChannel()) {
                log.warn("Reached maximum videos limit ({}) for channel", properties.getMaxVideosPerChannel());
                break;
            }

        } while (nextPageToken != null);

        log.info("Total videos fetched for channel: {}", allVideos.size());
        return allVideos;
    }

    /**
     * Get captions for a video
     */
    public List<CaptionResponse> getVideoCaptions(String videoUrlOrId) {
        log.info("Getting captions for video input: {}", videoUrlOrId);

        String videoId = extractVideoId(videoUrlOrId);
        log.debug("Resolved videoId for captions: {}", videoId);
        if (videoId == null) {
            log.warn("Invalid video URL or ID for captions: {}", videoUrlOrId);
            throw new ServiceException("Invalid video URL or ID: " + videoUrlOrId);
        }

        String url = UriComponentsBuilder.fromHttpUrl(properties.getBaseUrl() + "/captions")
                .queryParam("part", "snippet")
                .queryParam("videoId", videoId)
                .queryParam("key", getApiKeyOrThrow())
                .build()
                .toUriString();
        log.debug("YouTube getVideoCaptions URL: {}", sanitizeUrl(url));

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            log.debug("YouTube getVideoCaptions response status: {}", response.getStatusCode());

            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode items = root.get("items");

                List<CaptionResponse> captions = new ArrayList<>();
                if (items != null) {
                    for (JsonNode item : items) {
                        JsonNode snippet = item.get("snippet");

                        CaptionResponse caption = CaptionResponse.builder()
                                .id(item.get("id").asText())
                                .language(snippet.get("language").asText())
                                .name(snippet.get("name").asText())
                                .trackKind(snippet.get("trackKind").asText())
                                .isAutoSynced(snippet.has("isAutoSynced") && snippet.get("isAutoSynced").asBoolean())
                                .isCC(snippet.has("isCC") && snippet.get("isCC").asBoolean())
                                .isDraft(snippet.has("isDraft") && snippet.get("isDraft").asBoolean())
                                .build();

                        captions.add(caption);
                    }
                }
                log.info("Fetched captions. videoId={}, count={}", videoId, captions.size());

                return captions;
            } else {
                log.error("YouTube API error for getVideoCaptions: {} -> {}", videoId, response.getStatusCode());
                throw new ServiceException("YouTube API error: " + response.getStatusCode());
            }
        } catch (IOException e) {
            log.error("Error parsing YouTube API response for getVideoCaptions, videoId={}", videoId, e);
            throw new ServiceException("Failed to parse captions", e);
        }
    }

    /**
     * Download caption content (requires OAuth, limited functionality with API key only)
     */
    public String downloadCaption(String captionId) {
        log.warn("Attempting to download caption (OAuth required). captionId={}", captionId);

        String url = UriComponentsBuilder.fromHttpUrl(properties.getBaseUrl() + "/captions/" + captionId)
                .queryParam("key", getApiKeyOrThrow())
                .queryParam("fmt", "srt")
                .build()
                .toUriString();
        log.debug("YouTube downloadCaption URL: {}", sanitizeUrl(url));

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            log.debug("YouTube downloadCaption response status: {} for captionId={}", response.getStatusCode(), captionId);

            if (response.getStatusCode() == HttpStatus.OK) {
                log.info("Downloaded caption content successfully. captionId={}", captionId);
                return response.getBody();
            } else {
                log.error("Caption download failed with status {}. captionId={}", response.getStatusCode(), captionId);
                throw new ServiceException("Caption download failed. OAuth required for caption content access.");
            }
        } catch (Exception e) {
            log.error("Error downloading caption. captionId={}", captionId, e);
            throw new ServiceException("Caption download requires OAuth authentication", e);
        }
    }

    // Private helper methods

    private PlaylistItemsResponse getPlaylistItems(String playlistId, String pageToken) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(properties.getBaseUrl() + "/playlistItems")
                .queryParam("part", "snippet")
                .queryParam("playlistId", playlistId)
                .queryParam("maxResults", properties.getMaxResultsPerPage())
                .queryParam("key", getApiKeyOrThrow());

        if (pageToken != null) {
            builder.queryParam("pageToken", pageToken);
        }

        String url = builder.build().toUriString();
        log.debug("YouTube getPlaylistItems URL: {}", sanitizeUrl(url));

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            log.debug("YouTube getPlaylistItems response status: {} for playlistId={}", response.getStatusCode(), playlistId);

            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode root = objectMapper.readTree(response.getBody());

                List<ChannelVideoResponse> videos = new ArrayList<>();
                JsonNode items = root.get("items");
                int itemsCount = items != null && items.isArray() ? items.size() : 0;
                log.debug("Playlist items fetched: {} (before filtering)", itemsCount);

                if (items != null) {
                    for (JsonNode item : items) {
                        JsonNode snippet = item.get("snippet");
                        if (snippet == null) {
                            continue;
                        }
                        // Skip private/deleted videos by title marker
                        String title = snippet.has("title") ? snippet.get("title").asText() : null;
                        if ("Private video".equals(title) || "Deleted video".equals(title)) {
                            continue;
                        }

                        // resourceId.videoId per official doc
                        String videoId = null;
                        if (snippet.has("resourceId") && snippet.get("resourceId").has("videoId")) {
                            videoId = snippet.get("resourceId").get("videoId").asText();
                        }
                        if (videoId == null || videoId.trim().isEmpty()) {
                            // Fallback: ignore this item if no videoId present
                            continue;
                        }

                        ChannelVideoResponse video = ChannelVideoResponse.builder()
                                .videoId(videoId)
                                .title(title)
                                .description(snippet.has("description") ? snippet.get("description").asText() : null)
                                .publishedAt(snippet.has("publishedAt") ? snippet.get("publishedAt").asText() : null)
                                .thumbnails(extractThumbnails(snippet.has("thumbnails") ? snippet.get("thumbnails") : null))
                                .position(snippet.has("position") ? snippet.get("position").asLong() : null)
                                .build();

                        videos.add(video);
                    }
                }

                String next = root.has("nextPageToken") ? root.get("nextPageToken").asText() : null;
                long total = root.has("pageInfo") && root.get("pageInfo").has("totalResults") ? root.get("pageInfo").get("totalResults").asLong() : -1;
                log.info("Processed playlist items page. playlistId={}, videosAdded={}, nextPageToken={}, totalResults={}", playlistId, videos.size(), next, total);

                return PlaylistItemsResponse.builder()
                        .items(videos)
                        .nextPageToken(next)
                        .totalResults(total)
                        .build();

            } else {
                log.error("YouTube API error for getPlaylistItems: playlistId={} -> {}", playlistId, response.getStatusCode());
                throw new ServiceException("YouTube API error: " + response.getStatusCode());
            }
        } catch (IOException e) {
            log.error("Error parsing playlist items response. playlistId={}, pageToken={}", playlistId, pageToken, e);
            throw new ServiceException("Failed to parse playlist items", e);
        }
    }

    private String findChannelByHandle(String handleOrUsername) {
        // Extract handle or username from URL
        String identifier = null;

        Matcher handleMatcher = CHANNEL_HANDLE_PATTERN.matcher(handleOrUsername);
        if (handleMatcher.find()) {
            identifier = handleMatcher.group(1);
        } else {
            Matcher userMatcher = CHANNEL_USER_PATTERN.matcher(handleOrUsername);
            if (userMatcher.find()) {
                identifier = userMatcher.group(1);
            } else if (!handleOrUsername.startsWith("http")) {
                // Assume it's a handle or username
                identifier = handleOrUsername.startsWith("@") ? handleOrUsername.substring(1) : handleOrUsername;
            }
        }

        if (identifier == null) {
            log.debug("Could not resolve identifier from handleOrUsername: {}", handleOrUsername);
            return null;
        }
        log.debug("Resolved identifier for channel search: {} from input: {}", identifier, handleOrUsername);

        // Search for channel by handle/username
        String url = UriComponentsBuilder.fromHttpUrl(properties.getBaseUrl() + "/search")
                .queryParam("part", "snippet")
                .queryParam("type", "channel")
                .queryParam("q", identifier)
                .queryParam("maxResults", 1)
                .queryParam("key", getApiKeyOrThrow())
                .build()
                .toUriString();
        log.debug("YouTube findChannelByHandle URL: {}", sanitizeUrl(url));

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            log.debug("YouTube findChannelByHandle response status: {}", response.getStatusCode());

            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode items = root.get("items");
                int size = items != null && items.isArray() ? items.size() : 0;
                log.debug("findChannelByHandle items size: {} for identifier: {}", size, identifier);

                if (items.isArray() && items.size() > 0) {
                    JsonNode first = items.get(0);
                    if (first.has("id") && first.get("id").has("channelId")) {
                        String resolved = first.get("id").get("channelId").asText();
                        log.info("Resolved channelId={} via handle/username lookup for identifier={}", resolved, identifier);
                        return resolved;
                    }
                }
            }
        } catch (IOException e) {
            log.error("Error searching for channel by identifier: {}", identifier, e);
        }

        log.debug("Channel not found via handle/username search for identifier: {}", identifier);
        return null;
    }

    private String extractVideoId(String videoUrlOrId) {
        if (videoUrlOrId.length() == 11 && !videoUrlOrId.contains("/")) {
            // Likely already a video ID
            log.trace("Input already looks like a video ID: {}", videoUrlOrId);
            return videoUrlOrId;
        }

        try {
            String decoded = URLDecoder.decode(videoUrlOrId, StandardCharsets.UTF_8.name());
            Matcher matcher = VIDEO_ID_PATTERN.matcher(decoded);
            if (matcher.find()) {
                String id = matcher.group(1);
                log.trace("Extracted videoId: {} from input: {}", id, videoUrlOrId);
                return id;
            }
        } catch (Exception e) {
            log.warn("Error decoding video URL: {}", videoUrlOrId);
        }

        log.debug("Failed to extract videoId from input: {}", videoUrlOrId);
        return null;
    }

    private String extractChannelId(String channelUrlOrId) {
        // Direct channel ID
        if (channelUrlOrId.startsWith("UC") && channelUrlOrId.length() > 20 && !channelUrlOrId.contains("/")) {
            log.trace("Input already looks like a channel ID: {}", channelUrlOrId);
            return channelUrlOrId;
        }

        try {
            String decoded = URLDecoder.decode(channelUrlOrId, StandardCharsets.UTF_8.name());

            Matcher channelMatcher = CHANNEL_ID_PATTERN.matcher(decoded);
            if (channelMatcher.find()) {
                String id = channelMatcher.group(1);
                log.trace("Extracted channelId: {} from input: {}", id, channelUrlOrId);
                return id;
            }
        } catch (Exception e) {
            log.warn("Error decoding channel URL: {}", channelUrlOrId);
        }

        log.debug("Failed to extract channelId from input: {}", channelUrlOrId);
        return null;
    }

    private Map<String, String> extractThumbnails(JsonNode thumbnailsNode) {
        Map<String, String> thumbnails = new HashMap<>();
        if (thumbnailsNode == null) {
            log.debug("Thumbnails node is null; returning empty map.");
            return thumbnails;
        }
        thumbnailsNode.fields().forEachRemaining(entry -> thumbnails.put(entry.getKey(), entry.getValue().get("url").asText()));
        log.trace("Extracted {} thumbnail entries.", thumbnails.size());
        return thumbnails;
    }

    private String extractDuration(String isoDuration) {
        // Simple pass-through for now; can be enhanced to readable format if needed
        log.trace("Extracting duration from ISO string: {}", isoDuration);
        return isoDuration;
    }

    /**
     * Mask API key value in URLs for safe logging.
     */
    private String sanitizeUrl(String url) {
        if (url == null) {
            return null;
        }
        // Replace key parameter value with masked value
        return url.replaceAll("(?i)([?&]key=)[^&]+", "$1****");
    }
}
