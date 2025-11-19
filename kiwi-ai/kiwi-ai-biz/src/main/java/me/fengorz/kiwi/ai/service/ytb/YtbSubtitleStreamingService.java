package me.fengorz.kiwi.ai.service.ytb;

import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.ai.service.AiChatService;
import me.fengorz.kiwi.ai.service.AiStreamingService;
import me.fengorz.kiwi.ai.util.AiConstants;
import me.fengorz.kiwi.ai.util.LanguageConvertor;
import me.fengorz.kiwi.common.sdk.annotation.cache.KiwiCacheKey;
import me.fengorz.kiwi.common.sdk.annotation.cache.KiwiCacheKeyPrefix;
import me.fengorz.kiwi.common.sdk.constant.CacheConstants;
import me.fengorz.kiwi.common.sdk.enumeration.AiPromptModeEnum;
import me.fengorz.kiwi.common.sdk.enumeration.LanguageEnum;
import me.fengorz.kiwi.common.sdk.web.WebTools;
import me.fengorz.kiwi.ai.service.ytb.YouTuBeApiHelper;
import me.fengorz.kiwi.common.ytb.YtbSubtitlesResult;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Slf4j
@Service
@KiwiCacheKeyPrefix(AiConstants.CACHE_KEY_PREFIX_GROK.CLASS)
public class YtbSubtitleStreamingService {

    private final YouTuBeApiHelper youTuBeApiHelper;
    private final AiChatService aiChatService;
    private final AiStreamingService aiStreamingService;

    public YtbSubtitleStreamingService(YouTuBeApiHelper youTuBeApiHelper,
                                       @Qualifier("grokAiService") AiChatService aiChatService,
                                       @Qualifier("grokStreamingService") AiStreamingService aiStreamingService,
                                       org.springframework.cache.CacheManager cacheManager) {
        this.youTuBeApiHelper = youTuBeApiHelper;
        this.aiChatService = aiChatService;
        this.aiStreamingService = aiStreamingService;
    }

    // ---------------------------------------
    // Helpers
    // ---------------------------------------

    private String decode(String url) {
        try {
            return WebTools.decode(url);
        } catch (Exception e) {
            try {
                return URLDecoder.decode(url, StandardCharsets.UTF_8.name());
            } catch (Exception ex) {
                return url;
            }
        }
    }

    /**
     * Extract normalized YouTube video ID from various URL formats.
     * Supported: youtu.be/{id}, youtube.com/watch?v={id}, youtube.com/shorts/{id}, youtube.com/embed/{id}
     */
    private String extractVideoId(String rawUrl) {
        if (rawUrl == null || rawUrl.isEmpty()) return rawUrl;
        String url = decode(rawUrl).trim();
        try {
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://" + url;
            }
            URI uri = URI.create(url);
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase();
            String path = uri.getPath() == null ? "" : uri.getPath();
            String query = uri.getQuery();

            // youtu.be/<id>
            if (host.contains("youtu.be")) {
                String p = path.startsWith("/") ? path.substring(1) : path;
                return trimIdTail(p);
            }

            // youtube.com/watch?v=<id>&...
            if (host.contains("youtube.com")) {
                if (path.startsWith("/watch") && query != null) {
                    for (String kv : query.split("&")) {
                        String[] arr = kv.split("=", 2);
                        if (arr.length == 2 && "v".equals(arr[0])) {
                            return trimIdTail(arr[1]);
                        }
                    }
                }
                // youtube.com/shorts/<id>
                if (path.startsWith("/shorts/")) {
                    return trimIdTail(path.substring("/shorts/".length()));
                }
                // youtube.com/embed/<id>
                if (path.startsWith("/embed/")) {
                    return trimIdTail(path.substring("/embed/".length()));
                }
            }
        } catch (Exception ignore) {
            // fallback below
        }

        // Fallback: try to detect 11-char ID inside the string
        String candidate = url;
        int idx = candidate.indexOf("v=");
        if (idx >= 0) {
            String sub = candidate.substring(idx + 2);
            int amp = sub.indexOf('&');
            if (amp > 0) sub = sub.substring(0, amp);
            return trimIdTail(sub);
        }
        return url; // as-is fallback
    }

    private String trimIdTail(String id) {
        if (id == null) return null;
        int q = id.indexOf('?');
        if (q >= 0) id = id.substring(0, q);
        int amp = id.indexOf('&');
        if (amp >= 0) id = id.substring(0, amp);
        int slash = id.indexOf('/');
        if (slash >= 0) id = id.substring(0, slash);
        return id;
    }

    // ---------------------------------------
    // Public API (wrappers keep signatures stable)
    // ---------------------------------------

    /**
     * Get scrolling subtitles (cached by normalized videoId)
     */
    public YtbSubtitlesResult getScrollingSubtitles(String videoUrl) {
        String videoId = extractVideoId(videoUrl);
        String decodedUrl = decode(videoUrl);
        return getScrollingSubtitlesInternal(videoId, decodedUrl);
    }

    /**
     * Get video title (cached by normalized videoId)
     */
    public String getVideoTitle(String videoUrl) {
        String videoId = extractVideoId(videoUrl);
        String decodedUrl = decode(videoUrl);
        return getVideoTitleInternal(videoId, decodedUrl);
    }

    /**
     * Clean all caches scoped to this video (and optional language)
     */
    public void cleanAllCaches(String videoUrl, String language) {
        String videoId = extractVideoId(videoUrl);
        LanguageEnum lang = (language != null && !"null".equals(language)) ?
                LanguageConvertor.convertLanguageToEnum(language) : LanguageEnum.NONE;

        // Clean caches keyed by videoId
        cleanScrollingSubtitlesCacheById(videoId);
        cleanVideoTitleCacheById(videoId);
        if (language != null && !"null".equals(language)) {
            cleanStreamingSubtitleTranslationCacheById(videoId, language);
        }

        String decodedUrl = decode(videoUrl);
        // Clean AI translation caches (these use decoded URL as their own keys)
        aiChatService.cleanBatchCallForYtbAndCache(decodedUrl, AiPromptModeEnum.SUBTITLE_RETOUCH_TRANSLATOR, lang);
        aiChatService.cleanBatchCallForYtbAndCache(decodedUrl, AiPromptModeEnum.SUBTITLE_RETOUCH, lang);
        aiChatService.cleanBatchCallForYtbAndCache(decodedUrl, AiPromptModeEnum.SUBTITLE_TRANSLATOR, lang);
        aiChatService.cleanCallForYtbAndCache(decodedUrl, AiPromptModeEnum.SUBTITLE_RETOUCH_TRANSLATOR, lang);
        aiChatService.cleanCallForYtbAndCache(decodedUrl, AiPromptModeEnum.SUBTITLE_TRANSLATOR, lang);
        aiChatService.cleanCallForYtbAndCache(decodedUrl, AiPromptModeEnum.SUBTITLE_RETOUCH, lang);

        log.info("All caches cleaned for videoId: {}, language: {}", videoId, language);
    }

    // ---------------------------------------
    // Streaming API
    // ---------------------------------------

    public void streamSubtitleTranslation(String videoUrl, String language,
                                          Consumer<String> onChunk,
                                          Consumer<Exception> onError,
                                          Runnable onComplete) {
        CompletableFuture.runAsync(() -> {
            try {
                String videoId = extractVideoId(videoUrl);
                String decodedUrl = decode(videoUrl);
                // Normalize language to enum code for caching keys
                LanguageEnum targetLanguageEnum = (language != null && !"null".equals(language))
                        ? LanguageConvertor.convertLanguageToEnum(language) : null;
                String cacheLang = targetLanguageEnum != null ? targetLanguageEnum.getCode() : language;

                log.info("Starting subtitle translation streaming for videoId: {}, language: {}", videoId, cacheLang);

                boolean needsTranslation = cacheLang != null &&
                        !"null".equals(cacheLang) &&
                        !LanguageEnum.EN.getCode().equals(cacheLang);

                if (!needsTranslation) {
                    // No translation: return original subtitles
                    YtbSubtitlesResult subtitlesResult = getScrollingSubtitlesInternal(videoId, decodedUrl);
                    if (subtitlesResult == null) {
                        onError.accept(new RuntimeException("No subtitles available for this video"));
                        return;
                    }
                    onChunk.accept(subtitlesResult.getScrollingSubtitles());
                    onComplete.run();
                    return;
                }

                // Try cache first (videoId + language)
                String cachedTranslation = getCachedStreamingSubtitleTranslationById(videoId, cacheLang);
                if (cachedTranslation != null) {
                    log.info("Found cached streaming translation for videoId: {}, language: {}, length: {}",
                            videoId, cacheLang, cachedTranslation.length());
                    streamCachedContent(cachedTranslation, onChunk, onComplete);
                    return;
                }

                log.info("No cached translation found, performing live AI translation streaming");

                // Ensure original subtitles available (cached by id)
                YtbSubtitlesResult subtitlesResult = getScrollingSubtitlesInternal(videoId, decodedUrl);
                log.debug("Subtitle result: {}", subtitlesResult);

                if (subtitlesResult == null) {
                    onError.accept(new RuntimeException("No subtitles available for this video"));
                    return;
                }

                // Perform streaming translation and cache final content by (videoId, language)
                streamTranslationWithAiAndCache(videoId, cacheLang, subtitlesResult, targetLanguageEnum, onChunk, onError, onComplete);

            } catch (Exception e) {
                log.error("Error in subtitle translation streaming: {}", e.getMessage(), e);
                onError.accept(e);
            }
        });
    }

    // ---------------------------------------
    // Internal cached methods (cache keys use videoId)
    // ---------------------------------------

    @KiwiCacheKeyPrefix(AiConstants.CACHE_KEY_PREFIX_GROK.SUBTITLE_SCROLLING)
    @Cacheable(cacheNames = AiConstants.CACHE_NAMES, keyGenerator = CacheConstants.CACHE_KEY_GENERATOR_BEAN,
            unless = "#result == null")
    public YtbSubtitlesResult getScrollingSubtitlesInternal(@KiwiCacheKey(1) String videoId, String decodedUrl) {
        log.info("Fetching scrolling subtitles for videoId: {}", videoId);
        return youTuBeApiHelper.downloadSubtitles(decodedUrl);
    }

    @KiwiCacheKeyPrefix(AiConstants.CACHE_KEY_PREFIX_GROK.VIDEO_TITLE)
    @Cacheable(cacheNames = AiConstants.CACHE_NAMES, keyGenerator = CacheConstants.CACHE_KEY_GENERATOR_BEAN,
            unless = "#result == null")
    public String getVideoTitleInternal(@KiwiCacheKey(1) String videoId, String decodedUrl) {
        log.info("Fetching video title for videoId: {}", videoId);
        return youTuBeApiHelper.getVideoTitle(decodedUrl);
    }

    @KiwiCacheKeyPrefix(AiConstants.CACHE_KEY_PREFIX_GROK.SUBTITLE_STREAMING)
    @Cacheable(cacheNames = AiConstants.CACHE_NAMES, keyGenerator = CacheConstants.CACHE_KEY_GENERATOR_BEAN,
            unless = "#result == null")
    public String getCachedStreamingSubtitleTranslationById(@KiwiCacheKey(1) String videoId,
                                                            @KiwiCacheKey(2) String language) {
        return null; // Cache miss -> stream and CachePut later
    }

    @KiwiCacheKeyPrefix(AiConstants.CACHE_KEY_PREFIX_GROK.SUBTITLE_STREAMING)
    @CachePut(cacheNames = AiConstants.CACHE_NAMES, keyGenerator = CacheConstants.CACHE_KEY_GENERATOR_BEAN,
            unless = "#result == null")
    public String cacheStreamingSubtitleTranslation(@KiwiCacheKey(1) String videoId,
                                                    @KiwiCacheKey(2) String language,
                                                    String translatedContent) {
        log.info("Caching streaming subtitle translation for videoId: {}, language: {}, length: {}",
                videoId, language, translatedContent != null ? translatedContent.length() : 0);
        return translatedContent;
    }

    // ---------------------------------------
    // Streaming helpers
    // ---------------------------------------

    private void streamCachedContent(String cachedContent, Consumer<String> onChunk, Runnable onComplete) {
        try {
            String[] lines = cachedContent.split("\n");
            StringBuilder chunkBuilder = new StringBuilder();
            int linesPerChunk = Math.max(1, lines.length / 10);

            for (int i = 0; i < lines.length; i++) {
                chunkBuilder.append(lines[i]).append("\n");
                if ((i + 1) % linesPerChunk == 0 || i == lines.length - 1) {
                    String chunk = chunkBuilder.toString();
                    onChunk.accept(chunk);
                    chunkBuilder.setLength(0);
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
            onComplete.run();
            log.info("Successfully streamed cached content");
        } catch (Exception e) {
            log.error("Error streaming cached content: {}", e.getMessage(), e);
            onChunk.accept(cachedContent);
            onComplete.run();
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void streamTranslationWithAiAndCache(String videoId,
                                                 String language,
                                                 YtbSubtitlesResult subtitlesResult,
                                                 LanguageEnum targetLanguage,
                                                 Consumer<String> onChunk,
                                                 Consumer<Exception> onError,
                                                 Runnable onComplete) {
        try {
            AiPromptModeEnum promptMode = AiPromptModeEnum.SUBTITLE_RETOUCH_TRANSLATOR;
            StringBuilder fullContentBuilder = new StringBuilder();

            Consumer<String> cachingOnChunk = chunk -> {
                fullContentBuilder.append(chunk);
                onChunk.accept(chunk);
            };

            Runnable cachingOnComplete = () -> {
                String fullTranslatedContent = fullContentBuilder.toString();
                if (fullTranslatedContent.length() > 0) {
                    cacheStreamingSubtitleTranslation(videoId, language, fullTranslatedContent);
                }
                onComplete.run();
            };

            switch (subtitlesResult.getType()) {
                case SMALL_AUTO_GENERATED_RETURN_STRING:
                case SMALL_PROFESSIONAL_RETURN_STRING: {
                    String content = (String) subtitlesResult.getPendingToBeTranslatedOrRetouchedSubtitles();
                    streamSingleContent(content, promptMode, targetLanguage, cachingOnChunk, onError, cachingOnComplete);
                    break;
                }
                case LARGE_AUTO_GENERATED_RETURN_LIST:
                case LARGE_PROFESSIONAL_RETURN_LIST: {
                    List<String> contentList = (List) subtitlesResult.getPendingToBeTranslatedOrRetouchedSubtitles();
                    streamBatchContent(contentList, promptMode, targetLanguage, cachingOnChunk, onError, cachingOnComplete);
                    break;
                }
                default:
                    onError.accept(new RuntimeException("Unsupported subtitle type: " + subtitlesResult.getType()));
            }
        } catch (Exception e) {
            log.error("Error in AI streaming translation with caching: {}", e.getMessage(), e);
            onError.accept(e);
        }
    }

    private void streamSingleContent(String content,
                                     AiPromptModeEnum promptMode,
                                     LanguageEnum targetLanguage,
                                     Consumer<String> onChunk,
                                     Consumer<Exception> onError,
                                     Runnable onComplete) {
        aiStreamingService.streamCall(
                content,
                promptMode,
                targetLanguage,
                LanguageEnum.EN,
                onChunk,
                onError,
                onComplete
        );
    }

    private void streamBatchContent(List<String> contentList,
                                    AiPromptModeEnum promptMode,
                                    LanguageEnum targetLanguage,
                                    Consumer<String> onChunk,
                                    Consumer<Exception> onError,
                                    Runnable onComplete) {
        StringBuilder combinedContent = new StringBuilder();
        for (String content : contentList) {
            combinedContent.append(content).append("\n\n");
        }
        streamSingleContent(combinedContent.toString(), promptMode, targetLanguage, onChunk, onError, onComplete);
    }

    // ---------------------------------------
    // Evict by videoId
    // ---------------------------------------

    @KiwiCacheKeyPrefix(AiConstants.CACHE_KEY_PREFIX_GROK.SUBTITLE_SCROLLING)
    @CacheEvict(cacheNames = AiConstants.CACHE_NAMES, keyGenerator = CacheConstants.CACHE_KEY_GENERATOR_BEAN)
    public void cleanScrollingSubtitlesCacheById(@KiwiCacheKey(1) String videoId) {
        log.info("Cleaning scrolling subtitles cache for videoId: {}", videoId);
    }

    @KiwiCacheKeyPrefix(AiConstants.CACHE_KEY_PREFIX_GROK.VIDEO_TITLE)
    @CacheEvict(cacheNames = AiConstants.CACHE_NAMES, keyGenerator = CacheConstants.CACHE_KEY_GENERATOR_BEAN)
    public void cleanVideoTitleCacheById(@KiwiCacheKey(1) String videoId) {
        log.info("Cleaning video title cache for videoId: {}", videoId);
    }

    @KiwiCacheKeyPrefix(AiConstants.CACHE_KEY_PREFIX_GROK.SUBTITLE_STREAMING)
    @CacheEvict(cacheNames = AiConstants.CACHE_NAMES, keyGenerator = CacheConstants.CACHE_KEY_GENERATOR_BEAN)
    public void cleanStreamingSubtitleTranslationCacheById(@KiwiCacheKey(1) String videoId,
                                                           @KiwiCacheKey(2) String language) {
        log.info("Cleaning streaming subtitle translation cache for videoId: {}, language: {}", videoId, language);
    }
}
