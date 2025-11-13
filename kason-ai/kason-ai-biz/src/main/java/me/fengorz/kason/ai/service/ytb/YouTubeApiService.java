package me.fengorz.kason.ai.service.ytb;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kason.ai.api.vo.ytb.*;
import me.fengorz.kason.ai.config.YouTubeApiProperties;
import me.fengorz.kason.common.sdk.exception.ServiceException;
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
     * Ensure API key is present and not an unresolved placeholder.
     */
    private String getApiKeyOrThrow() {
        String key = properties.getKey();
        if (key == null || key.trim().isEmpty() || key.contains("{") || key.contains("}")) {
            throw new ServiceException("YouTube API key is not configured. Set env YTB_API_KEY or property 'youtube.api.key'.");
        }
        return key;
    }

    /**
     * Get video details by video URL or ID
     */
    public VideoDetailsResponse getVideoDetails(String videoUrlOrId) {
        log.info("Getting video details for: {}", videoUrlOrId);
        
        String videoId = extractVideoId(videoUrlOrId);
        if (videoId == null) {
            throw new ServiceException("Invalid video URL or ID: " + videoUrlOrId);
        }

        String url = UriComponentsBuilder.fromHttpUrl(properties.getBaseUrl() + "/videos")
                .queryParam("part", "snippet,contentDetails,statistics")
                .queryParam("id", videoId)
                .queryParam("key", getApiKeyOrThrow())
                .build()
                .toUriString();

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode items = root.get("items");
                
                if (items.isArray() && items.size() > 0) {
                    JsonNode video = items.get(0);
                    JsonNode snippet = video.get("snippet");
                    
                    return VideoDetailsResponse.builder()
                            .videoId(videoId)
                            .title(snippet.get("title").asText())
                            .description(snippet.get("description").asText())
                            .channelId(snippet.get("channelId").asText())
                            .channelTitle(snippet.get("channelTitle").asText())
                            .publishedAt(snippet.get("publishedAt").asText())
                            .thumbnails(extractThumbnails(snippet.get("thumbnails")))
                            .duration(extractDuration(video.get("contentDetails").get("duration").asText()))
                            .viewCount(video.get("statistics").get("viewCount").asLong())
                            .build();
                } else {
                    throw new ServiceException("Video not found: " + videoId);
                }
            } else {
                throw new ServiceException("YouTube API error: " + response.getStatusCode());
            }
        } catch (IOException e) {
            log.error("Error parsing YouTube API response", e);
            throw new ServiceException("Failed to parse video details", e);
        }
    }

    /**
     * Get channel details by channel URL, ID, or handle
     */
    public ChannelDetailsResponse getChannelDetails(String channelUrlOrId) {
        log.info("Getting channel details for: {}", channelUrlOrId);
        
        String channelId = extractChannelId(channelUrlOrId);
        
        // If we couldn't extract a channel ID, try to search by handle or username
        if (channelId == null) {
            channelId = findChannelByHandle(channelUrlOrId);
        }
        
        if (channelId == null) {
            throw new ServiceException("Invalid channel URL or ID: " + channelUrlOrId);
        }

        String url = UriComponentsBuilder.fromHttpUrl(properties.getBaseUrl() + "/channels")
                .queryParam("part", "snippet,contentDetails,statistics")
                .queryParam("id", channelId)
                .queryParam("key", getApiKeyOrThrow())
                .build()
                .toUriString();

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode items = root.get("items");
                
                if (items.isArray() && items.size() > 0) {
                    JsonNode channel = items.get(0);
                    JsonNode snippet = channel.get("snippet");
                    JsonNode statistics = channel.get("statistics");
                    
                    return ChannelDetailsResponse.builder()
                            .channelId(channelId)
                            .title(snippet.get("title").asText())
                            .description(snippet.get("description").asText())
                            .customUrl(snippet.has("customUrl") ? snippet.get("customUrl").asText() : null)
                            .publishedAt(snippet.get("publishedAt").asText())
                            .thumbnails(extractThumbnails(snippet.get("thumbnails")))
                            .subscriberCount(statistics.has("subscriberCount") ? statistics.get("subscriberCount").asLong() : 0)
                            .videoCount(statistics.has("videoCount") ? statistics.get("videoCount").asLong() : 0)
                            .viewCount(statistics.has("viewCount") ? statistics.get("viewCount").asLong() : 0)
                            .uploadsPlaylistId(channel.get("contentDetails").get("relatedPlaylists").get("uploads").asText())
                            .build();
                } else {
                    throw new ServiceException("Channel not found: " + channelId);
                }
            } else {
                throw new ServiceException("YouTube API error: " + response.getStatusCode());
            }
        } catch (IOException e) {
            log.error("Error parsing YouTube API response", e);
            throw new ServiceException("Failed to parse channel details", e);
        }
    }

    /**
     * Get all videos from a channel with pagination
     */
    public List<ChannelVideoResponse> getChannelVideos(String channelUrlOrId) {
        log.info("Getting all videos for channel: {}", channelUrlOrId);
        
        ChannelDetailsResponse channelDetails = getChannelDetails(channelUrlOrId);
        String uploadsPlaylistId = channelDetails.getUploadsPlaylistId();
        
        List<ChannelVideoResponse> allVideos = new ArrayList<>();
        String nextPageToken = null;
        int totalFetched = 0;
        
        do {
            PlaylistItemsResponse response = getPlaylistItems(uploadsPlaylistId, nextPageToken);
            allVideos.addAll(response.getItems());
            nextPageToken = response.getNextPageToken();
            totalFetched += response.getItems().size();
            
            log.info("Fetched {} videos, total: {}", response.getItems().size(), totalFetched);
            
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
        log.info("Getting captions for video: {}", videoUrlOrId);
        
        String videoId = extractVideoId(videoUrlOrId);
        if (videoId == null) {
            throw new ServiceException("Invalid video URL or ID: " + videoUrlOrId);
        }

        String url = UriComponentsBuilder.fromHttpUrl(properties.getBaseUrl() + "/captions")
                .queryParam("part", "snippet")
                .queryParam("videoId", videoId)
                .queryParam("key", getApiKeyOrThrow())
                .build()
                .toUriString();

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode items = root.get("items");
                
                List<CaptionResponse> captions = new ArrayList<>();
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
                
                return captions;
            } else {
                throw new ServiceException("YouTube API error: " + response.getStatusCode());
            }
        } catch (IOException e) {
            log.error("Error parsing YouTube API response", e);
            throw new ServiceException("Failed to parse captions", e);
        }
    }

    /**
     * Download caption content (requires OAuth, limited functionality with API key only)
     */
    public String downloadCaption(String captionId) {
        log.warn("Caption download requires OAuth authentication. API key only provides limited access.");
        
        String url = UriComponentsBuilder.fromHttpUrl(properties.getBaseUrl() + "/captions/" + captionId)
                .queryParam("key", getApiKeyOrThrow())
                .queryParam("fmt", "srt")
                .build()
                .toUriString();

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                return response.getBody();
            } else {
                throw new ServiceException("Caption download failed. OAuth required for caption content access.");
            }
        } catch (Exception e) {
            log.error("Error downloading caption", e);
            throw new ServiceException("Caption download requires OAuth authentication", e);
        }
    }

    // Private helper methods

    private PlaylistItemsResponse getPlaylistItems(String playlistId, String pageToken) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(properties.getBaseUrl() + "/playlistItems")
                .queryParam("part", "snippet,contentDetails")
                .queryParam("playlistId", playlistId)
                .queryParam("maxResults", properties.getMaxResultsPerPage())
                .queryParam("key", getApiKeyOrThrow());

        if (pageToken != null) {
            builder.queryParam("pageToken", pageToken);
        }
        
        String url = builder.build().toUriString();

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode root = objectMapper.readTree(response.getBody());
                
                List<ChannelVideoResponse> videos = new ArrayList<>();
                JsonNode items = root.get("items");
                
                for (JsonNode item : items) {
                    JsonNode snippet = item.get("snippet");
                    JsonNode contentDetails = item.get("contentDetails");
                    
                    // Skip private/deleted videos
                    if (snippet.get("title").asText().equals("Private video") || 
                        snippet.get("title").asText().equals("Deleted video")) {
                        continue;
                    }
                    
                    ChannelVideoResponse video = ChannelVideoResponse.builder()
                            .videoId(contentDetails.get("videoId").asText())
                            .title(snippet.get("title").asText())
                            .description(snippet.get("description").asText())
                            .publishedAt(snippet.get("publishedAt").asText())
                            .thumbnails(extractThumbnails(snippet.get("thumbnails")))
                            .position(snippet.get("position").asLong())
                            .build();
                    
                    videos.add(video);
                }
                
                return PlaylistItemsResponse.builder()
                        .items(videos)
                        .nextPageToken(root.has("nextPageToken") ? root.get("nextPageToken").asText() : null)
                        .totalResults(root.get("pageInfo").get("totalResults").asLong())
                        .build();
                
            } else {
                throw new ServiceException("YouTube API error: " + response.getStatusCode());
            }
        } catch (IOException e) {
            log.error("Error parsing playlist items response", e);
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
            return null;
        }
        
        // Search for channel by handle/username
        String url = UriComponentsBuilder.fromHttpUrl(properties.getBaseUrl() + "/search")
                .queryParam("part", "snippet")
                .queryParam("type", "channel")
                .queryParam("q", identifier)
                .queryParam("maxResults", 1)
                .queryParam("key", getApiKeyOrThrow())
                .build()
                .toUriString();

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode items = root.get("items");
                
                if (items.isArray() && items.size() > 0) {
                    JsonNode first = items.get(0);
                    if (first.has("id") && first.get("id").has("channelId")) {
                        return first.get("id").get("channelId").asText();
                    }
                }
            }
        } catch (IOException e) {
            log.error("Error searching for channel", e);
        }
        
        return null;
    }

    private String extractVideoId(String videoUrlOrId) {
        if (videoUrlOrId.length() == 11 && !videoUrlOrId.contains("/")) {
            // Likely already a video ID
            return videoUrlOrId;
        }
        
        try {
            String decoded = URLDecoder.decode(videoUrlOrId, StandardCharsets.UTF_8.name());
            Matcher matcher = VIDEO_ID_PATTERN.matcher(decoded);
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (Exception e) {
            log.warn("Error decoding video URL: {}", videoUrlOrId);
        }
        
        return null;
    }

    private String extractChannelId(String channelUrlOrId) {
        // Direct channel ID
        if (channelUrlOrId.startsWith("UC") && channelUrlOrId.length() > 20 && !channelUrlOrId.contains("/")) {
            return channelUrlOrId;
        }
        
        try {
            String decoded = URLDecoder.decode(channelUrlOrId, StandardCharsets.UTF_8.name());

            Matcher channelMatcher = CHANNEL_ID_PATTERN.matcher(decoded);
            if (channelMatcher.find()) {
                return channelMatcher.group(1);
            }
        } catch (Exception e) {
            log.warn("Error decoding channel URL: {}", channelUrlOrId);
        }
        
        return null;
    }

    private Map<String, String> extractThumbnails(JsonNode thumbnailsNode) {
        Map<String, String> thumbnails = new HashMap<>();
        if (thumbnailsNode == null) {
            return thumbnails;
        }
        thumbnailsNode.fields().forEachRemaining(entry -> thumbnails.put(entry.getKey(), entry.getValue().get("url").asText()));
        return thumbnails;
    }

    private String extractDuration(String isoDuration) {
        // Simple pass-through for now; can be enhanced to readable format if needed
        return isoDuration;
    }
}

