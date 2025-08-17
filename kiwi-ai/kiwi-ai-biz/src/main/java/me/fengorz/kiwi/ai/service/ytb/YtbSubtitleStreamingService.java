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
import me.fengorz.kiwi.common.ytb.YouTuBeHelper;
import me.fengorz.kiwi.common.ytb.YtbSubtitlesResult;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Slf4j
@Service
@KiwiCacheKeyPrefix(AiConstants.CACHE_KEY_PREFIX_GROK.CLASS)
public class YtbSubtitleStreamingService {

    private final YouTuBeHelper youTuBeHelper;
    private final AiChatService aiChatService;
    private final AiStreamingService aiStreamingService;
    private final CacheManager cacheManager;

    public YtbSubtitleStreamingService(YouTuBeHelper youTuBeHelper,
                                       @Qualifier("grokAiService") AiChatService aiChatService,
                                       @Qualifier("grokStreamingService") AiStreamingService aiStreamingService,
                                       CacheManager cacheManager) {
        this.youTuBeHelper = youTuBeHelper;
        this.aiChatService = aiChatService;
        this.aiStreamingService = aiStreamingService;
        this.cacheManager = cacheManager;
    }

    /**
     * Get scrolling subtitles (cached)
     */
    @KiwiCacheKeyPrefix(AiConstants.CACHE_KEY_PREFIX_GROK.SUBTITLE_SCROLLING)
    @Cacheable(cacheNames = AiConstants.CACHE_NAMES, keyGenerator = CacheConstants.CACHE_KEY_GENERATOR_BEAN,
            unless = "#result == null")
    public YtbSubtitlesResult getScrollingSubtitles(@KiwiCacheKey(1) String videoUrl) {
        log.info("Fetching scrolling subtitles for video: {}", videoUrl);
        String decodedUrl = WebTools.decode(videoUrl);
        return youTuBeHelper.downloadSubtitles(decodedUrl);
    }

    /**
     * Get video title (cached)
     */
    @KiwiCacheKeyPrefix(AiConstants.CACHE_KEY_PREFIX_GROK.VIDEO_TITLE)
    @Cacheable(cacheNames = AiConstants.CACHE_NAMES, keyGenerator = CacheConstants.CACHE_KEY_GENERATOR_BEAN,
            unless = "#result == null")
    public String getVideoTitle(@KiwiCacheKey(1) String videoUrl) {
        log.info("Fetching video title for: {}", videoUrl);
        String decodedUrl = WebTools.decode(videoUrl);
        return youTuBeHelper.getVideoTitle(decodedUrl);
    }

    /**
     * Stream subtitle translation with caching support
     */
    public void streamSubtitleTranslation(String videoUrl, String language, 
                                         Consumer<String> onChunk, 
                                         Consumer<Exception> onError, 
                                         Runnable onComplete) {
        
        CompletableFuture.runAsync(() -> {
            try {
                log.info("Starting subtitle translation streaming for video: {}, language: {}", videoUrl, language);
                
                // Check if translation is needed
                boolean needsTranslation = language != null && 
                                         !"null".equals(language) && 
                                         !LanguageEnum.EN.getCode().equals(language);
                
                if (!needsTranslation) {
                    // Get subtitles (cached) and return directly
                    YtbSubtitlesResult subtitlesResult = getScrollingSubtitles(videoUrl);
                    if (subtitlesResult == null) {
                        onError.accept(new RuntimeException("No subtitles available for this video"));
                        return;
                    }
                    onChunk.accept(subtitlesResult.getScrollingSubtitles());
                    onComplete.run();
                    return;
                }

                // First, check if we have cached translation
                String cachedTranslation = getCachedStreamingSubtitleTranslation(videoUrl, language);
                if (cachedTranslation != null) {
                    log.info("Found cached streaming translation for video: {}, language: {}, length: {}",
                            videoUrl, language, cachedTranslation.length());
                    // Stream the cached content as chunks for consistency with real streaming
                    streamCachedContent(cachedTranslation, onChunk, onComplete);
                    return;
                }

                log.info("No cached translation found, performing live AI translation streaming");

                // Get subtitles (cached)
                YtbSubtitlesResult subtitlesResult = getScrollingSubtitles(videoUrl);
                log.debug("Subtitle result: {}", subtitlesResult);

                if (subtitlesResult == null) {
                    onError.accept(new RuntimeException("No subtitles available for this video"));
                    return;
                }

                // Perform streaming translation with caching
                LanguageEnum targetLanguage = LanguageConvertor.convertLanguageToEnum(language);
                streamTranslationWithAiAndCache(videoUrl, language, subtitlesResult, targetLanguage, onChunk, onError, onComplete);

            } catch (Exception e) {
                log.error("Error in subtitle translation streaming: {}", e.getMessage(), e);
                onError.accept(e);
            }
        });
    }

    /**
     * Stream cached content as chunks to maintain consistency with live streaming
     */
    private void streamCachedContent(String cachedContent, Consumer<String> onChunk, Runnable onComplete) {
        try {
            // Split content into reasonable chunks (e.g., by lines or paragraphs)
            String[] lines = cachedContent.split("\n");
            StringBuilder chunkBuilder = new StringBuilder();
            int linesPerChunk = Math.max(1, lines.length / 10); // Aim for ~10 chunks

            for (int i = 0; i < lines.length; i++) {
                chunkBuilder.append(lines[i]).append("\n");

                // Send chunk when we reach the desired size or at the end
                if ((i + 1) % linesPerChunk == 0 || i == lines.length - 1) {
                    String chunk = chunkBuilder.toString();
                    onChunk.accept(chunk);
                    chunkBuilder.setLength(0); // Clear for next chunk

                    // Add small delay to simulate streaming behavior
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
            // Fallback: send all content at once
            onChunk.accept(cachedContent);
            onComplete.run();
        }
    }

    /**
     * Stream translation using AI service with caching
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void streamTranslationWithAiAndCache(String videoUrl, String language,
                                                 YtbSubtitlesResult subtitlesResult,
                                                 LanguageEnum targetLanguage,
                                                 Consumer<String> onChunk,
                                                 Consumer<Exception> onError,
                                                 Runnable onComplete) {

        try {
            AiPromptModeEnum promptMode = AiPromptModeEnum.SUBTITLE_RETOUCH_TRANSLATOR;
            StringBuilder fullContentBuilder = new StringBuilder();

            // Create wrapped callbacks that accumulate content for caching
            Consumer<String> cachingOnChunk = chunk -> {
                fullContentBuilder.append(chunk);
                onChunk.accept(chunk); // Forward to original callback
            };

            Runnable cachingOnComplete = () -> {
                // Cache the complete translated content
                String fullTranslatedContent = fullContentBuilder.toString();
                if (fullTranslatedContent.length() > 0) {
                    cacheStreamingSubtitleTranslationManually(videoUrl, language, fullTranslatedContent);
                    log.info("Cached complete streaming translation for video: {}, language: {}, length: {}",
                            videoUrl, language, fullTranslatedContent.length());
                }
                onComplete.run(); // Forward to original callback
            };

            switch (subtitlesResult.getType()) {
                case SMALL_AUTO_GENERATED_RETURN_STRING:
                case SMALL_PROFESSIONAL_RETURN_STRING:
                    // Stream single content
                    String content = (String) subtitlesResult.getPendingToBeTranslatedOrRetouchedSubtitles();
                    streamSingleContent(content, promptMode, targetLanguage, cachingOnChunk, onError, cachingOnComplete);
                    break;
                    
                case LARGE_AUTO_GENERATED_RETURN_LIST:
                case LARGE_PROFESSIONAL_RETURN_LIST:
                    // Stream batch content
                    List<String> contentList = (List) subtitlesResult.getPendingToBeTranslatedOrRetouchedSubtitles();
                    streamBatchContent(contentList, promptMode, targetLanguage, cachingOnChunk, onError, cachingOnComplete);
                    break;
                    
                default:
                    onError.accept(new RuntimeException("Unsupported subtitle type: " + subtitlesResult.getType()));
            }
            
        } catch (Exception e) {
            log.error("Error in AI streaming translation with caching: {}", e.getMessage(), e);
            onError.accept(e);
        }
    }

    /**
     * Stream single content piece
     */
    private void streamSingleContent(String content, AiPromptModeEnum promptMode, 
                                    LanguageEnum targetLanguage,
                                    Consumer<String> onChunk, 
                                    Consumer<Exception> onError, 
                                    Runnable onComplete) {
        
        aiStreamingService.streamCall(
            content,
            promptMode,
            targetLanguage,
            LanguageEnum.EN, // Native language
            onChunk,
            onError,
            onComplete
        );
    }

    /**
     * Stream batch content by combining into single request
     */
    private void streamBatchContent(List<String> contentList, AiPromptModeEnum promptMode, 
                                   LanguageEnum targetLanguage,
                                   Consumer<String> onChunk, 
                                   Consumer<Exception> onError, 
                                   Runnable onComplete) {
        
        // Combine all content into single string for streaming
        StringBuilder combinedContent = new StringBuilder();
        for (String content : contentList) {
            combinedContent.append(content).append("\n\n");
        }
        
        streamSingleContent(combinedContent.toString(), promptMode, targetLanguage, onChunk, onError, onComplete);
    }

    /**
     * Get cached streaming subtitle translation result (if available)
     */
    @KiwiCacheKeyPrefix(AiConstants.CACHE_KEY_PREFIX_GROK.SUBTITLE_STREAMING)
    @Cacheable(cacheNames = AiConstants.CACHE_NAMES, keyGenerator = CacheConstants.CACHE_KEY_GENERATOR_BEAN,
            unless = "#result == null")
    public String getCachedStreamingSubtitleTranslation(@KiwiCacheKey(1) String videoUrl,
                                                        @KiwiCacheKey(2) String language) {
        // This method will be called only if cache miss occurs
        // The actual translation will be handled by streamSubtitleTranslation method
        return null; // Cache miss - will trigger actual translation
    }

    /**
     * Cache streaming subtitle translation result
     */
    @KiwiCacheKeyPrefix(AiConstants.CACHE_KEY_PREFIX_GROK.SUBTITLE_STREAMING)
    public void cacheStreamingSubtitleTranslation(@KiwiCacheKey(1) String videoUrl,
                                                  @KiwiCacheKey(2) String language,
                                                  String translatedContent) {
        // This method will manually put the result into cache
        // We'll use Spring's CacheManager to manually cache the result
        log.info("Caching streaming subtitle translation for video: {}, language: {}, content length: {}",
                videoUrl, language, translatedContent != null ? translatedContent.length() : 0);
    }

    /**
     * Manually cache streaming subtitle translation result using CacheManager
     */
    private void cacheStreamingSubtitleTranslationManually(String videoUrl, String language, String translatedContent) {
        try {
            Cache cache = cacheManager.getCache(AiConstants.CACHE_NAMES);
            if (cache != null) {
                // Generate cache key using the same pattern as the @Cacheable annotation
                String cacheKey = AiConstants.CACHE_KEY_PREFIX_GROK.CLASS + ":" +
                                 AiConstants.CACHE_KEY_PREFIX_GROK.SUBTITLE_STREAMING + ":" +
                                 videoUrl + ":" + language;
                cache.put(cacheKey, translatedContent);
                log.info("Successfully cached streaming translation with key: {}, content length: {}",
                        cacheKey, translatedContent.length());
            } else {
                log.warn("Cache '{}' not found, unable to cache streaming translation", AiConstants.CACHE_NAMES);
            }
        } catch (Exception e) {
            log.error("Error manually caching streaming translation for video: {}, language: {}, error: {}",
                    videoUrl, language, e.getMessage(), e);
        }
    }

    /**
     * Clean subtitle caches
     */
    @KiwiCacheKeyPrefix(AiConstants.CACHE_KEY_PREFIX_GROK.SUBTITLE_SCROLLING)
    @CacheEvict(cacheNames = AiConstants.CACHE_NAMES, keyGenerator = CacheConstants.CACHE_KEY_GENERATOR_BEAN)
    public void cleanScrollingSubtitlesCache(@KiwiCacheKey(1) String videoUrl) {
        log.info("Cleaning scrolling subtitles cache for video: {}", videoUrl);
    }

    @KiwiCacheKeyPrefix(AiConstants.CACHE_KEY_PREFIX_GROK.VIDEO_TITLE)
    @CacheEvict(cacheNames = AiConstants.CACHE_NAMES, keyGenerator = CacheConstants.CACHE_KEY_GENERATOR_BEAN)
    public void cleanVideoTitleCache(@KiwiCacheKey(1) String videoUrl) {
        log.info("Cleaning video title cache for video: {}", videoUrl);
    }

    /**
     * Clean streaming subtitle translation cache
     */
    @KiwiCacheKeyPrefix(AiConstants.CACHE_KEY_PREFIX_GROK.SUBTITLE_STREAMING)
    @CacheEvict(cacheNames = AiConstants.CACHE_NAMES, keyGenerator = CacheConstants.CACHE_KEY_GENERATOR_BEAN)
    public void cleanStreamingSubtitleTranslationCache(@KiwiCacheKey(1) String videoUrl, @KiwiCacheKey(2) String language) {
        log.info("Cleaning streaming subtitle translation cache for video: {}, language: {}", videoUrl, language);
    }

    /**
     * Clean all caches for a video
     */
    public void cleanAllCaches(String videoUrl, String language) {
        String decodedUrl = WebTools.decode(videoUrl);
        LanguageEnum lang = (language != null && !"null".equals(language)) ? 
                           LanguageConvertor.convertLanguageToEnum(language) : LanguageEnum.NONE;
        
        // Clean scrolling subtitles cache
        cleanScrollingSubtitlesCache(videoUrl);
        
        // Clean video title cache
        cleanVideoTitleCache(videoUrl);
        
        // Clean streaming translation cache
        if (language != null && !"null".equals(language)) {
            cleanStreamingSubtitleTranslationCache(videoUrl, language);
        }

        // Clean AI translation caches
        aiChatService.cleanBatchCallForYtbAndCache(decodedUrl, AiPromptModeEnum.SUBTITLE_RETOUCH_TRANSLATOR, lang);
        aiChatService.cleanBatchCallForYtbAndCache(decodedUrl, AiPromptModeEnum.SUBTITLE_RETOUCH, lang);
        aiChatService.cleanBatchCallForYtbAndCache(decodedUrl, AiPromptModeEnum.SUBTITLE_TRANSLATOR, lang);
        aiChatService.cleanCallForYtbAndCache(decodedUrl, AiPromptModeEnum.SUBTITLE_RETOUCH_TRANSLATOR, lang);
        aiChatService.cleanCallForYtbAndCache(decodedUrl, AiPromptModeEnum.SUBTITLE_TRANSLATOR, lang);
        aiChatService.cleanCallForYtbAndCache(decodedUrl, AiPromptModeEnum.SUBTITLE_RETOUCH, lang);
        
        log.info("All caches cleaned for video: {}, language: {}", videoUrl, language);
    }
}
