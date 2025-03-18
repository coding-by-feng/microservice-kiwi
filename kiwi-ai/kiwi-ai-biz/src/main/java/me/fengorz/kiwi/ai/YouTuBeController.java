package me.fengorz.kiwi.ai;

import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.ai.api.vo.YtbSubtitlesVO;
import me.fengorz.kiwi.common.api.R;
import me.fengorz.kiwi.common.sdk.constant.GlobalConstants;
import me.fengorz.kiwi.common.sdk.enumeration.AiPromptModeEnum;
import me.fengorz.kiwi.common.sdk.enumeration.LanguageEnum;
import me.fengorz.kiwi.common.sdk.web.WebTools;
import me.fengorz.kiwi.common.ytb.YouTuBeHelper;
import me.fengorz.kiwi.common.ytb.YtbSubtitlesResult;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/ai/ytb/video")
public class YouTuBeController {

    private final YouTuBeHelper youTuBeHelper;
    private final AiChatService grokAiService;

    public YouTuBeController(YouTuBeHelper youTuBeHelper,
                             @Qualifier("grokAiService") AiChatService grokAiService) {
        this.youTuBeHelper = youTuBeHelper;
        this.grokAiService = grokAiService;
    }

    @GetMapping("/download")
    public ResponseEntity<StreamingResponseBody> downloadVideo(@RequestParam("url") String videoUrl) {
        try {
            InputStream inputStream = youTuBeHelper.downloadVideo(WebTools.decode(videoUrl));
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", UUID.randomUUID().toString());

            StreamingResponseBody stream = outputStream -> {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                inputStream.close();
            };

            return new ResponseEntity<>(stream, headers, HttpStatus.OK);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/subtitles")
    public R<YtbSubtitlesVO> downloadSubtitles(@RequestParam("url") String videoUrl, @RequestParam(value = "language", required = false) String language) {
        String decodedUrl = WebTools.decode(videoUrl);
        return R.success(buildYtbSubtitlesVoWithTranslation(decodedUrl, language, youTuBeHelper.downloadSubtitles(decodedUrl)));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private YtbSubtitlesVO buildYtbSubtitlesVoWithTranslation(String decodedUrl, String language, YtbSubtitlesResult ytbSubtitlesResult) {
        log.info("ytbSubtitlesResult: =" + ytbSubtitlesResult);
        YtbSubtitlesVO result = null;
        boolean ifNeedTranslation = language != null && !"null".equals(language);
        LanguageEnum lang = ifNeedTranslation ? LanguageConvertor.convertLanguageToEnum(language) : LanguageEnum.NONE;
        switch (ytbSubtitlesResult.getType()) {
            case SMALL_AUTO_GENERATED_RETURN_STRING: {
                String pendingToBeTranslatedOrRetouchedSubtitles = (String) ytbSubtitlesResult.getPendingToBeTranslatedOrRetouchedSubtitles();
                String translatedOrRetouchedSubtitles = ifNeedTranslation ? grokAiService.callForYtbAndCache(decodedUrl, pendingToBeTranslatedOrRetouchedSubtitles,
                        AiPromptModeEnum.SUBTITLE_RETOUCH_TRANSLATOR, lang) : grokAiService.callForYtbAndCache(decodedUrl,
                        pendingToBeTranslatedOrRetouchedSubtitles, AiPromptModeEnum.SUBTITLE_RETOUCH, lang);
                result = PojoBuilder.buildYtbSubtitlesVO(translatedOrRetouchedSubtitles,
                        ytbSubtitlesResult.getScrollingSubtitles(), ytbSubtitlesResult.getType().getValue());
                break;
            }
            case LARGE_AUTO_GENERATED_RETURN_LIST: {
                List<String> pendingToBeTranslatedOrRetouchedSubtitlesList = (List) ytbSubtitlesResult.getPendingToBeTranslatedOrRetouchedSubtitles();
                String translatedOrRetouchedSubtitles = ifNeedTranslation ? grokAiService.batchCallForYtbAndCache(decodedUrl, pendingToBeTranslatedOrRetouchedSubtitlesList,
                        AiPromptModeEnum.SUBTITLE_RETOUCH_TRANSLATOR, lang) : grokAiService.batchCallForYtbAndCache(decodedUrl,
                        pendingToBeTranslatedOrRetouchedSubtitlesList, AiPromptModeEnum.SUBTITLE_RETOUCH, lang);
                result = PojoBuilder.buildYtbSubtitlesVO(translatedOrRetouchedSubtitles,
                        ytbSubtitlesResult.getScrollingSubtitles(), ytbSubtitlesResult.getType().getValue());
                break;
            }
            case SMALL_PROFESSIONAL_RETURN_STRING:
                String pendingToBeTranslatedOrRetouchedSubtitles = (String) ytbSubtitlesResult.getPendingToBeTranslatedOrRetouchedSubtitles();
                result = PojoBuilder.buildYtbSubtitlesVO(
                        ifNeedTranslation ? grokAiService.callForYtbAndCache(decodedUrl,
                                pendingToBeTranslatedOrRetouchedSubtitles, AiPromptModeEnum.SUBTITLE_TRANSLATOR, lang) : pendingToBeTranslatedOrRetouchedSubtitles,
                        ytbSubtitlesResult.getScrollingSubtitles(),
                        ytbSubtitlesResult.getType().getValue());
                break;
            case LARGE_PROFESSIONAL_RETURN_LIST:
                List<String> pendingToBeTranslatedOrRetouchedSubtitlesList = (List) ytbSubtitlesResult.getPendingToBeTranslatedOrRetouchedSubtitles();
                result = PojoBuilder.buildYtbSubtitlesVO(ifNeedTranslation ? grokAiService.batchCallForYtbAndCache(decodedUrl,
                                pendingToBeTranslatedOrRetouchedSubtitlesList, AiPromptModeEnum.SUBTITLE_TRANSLATOR, lang)
                                : String.join(GlobalConstants.SYMBOL_LINE, pendingToBeTranslatedOrRetouchedSubtitlesList),
                        ytbSubtitlesResult.getScrollingSubtitles(),
                        ytbSubtitlesResult.getType().getValue());
                break;
        }
        return result;
    }

    @GetMapping("/title")
    public R<String> getVideoTitle(@RequestParam("url") String videoUrl) {
        String title = youTuBeHelper.getVideoTitle(WebTools.decode(videoUrl));
        return R.success(title);
    }
}