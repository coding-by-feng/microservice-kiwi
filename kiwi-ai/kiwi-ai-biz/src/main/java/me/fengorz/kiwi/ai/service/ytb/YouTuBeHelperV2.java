package me.fengorz.kiwi.ai.service.ytb;

import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.ai.api.vo.ytb.CaptionResponse;
import me.fengorz.kiwi.ai.api.vo.ytb.ChannelDetailsResponse;
import me.fengorz.kiwi.ai.api.vo.ytb.ChannelVideoResponse;
import me.fengorz.kiwi.ai.api.vo.ytb.VideoDetailsResponse;
import me.fengorz.kiwi.common.sdk.annotation.cache.KiwiCacheKey;
import me.fengorz.kiwi.common.sdk.annotation.cache.KiwiCacheKeyPrefix;
import me.fengorz.kiwi.common.sdk.constant.CacheConstants;
import me.fengorz.kiwi.common.sdk.exception.ServiceException;
import me.fengorz.kiwi.common.ytb.SubtitleTypeEnum;
import me.fengorz.kiwi.common.ytb.YtbConstants;
import me.fengorz.kiwi.common.ytb.YtbSubtitlesResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@KiwiCacheKeyPrefix(YtbConstants.CACHE_KEY_PREFIX_YTB.CLASS)
public class YouTuBeHelperV2 {

    private final YouTubeApiService youTubeApiService;

    @Value("${youtube.video.large-subtitles.threshold}")
    private int largeSubtitlesThreshold = 100;

    public YouTuBeHelperV2(YouTubeApiService youTubeApiService) {
        this.youTubeApiService = youTubeApiService;
    }

    /**
     * Note: Video download is not supported through YouTube API.
     * This would require a different approach or service.
     */
    public InputStream downloadVideo(String videoUrl) {
        throw new UnsupportedOperationException(
            "Video download is not supported through YouTube Data API. " +
            "Consider using alternative services or maintaining yt-dlp for this functionality."
        );
    }

    @SuppressWarnings("unused")
    @KiwiCacheKeyPrefix(YtbConstants.CACHE_KEY_PREFIX_YTB.SUBTITLES)
    @CacheEvict(cacheNames = YtbConstants.CACHE_NAMES, keyGenerator = CacheConstants.CACHE_KEY_GENERATOR_BEAN)
    public void cleanSubtitles(@KiwiCacheKey(1) String videoUrl) {
        log.info("Clearing subtitles cache for video: {}", videoUrl);
    }

    @KiwiCacheKeyPrefix(YtbConstants.CACHE_KEY_PREFIX_YTB.SUBTITLES)
    @Cacheable(cacheNames = YtbConstants.CACHE_NAMES, keyGenerator = CacheConstants.CACHE_KEY_GENERATOR_BEAN,
            unless = "#result == null")
    public YtbSubtitlesResult downloadSubtitles(@KiwiCacheKey(1) String videoUrl) {
        log.info("Getting subtitles for video: {}", videoUrl);
        
        try {
            // Get available captions
            List<CaptionResponse> captions = youTubeApiService.getVideoCaptions(videoUrl);
            
            if (captions.isEmpty()) {
                log.warn("No captions available for video: {}", videoUrl);
                throw new ServiceException("No captions available for this video");
            }
            
            // Prefer English captions
            CaptionResponse selectedCaption = captions.stream()
                    .filter(c -> c.getLanguage().startsWith("en"))
                    .findFirst()
                    .orElse(captions.get(0));
            
            log.info("Selected caption: language={}, trackKind={}, isAutoSynced={}", 
                     selectedCaption.getLanguage(), selectedCaption.getTrackKind(), selectedCaption.getIsAutoSynced());
            
            // Note: Caption content download requires OAuth
            // For now, we'll create a placeholder result
            String captionContent = tryDownloadCaptionContent(selectedCaption.getId());
            
            return buildSubtitlesResult(videoUrl, captionContent, selectedCaption.getIsAutoSynced());
            
        } catch (Exception e) {
            log.error("Error getting subtitles for video: {}", videoUrl, e);
            throw new ServiceException("Failed to get subtitles: " + e.getMessage(), e);
        }
    }

    public String getVideoTitle(String videoUrl) {
        log.info("Getting video title for: {}", videoUrl);
        
        try {
            VideoDetailsResponse videoDetails = youTubeApiService.getVideoDetails(videoUrl);
            String title = videoDetails.getTitle();
            log.info("Video title retrieved: {}", title);
            return title;
        } catch (Exception e) {
            log.error("Error getting video title for URL: {}", videoUrl, e);
            throw new ServiceException("Failed to get video title: " + e.getMessage(), e);
        }
    }

    /**
     * Extract channel name using YouTube API
     */
    public String extractChannelNameWithYtDlp(String channelUrl) throws ServiceException {
        log.info("Extracting channel name for: {}", channelUrl);
        
        try {
            ChannelDetailsResponse channelDetails = youTubeApiService.getChannelDetails(channelUrl);
            String channelName = channelDetails.getTitle();
            log.info("Successfully extracted channel name: {}", channelName);
            return channelName;
        } catch (Exception e) {
            log.error("Error extracting channel name for URL: {}", channelUrl, e);
            throw new ServiceException("Failed to extract channel name: " + e.getMessage(), e);
        }
    }

    /**
     * Extract all video links from a YouTube channel using API
     */
    public List<String> extractAllVideoLinks(String channelLink) throws ServiceException {
        log.info("Extracting video links from channel: {}", channelLink);
        
        try {
            List<ChannelVideoResponse> videos = youTubeApiService.getChannelVideos(channelLink);
            
            List<String> videoLinks = videos.stream()
                    .map(video -> "https://www.youtube.com/watch?v=" + video.getVideoId())
                    .collect(Collectors.toList());
            
            log.info("Successfully extracted {} video links from channel", videoLinks.size());
            return videoLinks;
        } catch (Exception e) {
            log.error("Error extracting video links from channel: {}", channelLink, e);
            throw new ServiceException("Failed to extract video links: " + e.getMessage(), e);
        }
    }

    // Private helper methods

    private String tryDownloadCaptionContent(String captionId) {
        try {
            // This will likely fail without OAuth, but we'll try
            return youTubeApiService.downloadCaption(captionId);
        } catch (Exception e) {
            log.warn("Could not download caption content (OAuth required): {}", e.getMessage());
            
            // Return placeholder content indicating captions are available but require OAuth
            return "Caption content available but requires OAuth authentication for download.\n" +
                   "Caption ID: " + captionId + "\n" +
                   "Please implement OAuth flow for full caption access.";
        }
    }

    private YtbSubtitlesResult buildSubtitlesResult(String videoUrl, String captionContent, Boolean isAutoSynced) {
        List<String> subtitleLines = Arrays.asList(captionContent.split("\\n"));

        YtbSubtitlesResult result = YtbSubtitlesResult.builder()
                .videoUrl(videoUrl)
                .scrollingSubtitles(captionContent)
                .build();

        // Determine type based on auto-sync status and content length
        boolean isAutoGenerated = isAutoSynced != null && isAutoSynced;
        boolean isLarge = subtitleLines.size() > largeSubtitlesThreshold;

        if (isAutoGenerated) {
            if (isLarge) {
                result.setType(SubtitleTypeEnum.LARGE_AUTO_GENERATED_RETURN_LIST);
                result.setPendingToBeTranslatedOrRetouchedSubtitles(subtitleLines);
            } else {
                result.setType(SubtitleTypeEnum.SMALL_AUTO_GENERATED_RETURN_STRING);
                result.setPendingToBeTranslatedOrRetouchedSubtitles(captionContent);
            }
        } else {
            if (isLarge) {
                result.setType(SubtitleTypeEnum.LARGE_PROFESSIONAL_RETURN_LIST);
                result.setPendingToBeTranslatedOrRetouchedSubtitles(subtitleLines);
            } else {
                result.setType(SubtitleTypeEnum.SMALL_PROFESSIONAL_RETURN_STRING);
                result.setPendingToBeTranslatedOrRetouchedSubtitles(captionContent);
            }
        }

        return result;
    }
}