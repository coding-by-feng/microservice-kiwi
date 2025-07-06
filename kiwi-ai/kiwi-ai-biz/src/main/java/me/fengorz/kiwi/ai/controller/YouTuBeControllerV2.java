package me.fengorz.kiwi.ai.controller;

import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.ai.api.vo.ytb.CaptionResponse;
import me.fengorz.kiwi.ai.api.vo.ytb.ChannelDetailsResponse;
import me.fengorz.kiwi.ai.api.vo.ytb.VideoDetailsResponse;
import me.fengorz.kiwi.ai.api.vo.ytb.YtbSubtitlesVO;
import me.fengorz.kiwi.ai.service.AiChatService;
import me.fengorz.kiwi.ai.service.ytb.YouTuBeHelperV2;
import me.fengorz.kiwi.ai.service.ytb.YouTubeApiService;
import me.fengorz.kiwi.ai.util.LanguageConvertor;
import me.fengorz.kiwi.common.api.R;
import me.fengorz.kiwi.common.sdk.constant.GlobalConstants;
import me.fengorz.kiwi.common.sdk.enumeration.AiPromptModeEnum;
import me.fengorz.kiwi.common.sdk.enumeration.LanguageEnum;
import me.fengorz.kiwi.common.sdk.web.WebTools;
import me.fengorz.kiwi.common.ytb.YtbSubtitlesResult;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.retry.annotation.Retryable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/ai/ytb/v2")
public class YouTuBeControllerV2 {

    private final YouTuBeHelperV2 youTuBeHelper;
    private final YouTubeApiService youTubeApiService;
    private final AiChatService grokAiService;

    public YouTuBeControllerV2(YouTuBeHelperV2 youTuBeHelper,
                              YouTubeApiService youTubeApiService,
                              @Qualifier("grokAiService") AiChatService grokAiService) {
        this.youTuBeHelper = youTuBeHelper;
        this.youTubeApiService = youTubeApiService;
        this.grokAiService = grokAiService;
    }

    @GetMapping("/video/details")
    public R<VideoDetailsResponse> getVideoDetails(@RequestParam("url") String videoUrl) {
        try {
            String decodedUrl = WebTools.decode(videoUrl);
            VideoDetailsResponse videoDetails = youTubeApiService.getVideoDetails(decodedUrl);
            return R.success(videoDetails);
        } catch (Exception e) {
            log.error("Error getting video details: {}", e.getMessage(), e);
            return R.failed("Failed to get video details: " + e.getMessage());
        }
    }

    @GetMapping("/channel/details")
    public R<ChannelDetailsResponse> getChannelDetails(@RequestParam("url") String channelUrl) {
        try {
            String decodedUrl = WebTools.decode(channelUrl);
            ChannelDetailsResponse channelDetails = youTubeApiService.getChannelDetails(decodedUrl);
            return R.success(channelDetails);
        } catch (Exception e) {
            log.error("Error getting channel details: {}", e.getMessage(), e);
            return R.failed("Failed to get channel details: " + e.getMessage());
        }
    }

    @GetMapping("/video/captions")
    public R<List<CaptionResponse>> getVideoCaptions(@RequestParam("url") String videoUrl) {
        try {
            String decodedUrl = WebTools.decode(videoUrl);
            List<CaptionResponse> captions = youTubeApiService.getVideoCaptions(decodedUrl);
            return R.success(captions);
        } catch (Exception e) {
            log.error("Error getting video captions: {}", e.getMessage(), e);
            return R.failed("Failed to get video captions: " + e.getMessage());
        }
    }

    @GetMapping("/subtitles")
    public R<YtbSubtitlesVO> downloadSubtitles(@RequestParam("url") String videoUrl, 
                                              @RequestParam(value = "language", required = false) String language) {
        String decodedUrl = WebTools.decode(videoUrl);
        return R.success(buildYtbSubtitlesVoWithTranslation(decodedUrl, language, youTuBeHelper.downloadSubtitles(decodedUrl)));
    }

    @DeleteMapping("/subtitles")
    public R<Void> cleanSubtitles(@RequestParam("url") String videoUrl, 
                                 @RequestParam(value = "language", required = false) String language) {
        String decodedVideoUrl = WebTools.decode(videoUrl);
        boolean ifNeedTranslation = language != null && !"null".equals(language);
        LanguageEnum lang = ifNeedTranslation ? LanguageConvertor.convertLanguageToEnum(language) : LanguageEnum.NONE;
        
        youTuBeHelper.cleanSubtitles(decodedVideoUrl);
        grokAiService.cleanBatchCallForYtbAndCache(decodedVideoUrl, AiPromptModeEnum.SUBTITLE_RETOUCH_TRANSLATOR, lang);
        grokAiService.cleanBatchCallForYtbAndCache(decodedVideoUrl, AiPromptModeEnum.SUBTITLE_RETOUCH, lang);
        grokAiService.cleanBatchCallForYtbAndCache(decodedVideoUrl, AiPromptModeEnum.SUBTITLE_TRANSLATOR, lang);
        grokAiService.cleanCallForYtbAndCache(decodedVideoUrl, AiPromptModeEnum.SUBTITLE_RETOUCH_TRANSLATOR, lang);
        grokAiService.cleanCallForYtbAndCache(decodedVideoUrl, AiPromptModeEnum.SUBTITLE_TRANSLATOR, lang);
        grokAiService.cleanCallForYtbAndCache(decodedVideoUrl, AiPromptModeEnum.SUBTITLE_RETOUCH, lang);
        
        return R.success();
    }

    @GetMapping("/title")
    public R<String> getVideoTitle(@RequestParam("url") String videoUrl) {
        try {
            String title = youTuBeHelper.getVideoTitle(WebTools.decode(videoUrl));
            return R.success(title);
        } catch (Exception e) {
            log.error("Error getting video title: {}", e.getMessage(), e);
            return R.failed("Failed to get video title: " + e.getMessage());
        }
    }

    @Retryable(maxAttempts = 2, value = Exception.class)
    @SuppressWarnings({"rawtypes", "unchecked"})
    private YtbSubtitlesVO buildYtbSubtitlesVoWithTranslation(String decodedUrl, String language, YtbSubtitlesResult ytbSubtitlesResult) {
        log.info("ytbSubtitlesResult: {}", ytbSubtitlesResult);
        YtbSubtitlesVO result = null;
        boolean ifNeedTranslation = language != null && !"null".equals(language);
        LanguageEnum lang = ifNeedTranslation ? LanguageConvertor.convertLanguageToEnum(language) : LanguageEnum.NONE;
        
        switch (ytbSubtitlesResult.getType()) {
            case SMALL_AUTO_GENERATED_RETURN_STRING: {
                String pendingToBeTranslatedOrRetouchedSubtitles = (String) ytbSubtitlesResult.getPendingToBeTranslatedOrRetouchedSubtitles();
                result = YtbSubtitlesVO.builder()
                        .translatedOrRetouchedSubtitles(ifNeedTranslation ? grokAiService.callForYtbAndCache(decodedUrl, pendingToBeTranslatedOrRetouchedSubtitles,
                                AiPromptModeEnum.SUBTITLE_RETOUCH_TRANSLATOR, lang) : grokAiService.callForYtbAndCache(decodedUrl,
                                pendingToBeTranslatedOrRetouchedSubtitles, AiPromptModeEnum.SUBTITLE_RETOUCH, lang))
                        .scrollingSubtitles(ytbSubtitlesResult.getScrollingSubtitles())
                        .type(ytbSubtitlesResult.getType().getValue())
                        .build();
                break;
            }
            case LARGE_AUTO_GENERATED_RETURN_LIST: {
                List<String> pendingToBeTranslatedOrRetouchedSubtitlesList = (List) ytbSubtitlesResult.getPendingToBeTranslatedOrRetouchedSubtitles();
                result = YtbSubtitlesVO.builder()
                        .translatedOrRetouchedSubtitles(ifNeedTranslation ? grokAiService.batchCallForYtbAndCache(decodedUrl, pendingToBeTranslatedOrRetouchedSubtitlesList,
                                AiPromptModeEnum.SUBTITLE_RETOUCH_TRANSLATOR, lang) : grokAiService.batchCallForYtbAndCache(decodedUrl,
                                pendingToBeTranslatedOrRetouchedSubtitlesList, AiPromptModeEnum.SUBTITLE_RETOUCH, lang))
                        .scrollingSubtitles(ytbSubtitlesResult.getScrollingSubtitles())
                        .type(ytbSubtitlesResult.getType().getValue())
                        .build();
                break;
            }
            case SMALL_PROFESSIONAL_RETURN_STRING:
                String pendingToBeTranslatedOrRetouchedSubtitles = (String) ytbSubtitlesResult.getPendingToBeTranslatedOrRetouchedSubtitles();
                result = YtbSubtitlesVO.builder()
                        .translatedOrRetouchedSubtitles(ifNeedTranslation ? grokAiService.callForYtbAndCache(decodedUrl,
                                pendingToBeTranslatedOrRetouchedSubtitles, AiPromptModeEnum.SUBTITLE_TRANSLATOR, lang) : GlobalConstants.EMPTY)
                        .type(ytbSubtitlesResult.getType().getValue())
                        .scrollingSubtitles(ytbSubtitlesResult.getScrollingSubtitles())
                        .build();
                break;
            case LARGE_PROFESSIONAL_RETURN_LIST:
                List<String> pendingToBeTranslatedOrRetouchedSubtitlesList = (List) ytbSubtitlesResult.getPendingToBeTranslatedOrRetouchedSubtitles();
                result = YtbSubtitlesVO.builder()
                        .translatedOrRetouchedSubtitles(ifNeedTranslation ? grokAiService.batchCallForYtbAndCache(decodedUrl,
                                pendingToBeTranslatedOrRetouchedSubtitlesList, AiPromptModeEnum.SUBTITLE_TRANSLATOR, lang) : GlobalConstants.EMPTY)
                        .scrollingSubtitles(ytbSubtitlesResult.getScrollingSubtitles())
                        .type(ytbSubtitlesResult.getType().getValue())
                        .build();
                break;
        }
        return result;
    }
}