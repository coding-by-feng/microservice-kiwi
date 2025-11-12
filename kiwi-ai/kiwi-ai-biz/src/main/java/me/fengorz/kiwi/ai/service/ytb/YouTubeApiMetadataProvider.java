package me.fengorz.kiwi.ai.service.ytb;

import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.ai.api.vo.ytb.ChannelVideoResponse;
import me.fengorz.kiwi.ai.api.vo.ytb.VideoDetailsResponse;
import me.fengorz.kiwi.common.ytb.metadata.YouTubeMetadataProvider;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Metadata provider backed by the YouTube Data API v3.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "youtube.api.enabled", havingValue = "true", matchIfMissing = true)
public class YouTubeApiMetadataProvider implements YouTubeMetadataProvider {

    private final YouTubeApiService youTubeApiService;

    public YouTubeApiMetadataProvider(YouTubeApiService youTubeApiService) {
        this.youTubeApiService = youTubeApiService;
    }

    @Override
    public String getVideoTitle(String videoUrlOrId) {
        VideoDetailsResponse details = youTubeApiService.getVideoDetails(videoUrlOrId);
        return details != null ? details.getTitle() : null;
    }

    @Override
    public String getChannelName(String channelUrlOrId) {
        ChannelVideoResponse details = youTubeApiService.getChannelDetails(channelUrlOrId);
        return details != null ? details.getTitle() : null;
    }

    @Override
    public List<String> listChannelVideoLinks(String channelUrlOrId) {
        List<ChannelVideoResponse> videos = youTubeApiService.getChannelVideos(channelUrlOrId);
        return videos.stream()
                .map(video -> "https://www.youtube.com/watch?v=" + video.getVideoId())
                .collect(Collectors.toList());
    }

    @Override
    public LocalDateTime getVideoPublishedAt(String videoUrlOrId) {
        VideoDetailsResponse details = youTubeApiService.getVideoDetails(videoUrlOrId);
        if (details == null || StringUtils.isBlank(details.getPublishedAt())) {
            return null;
        }
        return parsePublishedAt(details.getPublishedAt());
    }

    private LocalDateTime parsePublishedAt(String publishedAt) {
        if (publishedAt == null || publishedAt.isEmpty()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(publishedAt, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toLocalDateTime();
        } catch (Exception e) {
            log.debug("Failed to parse publishedAt '{}' with ISO_OFFSET_DATE_TIME: {}", publishedAt, e.getMessage());
        }
        try {
            return OffsetDateTime.parse(publishedAt).toLocalDateTime();
        } catch (Exception e) {
            log.warn("Unable to parse publishedAt '{}' for video metadata: {}", publishedAt, e.getMessage());
            return null;
        }
    }
}
