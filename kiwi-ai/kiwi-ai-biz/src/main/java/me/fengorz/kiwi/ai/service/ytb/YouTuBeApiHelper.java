package me.fengorz.kiwi.ai.service.ytb;

import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.ai.api.vo.ytb.CaptionResponse;
import me.fengorz.kiwi.ai.api.vo.ytb.VideoDetailsResponse;
import me.fengorz.kiwi.common.sdk.annotation.cache.KiwiCacheKey;
import me.fengorz.kiwi.common.sdk.annotation.cache.KiwiCacheKeyPrefix;
import me.fengorz.kiwi.common.sdk.constant.CacheConstants;
import me.fengorz.kiwi.common.sdk.constant.GlobalConstants;
import me.fengorz.kiwi.common.sdk.exception.ServiceException;
import me.fengorz.kiwi.common.ytb.SubtitleTypeEnum;
import me.fengorz.kiwi.common.ytb.VttFileCleaner;
import me.fengorz.kiwi.common.ytb.YtbConstants;
import me.fengorz.kiwi.common.ytb.YtbSubtitlesResult;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * YouTube helper backed by YouTube Data API instead of yt-dlp.
 */
@Slf4j
@Service
@KiwiCacheKeyPrefix(YtbConstants.CACHE_KEY_PREFIX_YTB.CLASS)
public class YouTuBeApiHelper {

    private final YouTubeApiService youTubeApiService;

    @Value("${youtube.video.large-subtitles.threshold:200}")
    private int largeSubtitlesThreshold;

    @Value("${youtube.video.subtitles.langs:en,en-GB,en-US}")
    private String subtitlesLangs;

    public YouTuBeApiHelper(YouTubeApiService youTubeApiService) {
        this.youTubeApiService = youTubeApiService;
    }

    @SuppressWarnings("unused")
    @KiwiCacheKeyPrefix(YtbConstants.CACHE_KEY_PREFIX_YTB.SUBTITLES)
    @CacheEvict(cacheNames = YtbConstants.CACHE_NAMES, keyGenerator = CacheConstants.CACHE_KEY_GENERATOR_BEAN)
    public void cleanSubtitles(@KiwiCacheKey(1) String videoUrl) {
    }

    @KiwiCacheKeyPrefix(YtbConstants.CACHE_KEY_PREFIX_YTB.SUBTITLES)
    @Cacheable(cacheNames = YtbConstants.CACHE_NAMES, keyGenerator = CacheConstants.CACHE_KEY_GENERATOR_BEAN,
            unless = "#result == null")
    public YtbSubtitlesResult downloadSubtitles(@KiwiCacheKey(1) String videoUrl) {
        List<CaptionResponse> captions = youTubeApiService.getVideoCaptions(videoUrl);
        if (CollectionUtils.isEmpty(captions)) {
            throw new ServiceException("No captions available for video: " + videoUrl);
        }

        CaptionResponse selected = selectCaptionByLanguage(captions);
        if (selected == null) {
            throw new ServiceException("No matching captions found for video: " + videoUrl);
        }

        String captionContent = youTubeApiService.downloadCaption(selected.getId());
        if (StringUtils.isBlank(captionContent)) {
            throw new ServiceException("Empty caption content returned for video: " + videoUrl);
        }

        SubtitleParseResult parseResult = parseCaptionContent(captionContent);
        return buildResult(videoUrl, selected, parseResult);
    }

    public String getVideoTitle(String videoUrl) {
        VideoDetailsResponse details = youTubeApiService.getVideoDetails(videoUrl);
        return details.getTitle();
    }

    private CaptionResponse selectCaptionByLanguage(List<CaptionResponse> captions) {
        List<String> preferred = subtitlesLangs == null ? new ArrayList<>() : Arrays.asList(subtitlesLangs.split(","));
        for (String lang : preferred) {
            String trimmed = lang.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            for (CaptionResponse caption : captions) {
                if (trimmed.equalsIgnoreCase(caption.getLanguage())) {
                    return caption;
                }
            }
        }
        return captions.get(0);
    }

    private SubtitleParseResult parseCaptionContent(String captionContent) {
        List<String> subtitles = new ArrayList<>();
        String previousLine = null;
        String[] lines = captionContent.split("\\r?\\n");
        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty()) {
                continue;
            }
            if (line.matches("\\d+") || line.startsWith("WEBVTT")) {
                continue;
            }
            if (line.contains("-->") && line.length() <= 50) {
                String clean = cleanSubtitleLine(line);
                subtitles.add(clean);
                previousLine = clean;
                continue;
            }
            String cleanedLine = cleanSubtitleLine(line);
            if (StringUtils.isBlank(cleanedLine)) {
                continue;
            }
            if (StringUtils.equals(previousLine, cleanedLine)) {
                continue;
            }
            subtitles.add(cleanedLine);
            previousLine = cleanedLine;
        }
        return new SubtitleParseResult(subtitles);
    }

    private YtbSubtitlesResult buildResult(String videoUrl, CaptionResponse caption, SubtitleParseResult parseResult) {
        List<String> subtitles = parseResult.subtitles;
        boolean autoGenerated = Boolean.TRUE.equals(caption.getIsAutoSynced()) ||
                "asr".equalsIgnoreCase(StringUtils.defaultString(caption.getTrackKind()));
        String langCode = StringUtils.defaultIfBlank(caption.getLanguage(), "en");

        if (autoGenerated) {
            return buildAutoGeneratedResult(videoUrl, subtitles, langCode);
        }

        YtbSubtitlesResult result = YtbSubtitlesResult.builder()
                .videoUrl(videoUrl)
                .scrollingSubtitles(String.join(GlobalConstants.SYMBOL_LINE, subtitles))
                .langCode(langCode)
                .build();

        List<String> cleaned = VttFileCleaner.cleanTimestamp(subtitles);
        if (subtitles.size() > this.largeSubtitlesThreshold) {
            result.setType(SubtitleTypeEnum.LARGE_PROFESSIONAL_RETURN_LIST);
            result.setPendingToBeTranslatedOrRetouchedSubtitles(cleaned);
        } else {
            result.setType(SubtitleTypeEnum.SMALL_PROFESSIONAL_RETURN_STRING);
            result.setPendingToBeTranslatedOrRetouchedSubtitles(String.join(GlobalConstants.SYMBOL_LINE, cleaned));
        }
        return result;
    }

    private YtbSubtitlesResult buildAutoGeneratedResult(String videoUrl, List<String> subtitles, String langCode) {
        List<String> clearSubtitles = VttFileCleaner.cleanTimestamp(subtitles);
        YtbSubtitlesResult result = YtbSubtitlesResult.builder()
                .videoUrl(videoUrl)
                .scrollingSubtitles(String.join(GlobalConstants.SYMBOL_LINE, VttFileCleaner.cleanDuplicatedLines(subtitles)))
                .langCode(langCode)
                .build();
        if (subtitles.size() > this.largeSubtitlesThreshold) {
            result.setType(SubtitleTypeEnum.LARGE_AUTO_GENERATED_RETURN_LIST);
            result.setPendingToBeTranslatedOrRetouchedSubtitles(clearSubtitles);
        } else {
            result.setType(SubtitleTypeEnum.SMALL_AUTO_GENERATED_RETURN_STRING);
            result.setPendingToBeTranslatedOrRetouchedSubtitles(String.join(GlobalConstants.SYMBOL_LINE, clearSubtitles));
        }
        return result;
    }

    private String cleanSubtitleLine(String line) {
        String result = Objects.toString(line, "");
        result = result.replaceAll("&nbsp;", "")
                .replaceAll("\\s+align:start position:0%$", "");
        return result.replaceAll("\\s+", " ").trim();
    }

    private static class SubtitleParseResult {
        private final List<String> subtitles;

        private SubtitleParseResult(List<String> subtitles) {
            this.subtitles = subtitles;
        }
    }
}
