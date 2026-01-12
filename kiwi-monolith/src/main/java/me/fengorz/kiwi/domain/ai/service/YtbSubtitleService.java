/*
 * Copyright [2019~2025] [codingByFeng]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.fengorz.kiwi.domain.ai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.constant.GlobalConstants;
import me.fengorz.kiwi.common.enumeration.AiPromptModeEnum;
import me.fengorz.kiwi.common.enumeration.LanguageEnum;
import me.fengorz.kiwi.domain.ai.entity.YtbChannelVideo;
import me.fengorz.kiwi.domain.ai.entity.YtbVideoSubtitles;
import me.fengorz.kiwi.domain.ai.ytb.SubtitleTypeEnum;
import me.fengorz.kiwi.domain.ai.ytb.YouTubeClient;
import me.fengorz.kiwi.domain.ai.ytb.YtbSubtitlesResult;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * YouTube Subtitle Service - handles subtitle operations
 *
 * @author codingByFeng
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class YtbSubtitleService {

    private static final String CACHE_NAME = "ytbSubtitles";

    private final YouTubeClient youTubeClient;
    private final AiChatService aiChatService;
    private final YtbChannelVideoService ytbChannelVideoService;
    private final YtbVideoSubtitlesService ytbVideoSubtitlesService;
    private final YtbVideoSubtitlesTranslationService ytbVideoSubtitlesTranslationService;

    private String decode(String url) {
        try {
            return URLDecoder.decode(url, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return url;
        }
    }

    private String extractVideoId(String rawUrl) {
        if (rawUrl == null || rawUrl.isEmpty())
            return rawUrl;
        String url = decode(rawUrl).trim();
        try {
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://" + url;
            }
            URI uri = URI.create(url);
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase();
            String path = uri.getPath() == null ? "" : uri.getPath();
            String query = uri.getQuery();

            if (host.contains("youtu.be")) {
                String p = path.startsWith("/") ? path.substring(1) : path;
                return trimIdTail(p);
            }

            if (host.contains("youtube.com")) {
                if (path.startsWith("/watch") && query != null) {
                    for (String kv : query.split("&")) {
                        String[] arr = kv.split("=", 2);
                        if (arr.length == 2 && "v".equals(arr[0])) {
                            return trimIdTail(arr[1]);
                        }
                    }
                }
                if (path.startsWith("/shorts/")) {
                    return trimIdTail(path.substring("/shorts/".length()));
                }
                if (path.startsWith("/embed/")) {
                    return trimIdTail(path.substring("/embed/".length()));
                }
            }
        } catch (Exception ignore) {
        }
        return url;
    }

    private String trimIdTail(String id) {
        if (id == null)
            return null;
        int q = id.indexOf('?');
        if (q >= 0)
            id = id.substring(0, q);
        int amp = id.indexOf('&');
        if (amp >= 0)
            id = id.substring(0, amp);
        int slash = id.indexOf('/');
        if (slash >= 0)
            id = id.substring(0, slash);
        return id;
    }

    @Cacheable(cacheNames = CACHE_NAME, key = "'scrolling:' + #videoUrl", unless = "#result == null")
    public YtbSubtitlesResult getScrollingSubtitles(String videoUrl) {
        log.info("Fetching scrolling subtitles for: {}", videoUrl);
        String decodedUrl = decode(videoUrl);
        return youTubeClient.downloadSubtitles(decodedUrl);
    }

    @Cacheable(cacheNames = CACHE_NAME, key = "'title:' + #videoUrl", unless = "#result == null")
    public String getVideoTitle(String videoUrl) {
        log.info("Fetching video title for: {}", videoUrl);
        String decodedUrl = decode(videoUrl);
        return youTubeClient.getVideoTitle(decodedUrl);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Cacheable(cacheNames = CACHE_NAME, key = "'translated:' + #videoUrl + ':' + #language", unless = "#result == null")
    public String getTranslatedSubtitles(String videoUrl, String language) {
        long startTime = System.currentTimeMillis();
        log.info("[SUBTITLE-TRANSLATE] Starting translation - URL: {}, language: {}", videoUrl, language);
        String decodedUrl = decode(videoUrl);

        YtbSubtitlesResult subtitlesResult = getScrollingSubtitles(videoUrl);
        if (subtitlesResult == null) {
            log.warn("[SUBTITLE-TRANSLATE] No subtitles found for URL: {}", videoUrl);
            return null;
        }

        boolean needsTranslation = language != null && !"null".equals(language)
                && !LanguageEnum.EN.getCode().equals(language);

        LanguageEnum lang = convertLanguage(language);
        log.info("[SUBTITLE-TRANSLATE] Parsed language: {} (needsTranslation: {})", lang, needsTranslation);

        SubtitleTypeEnum type = subtitlesResult.getType();
        Object pending = subtitlesResult.getPendingToBeTranslatedOrRetouchedSubtitles();
        log.info("[SUBTITLE-TRANSLATE] Subtitle type: {}, content size: {}",
                type, pending instanceof List ? ((List<?>) pending).size() : (pending != null ? ((String) pending).length() : 0));

        String result;
        switch (type) {
            case SMALL_AUTO_GENERATED_RETURN_STRING: {
                String content = (String) pending;
                AiPromptModeEnum mode = needsTranslation ? AiPromptModeEnum.SUBTITLE_RETOUCH_TRANSLATOR : AiPromptModeEnum.SUBTITLE_RETOUCH;
                log.info("[SUBTITLE-TRANSLATE] Processing SMALL_AUTO_GENERATED with mode: {}", mode);
                result = aiChatService.callForYtbAndCache(decodedUrl, content, mode, lang);
                break;
            }
            case LARGE_AUTO_GENERATED_RETURN_LIST: {
                List<String> contentList = (List) pending;
                AiPromptModeEnum mode = needsTranslation ? AiPromptModeEnum.SUBTITLE_RETOUCH_TRANSLATOR : AiPromptModeEnum.SUBTITLE_RETOUCH;
                log.info("[SUBTITLE-TRANSLATE] Processing LARGE_AUTO_GENERATED ({} chunks) with mode: {}", contentList.size(), mode);
                result = aiChatService.batchCallForYtbAndCache(decodedUrl, contentList, mode, lang);
                break;
            }
            case SMALL_PROFESSIONAL_RETURN_STRING: {
                String content = (String) pending;
                if (needsTranslation) {
                    log.info("[SUBTITLE-TRANSLATE] Processing SMALL_PROFESSIONAL with mode: SUBTITLE_TRANSLATOR");
                    result = aiChatService.callForYtbAndCache(decodedUrl, content, AiPromptModeEnum.SUBTITLE_TRANSLATOR, lang);
                } else {
                    log.info("[SUBTITLE-TRANSLATE] SMALL_PROFESSIONAL without translation - returning empty");
                    result = GlobalConstants.EMPTY;
                }
                break;
            }
            case LARGE_PROFESSIONAL_RETURN_LIST: {
                List<String> contentList = (List) pending;
                if (needsTranslation) {
                    log.info("[SUBTITLE-TRANSLATE] Processing LARGE_PROFESSIONAL ({} chunks) with mode: SUBTITLE_TRANSLATOR", contentList.size());
                    result = aiChatService.batchCallForYtbAndCache(decodedUrl, contentList, AiPromptModeEnum.SUBTITLE_TRANSLATOR, lang);
                } else {
                    log.info("[SUBTITLE-TRANSLATE] LARGE_PROFESSIONAL without translation - returning empty");
                    result = GlobalConstants.EMPTY;
                }
                break;
            }
            default:
                log.warn("[SUBTITLE-TRANSLATE] Unknown subtitle type: {}", type);
                result = GlobalConstants.EMPTY;
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info("[SUBTITLE-TRANSLATE] Completed - Duration: {}ms, Result length: {}",
                duration, result != null ? result.length() : 0);
        return result;
    }

    private static final Pattern TIMESTAMP_PATTERN = Pattern.compile(
            "\\d{2}:\\d{2}:\\d{2}\\.\\d{3}\\s+-->\\s+\\d{2}:\\d{2}:\\d{2}\\.\\d{3}.*");

    /**
     * Get enhanced scrolling subtitles with punctuation added while preserving original timestamps.
     * This method:
     * 1. Gets the original scrolling subtitles (with timestamps)
     * 2. Extracts only text lines and sends them to AI for punctuation-only enhancement
     * 3. Recombines the punctuated text with original timestamps
     *
     * @param videoUrl the YouTube video URL
     * @return enhanced subtitles with timestamps preserved and text punctuated
     */
    @Cacheable(cacheNames = CACHE_NAME, key = "'enhanced:' + #videoUrl", unless = "#result == null")
    public String getEnhancedScrollingSubtitles(String videoUrl) {
        long startTime = System.currentTimeMillis();
        log.info("[SUBTITLE-ENHANCE] Starting enhancement - URL: {}", videoUrl);
        String decodedUrl = decode(videoUrl);

        YtbSubtitlesResult subtitlesResult = getScrollingSubtitles(videoUrl);
        if (subtitlesResult == null) {
            log.warn("[SUBTITLE-ENHANCE] No subtitles found for URL: {}", videoUrl);
            return null;
        }

        String scrollingSubtitles = subtitlesResult.getScrollingSubtitles();
        if (scrollingSubtitles == null || scrollingSubtitles.isEmpty()) {
            log.warn("[SUBTITLE-ENHANCE] Empty scrolling subtitles for URL: {}", videoUrl);
            return null;
        }

        // Split into lines
        String[] lines = scrollingSubtitles.split("\n");

        // Separate timestamps and text lines
        List<String> timestamps = new ArrayList<>();
        List<String> textLines = new ArrayList<>();
        List<Integer> textLineIndices = new ArrayList<>();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) {
                continue;
            }
            if (TIMESTAMP_PATTERN.matcher(line).matches()) {
                timestamps.add(line);
            } else if (!line.equals("WEBVTT") && !line.startsWith("Kind:") && !line.startsWith("Language:")) {
                textLines.add(line);
                textLineIndices.add(timestamps.size() - 1); // Associate with last timestamp
            }
        }

        log.info("[SUBTITLE-ENHANCE] Extracted {} timestamps, {} text lines", timestamps.size(), textLines.size());

        if (textLines.isEmpty()) {
            log.warn("[SUBTITLE-ENHANCE] No text lines extracted for URL: {}", videoUrl);
            return scrollingSubtitles;
        }

        // Send text lines to AI for punctuation only
        String textOnlyContent = String.join("\n", textLines);
        String punctuatedContent = aiChatService.callForYtbAndCache(
                decodedUrl,
                textOnlyContent,
                AiPromptModeEnum.SUBTITLE_PUNCTUATION_ONLY,
                LanguageEnum.EN
        );

        if (punctuatedContent == null || punctuatedContent.isEmpty()) {
            log.warn("[SUBTITLE-ENHANCE] AI returned empty result, returning original subtitles");
            return scrollingSubtitles;
        }

        // Parse punctuated lines
        String[] punctuatedLines = punctuatedContent.split("\n");

        // Recombine timestamps with punctuated text
        StringBuilder result = new StringBuilder();
        result.append("WEBVTT\n\n");

        int punctuatedIndex = 0;
        for (int i = 0; i < textLineIndices.size() && punctuatedIndex < punctuatedLines.length; i++) {
            int timestampIdx = textLineIndices.get(i);
            if (timestampIdx >= 0 && timestampIdx < timestamps.size()) {
                result.append(timestamps.get(timestampIdx)).append("\n");
            }
            // Use punctuated line, skip empty lines from AI output
            String punctuatedLine = punctuatedLines[punctuatedIndex].trim();
            while (punctuatedLine.isEmpty() && punctuatedIndex < punctuatedLines.length - 1) {
                punctuatedIndex++;
                punctuatedLine = punctuatedLines[punctuatedIndex].trim();
            }
            result.append(punctuatedLine).append("\n\n");
            punctuatedIndex++;
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info("[SUBTITLE-ENHANCE] Completed - Duration: {}ms, Result length: {}",
                duration, result.length());

        return result.toString();
    }

    /**
     * Clean all subtitle data for a video URL:
     * 1. Evict Spring Cache (ytbSubtitles)
     * 2. Clean AI response cache
     * 3. Delete subtitle translations from DB
     * 4. Delete subtitles from DB
     *
     * @param videoUrl the YouTube video URL
     * @param language the language code (optional)
     */
    @Transactional
    @CacheEvict(cacheNames = CACHE_NAME, allEntries = true)
    public void cleanSubtitlesCache(String videoUrl, String language) {
        log.info("Cleaning subtitles cache and DB records for: {}, language: {}", videoUrl, language);
        String decodedUrl = decode(videoUrl);
        LanguageEnum lang = convertLanguage(language);

        // 1. Clean AI response cache
        aiChatService.cleanBatchCallForYtbAndCache(decodedUrl, AiPromptModeEnum.SUBTITLE_RETOUCH_TRANSLATOR, lang);
        aiChatService.cleanBatchCallForYtbAndCache(decodedUrl, AiPromptModeEnum.SUBTITLE_RETOUCH, lang);
        aiChatService.cleanBatchCallForYtbAndCache(decodedUrl, AiPromptModeEnum.SUBTITLE_TRANSLATOR, lang);
        aiChatService.cleanCallForYtbAndCache(decodedUrl, AiPromptModeEnum.SUBTITLE_RETOUCH_TRANSLATOR, lang);
        aiChatService.cleanCallForYtbAndCache(decodedUrl, AiPromptModeEnum.SUBTITLE_TRANSLATOR, lang);
        aiChatService.cleanCallForYtbAndCache(decodedUrl, AiPromptModeEnum.SUBTITLE_RETOUCH, lang);
        aiChatService.cleanCallForYtbAndCache(decodedUrl, AiPromptModeEnum.SUBTITLE_PUNCTUATION_ONLY, lang);

        // 2. Find video by URL and delete related DB records
        Optional<YtbChannelVideo> videoOpt = ytbChannelVideoService.findByVideoLink(decodedUrl);
        if (videoOpt.isPresent()) {
            Long videoId = videoOpt.get().getId();
            log.info("Found video ID: {} for URL: {}, deleting subtitles and translations", videoId, decodedUrl);

            // 2.1 Find all subtitles for this video
            List<YtbVideoSubtitles> subtitlesList = ytbVideoSubtitlesService.findByVideoId(videoId);
            if (!subtitlesList.isEmpty()) {
                // 2.2 Get all subtitle IDs
                List<Long> subtitleIds = subtitlesList.stream()
                        .map(YtbVideoSubtitles::getId)
                        .collect(Collectors.toList());

                // 2.3 Delete all translations for these subtitles
                log.info("Deleting {} translations for {} subtitles", subtitleIds.size(), subtitleIds.size());
                ytbVideoSubtitlesTranslationService.deleteBySubtitlesIds(subtitleIds);

                // 2.4 Delete all subtitles for this video
                log.info("Deleting {} subtitles for video ID: {}", subtitlesList.size(), videoId);
                ytbVideoSubtitlesService.deleteByVideoId(videoId);
            } else {
                log.info("No subtitles found in DB for video ID: {}", videoId);
            }
        } else {
            log.info("No video found in DB for URL: {}, skipping DB cleanup", decodedUrl);
        }

        log.info("Completed cleaning subtitles for: {}", videoUrl);
    }

    private LanguageEnum convertLanguage(String language) {
        if (language == null || "null".equals(language)) {
            return LanguageEnum.EN;
        }
        try {
            return LanguageEnum.fromCode(language);
        } catch (Exception e) {
            return LanguageEnum.EN;
        }
    }
}
