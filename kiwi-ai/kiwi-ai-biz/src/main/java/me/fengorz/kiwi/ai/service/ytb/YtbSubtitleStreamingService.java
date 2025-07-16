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

    public YtbSubtitleStreamingService(YouTuBeHelper youTuBeHelper,
                                       @Qualifier("grokAiService") AiChatService aiChatService,
                                       @Qualifier("grokStreamingService") AiStreamingService aiStreamingService) {
        this.youTuBeHelper = youTuBeHelper;
        this.aiChatService = aiChatService;
        this.aiStreamingService = aiStreamingService;
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
                
                // Get subtitles (cached)
                YtbSubtitlesResult subtitlesResult = getScrollingSubtitles(videoUrl);

                log.debug("Subtitle result: {}", subtitlesResult);

                if (subtitlesResult == null) {
                    onError.accept(new RuntimeException("No subtitles available for this video"));
                    return;
                }
                
                // Check if translation is needed
                boolean needsTranslation = language != null && 
                                         !"null".equals(language) && 
                                         !LanguageEnum.EN.getCode().equals(language);
                
                if (!needsTranslation) {
                    // Return scrolling subtitles directly
                    onChunk.accept(subtitlesResult.getScrollingSubtitles());
                    onComplete.run();
                    return;
                }
                
                // Perform streaming translation
                LanguageEnum targetLanguage = LanguageConvertor.convertLanguageToEnum(language);
                streamTranslationWithAi(subtitlesResult, targetLanguage, onChunk, onError, onComplete);
                
            } catch (Exception e) {
                log.error("Error in subtitle translation streaming: {}", e.getMessage(), e);
                onError.accept(e);
            }
        });
    }

    /**
     * Stream translation using AI service
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void streamTranslationWithAi(YtbSubtitlesResult subtitlesResult,
                                         LanguageEnum targetLanguage,
                                         Consumer<String> onChunk,
                                         Consumer<Exception> onError,
                                         Runnable onComplete) {
        
        try {
            AiPromptModeEnum promptMode = AiPromptModeEnum.SUBTITLE_RETOUCH_TRANSLATOR;

            switch (subtitlesResult.getType()) {
                case SMALL_AUTO_GENERATED_RETURN_STRING:
                case SMALL_PROFESSIONAL_RETURN_STRING:
                    // Stream single content
                    String content = (String) subtitlesResult.getPendingToBeTranslatedOrRetouchedSubtitles();
                    streamSingleContent(content, promptMode, targetLanguage, onChunk, onError, onComplete);
                    break;
                    
                case LARGE_AUTO_GENERATED_RETURN_LIST:
                case LARGE_PROFESSIONAL_RETURN_LIST:
                    // Stream batch content
                    List<String> contentList = (List) subtitlesResult.getPendingToBeTranslatedOrRetouchedSubtitles();
                    streamBatchContent(contentList, promptMode, targetLanguage, onChunk, onError, onComplete);
                    break;
                    
                default:
                    onError.accept(new RuntimeException("Unsupported subtitle type: " + subtitlesResult.getType()));
            }
            
        } catch (Exception e) {
            log.error("Error in AI streaming translation: {}", e.getMessage(), e);
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