package me.fengorz.kiwi.ai;

import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.api.R;
import me.fengorz.kiwi.common.sdk.enumeration.AiPromptModeEnum;
import me.fengorz.kiwi.common.video.YouTuBeHelper;
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
            InputStream inputStream = youTuBeHelper.downloadVideo(videoUrl);
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
    public R<String> downloadSubtitles(@RequestParam("url") String videoUrl, @RequestParam(value = "language", required = false) String language) {
        if (language == null || "null".equals(language)) {
            String subtitles = youTuBeHelper.downloadSubtitles(videoUrl, false);
            return R.success(subtitles);
        }
        return R.success(grokAiService.batchCall(youTuBeHelper.downloadSubtitles(videoUrl, true),
                AiPromptModeEnum.SUBTITLE_TRANSLATOR, LanguageConvertor.convertLanguageToEnum(language)));
    }

    @GetMapping("/title")
    public R<String> getVideoTitle(@RequestParam("url") String videoUrl) {
        String title = youTuBeHelper.getVideoTitle(videoUrl);
        return R.success(title);
    }
}