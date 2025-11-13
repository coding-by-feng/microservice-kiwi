package me.fengorz.kason.ai.controller;

import lombok.extern.slf4j.Slf4j;
import me.fengorz.kason.ai.service.AiChatService;
import me.fengorz.kason.ai.service.ytb.YtbSubtitleStreamingService;
import me.fengorz.kason.ai.util.LanguageConvertor;
import me.fengorz.kason.common.api.R;
import me.fengorz.kason.common.sdk.constant.GlobalConstants;
import me.fengorz.kason.common.sdk.enumeration.AiPromptModeEnum;
import me.fengorz.kason.common.sdk.enumeration.LanguageEnum;
import me.fengorz.kason.common.sdk.web.WebTools;
import me.fengorz.kason.common.ytb.YouTuBeHelper;
import me.fengorz.kason.common.ytb.YtbSubtitlesResult;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Retryable;
import org.springframework.web.bind.annotation.*;
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
    private final YtbSubtitleStreamingService subtitleStreamingService;

    public YouTuBeController(YouTuBeHelper youTuBeHelper,
                             @Qualifier("grokAiService") AiChatService grokAiService,
                             YtbSubtitleStreamingService subtitleStreamingService) {
        this.youTuBeHelper = youTuBeHelper;
        this.grokAiService = grokAiService;
        this.subtitleStreamingService = subtitleStreamingService;
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

    /**
     * Download the translated/retouched subtitles as a .txt file.
     * Optimized version using WebSocket streaming service internally for better performance.
     */
    @GetMapping("/subtitles/translated/download")
    public ResponseEntity<StreamingResponseBody> downloadTranslatedSubtitlesAsTxt(
            @RequestParam("url") String videoUrl,
            @RequestParam(value = "language", required = false) String language) {
        try {
            String decodedUrl = WebTools.decode(videoUrl);

            // Build a safe filename first
            String rawTitle;
            try {
                rawTitle = subtitleStreamingService.getVideoTitle(videoUrl);
            } catch (Exception ex) {
                rawTitle = UUID.randomUUID().toString();
            }
            String safeTitle = (rawTitle == null || rawTitle.isEmpty()) ? UUID.randomUUID().toString()
                    : rawTitle.replaceAll("[\\\\/:*?\"<>|\\r\\n]", "_").trim();

            String langCode = null;
            try {
                LanguageEnum le = LanguageConvertor.convertLanguageToEnum(language);
                langCode = le != null ? le.getCode() : null;
            } catch (Exception ignore) {
                // ignore and leave langCode null
            }

            String filename = "subtitles-" + safeTitle + (langCode != null ? "-" + langCode : "") + ".txt";

            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.CONTENT_TYPE, "text/plain; charset=UTF-8");
            headers.setContentDispositionFormData("attachment", filename);

            // Use StreamingResponseBody for real-time streaming download
            StreamingResponseBody stream = outputStream -> {
                StringBuilder contentBuilder = new StringBuilder();
                final boolean[] hasError = {false};
                final Exception[] streamException = {null};
                final Object lock = new Object();
                final boolean[] isComplete = {false};

                try {
                    // Use the optimized streaming service (same as WebSocket endpoint)
                    subtitleStreamingService.streamSubtitleTranslation(
                        decodedUrl,
                        language,
                        // onChunk callback - stream each chunk as it arrives
                        chunk -> {
                            try {
                                synchronized (lock) {
                                    if (!hasError[0]) {
                                        // Write chunk directly to output stream for real-time download
                                        byte[] chunkBytes = chunk.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                                        outputStream.write(chunkBytes);
                                        outputStream.flush();

                                        // Also accumulate for logging purposes
                                        contentBuilder.append(chunk);

                                        log.debug("Streamed chunk of {} bytes to download", chunkBytes.length);
                                    }
                                }
                            } catch (Exception e) {
                                log.error("Error writing chunk to download stream: {}", e.getMessage(), e);
                                synchronized (lock) {
                                    hasError[0] = true;
                                    streamException[0] = e;
                                    lock.notify();
                                }
                            }
                        },
                        // onError callback
                        error -> {
                            log.error("Error in subtitle streaming for download: {}", error.getMessage(), error);
                            synchronized (lock) {
                                hasError[0] = true;
                                streamException[0] = error;
                                lock.notify();
                            }
                        },
                        // onComplete callback
                        () -> {
                            log.info("Subtitle streaming completed for download. Total content length: {} characters",
                                contentBuilder.length());
                            synchronized (lock) {
                                isComplete[0] = true;
                                lock.notify();
                            }
                        }
                    );

                    // Wait for completion or error
                    synchronized (lock) {
                        while (!isComplete[0] && !hasError[0]) {
                            try {
                                lock.wait(30000); // 30 second timeout
                                if (!isComplete[0] && !hasError[0]) {
                                    throw new RuntimeException("Subtitle streaming timeout after 30 seconds");
                                }
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                throw new RuntimeException("Subtitle streaming interrupted", e);
                            }
                        }
                    }

                    if (hasError[0] && streamException[0] != null) {
                        throw new RuntimeException("Subtitle streaming failed", streamException[0]);
                    }

                    log.info("Successfully completed streaming download for video: {}, language: {}, total size: {} bytes",
                        videoUrl, language, contentBuilder.length());

                } catch (Exception e) {
                    log.error("Error in streaming download: {}", e.getMessage(), e);
                    // Write error message to stream if possible
                    try {
                        String errorMsg = "Error generating subtitles: " + e.getMessage();
                        outputStream.write(errorMsg.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                        outputStream.flush();
                    } catch (Exception writeError) {
                        log.error("Failed to write error to stream: {}", writeError.getMessage(), writeError);
                    }
                    throw new RuntimeException("Subtitle download failed", e);
                } finally {
                    try {
                        outputStream.close();
                    } catch (Exception e) {
                        log.error("Error closing output stream: {}", e.getMessage(), e);
                    }
                }
            };

            return new ResponseEntity<>(stream, headers, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Error setting up translated subtitles download: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/subtitles/scrolling")
    public R<String> getScrollingSubtitles(@RequestParam("url") String videoUrl) {
        try {
            YtbSubtitlesResult ytbSubtitlesResult = subtitleStreamingService.getScrollingSubtitles(videoUrl);
            return R.success(ytbSubtitlesResult.getScrollingSubtitles());
        } catch (Exception e) {
            log.error("Error getting scrolling subtitles: {}", e.getMessage(), e);
            return R.failed("Failed to get scrolling subtitles: " + e.getMessage());
        }
    }

    /**
     * HTTP endpoint for subtitle translation (backward compatibility)
     * @deprecated Use WebSocket endpoint /ai/ws/ytb/subtitle for real-time streaming
     */
    @GetMapping("/subtitles/translated")
    @Deprecated
    public R<String> getTranslatedOrRetouchedSubtitles(@RequestParam("url") String videoUrl,
                                                       @RequestParam(value = "language", required = false) String language) {
        try {
            String decodedUrl = WebTools.decode(videoUrl);
            // Use caching service to fetch subtitles (cached by normalized videoId)
            YtbSubtitlesResult ytbSubtitlesResult = subtitleStreamingService.getScrollingSubtitles(videoUrl);
            String translatedOrRetouchedSubtitles = buildTranslatedOrRetouchedSubtitles(decodedUrl, language, ytbSubtitlesResult);
            return R.success(translatedOrRetouchedSubtitles);
        } catch (Exception e) {
            log.error("Error getting translated subtitles: {}", e.getMessage(), e);
            return R.failed("Failed to get translated subtitles: " + e.getMessage());
        }
    }

    /**
     * New HTTP endpoint that redirects to WebSocket recommendation
     */
    @GetMapping("/subtitles/translated/stream")
    public R<String> getTranslatedSubtitlesStreamInfo() {
        return R.success("For real-time subtitle translation with streaming support, " +
                "please use the WebSocket endpoint: ws://your-domain/ai/ws/ytb/subtitle. " +
                "This provides real-time streaming of translation results with better performance.");
    }

    @DeleteMapping("/subtitles")
    public R<Void> cleanSubtitles(@RequestParam("url") String videoUrl,
                                  @RequestParam(value = "language", required = false) String language) {
        try {
            subtitleStreamingService.cleanAllCaches(videoUrl, language);
            return R.success();
        } catch (Exception e) {
            log.error("Error cleaning subtitles cache: {}", e.getMessage(), e);
            return R.failed("Failed to clean subtitles cache: " + e.getMessage());
        }
    }

    @GetMapping("/title")
    public R<String> getVideoTitle(@RequestParam("url") String videoUrl) {
        try {
            String title = subtitleStreamingService.getVideoTitle(videoUrl);
            return R.success(title);
        } catch (Exception e) {
            log.error("Error getting video title: {}", e.getMessage(), e);
            return R.failed("Failed to get video title: " + e.getMessage());
        }
    }

    /**
     * Legacy method for backward compatibility - kept for existing HTTP clients
     * @deprecated Use YtbSubtitleStreamingService.streamSubtitleTranslation() instead
     */
    @Retryable(maxAttempts = 2, value = Exception.class)
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Deprecated
    private String buildTranslatedOrRetouchedSubtitles(String decodedUrl, String language, YtbSubtitlesResult ytbSubtitlesResult) {
        log.info("ytbSubtitlesResult: {}", ytbSubtitlesResult);
        boolean ifNeedTranslation = language != null && !"null".equals(language) && !LanguageEnum.EN.getCode().equals(language);
        LanguageEnum lang = LanguageConvertor.convertLanguageToEnum(language);

        switch (ytbSubtitlesResult.getType()) {
            case SMALL_AUTO_GENERATED_RETURN_STRING: {
                String pendingToBeTranslatedOrRetouchedSubtitles = (String) ytbSubtitlesResult.getPendingToBeTranslatedOrRetouchedSubtitles();
                return ifNeedTranslation ?
                        grokAiService.callForYtbAndCache(decodedUrl, pendingToBeTranslatedOrRetouchedSubtitles,
                                AiPromptModeEnum.SUBTITLE_RETOUCH_TRANSLATOR, lang) :
                        grokAiService.callForYtbAndCache(decodedUrl, pendingToBeTranslatedOrRetouchedSubtitles,
                                AiPromptModeEnum.SUBTITLE_RETOUCH, lang);
            }
            case LARGE_AUTO_GENERATED_RETURN_LIST: {
                List<String> pendingToBeTranslatedOrRetouchedSubtitlesList = (List) ytbSubtitlesResult.getPendingToBeTranslatedOrRetouchedSubtitles();
                return ifNeedTranslation ?
                        grokAiService.batchCallForYtbAndCache(decodedUrl, pendingToBeTranslatedOrRetouchedSubtitlesList,
                                AiPromptModeEnum.SUBTITLE_RETOUCH_TRANSLATOR, lang) :
                        grokAiService.batchCallForYtbAndCache(decodedUrl, pendingToBeTranslatedOrRetouchedSubtitlesList,
                                AiPromptModeEnum.SUBTITLE_RETOUCH, lang);
            }
            case SMALL_PROFESSIONAL_RETURN_STRING: {
                String pendingToBeTranslatedOrRetouchedSubtitles = (String) ytbSubtitlesResult.getPendingToBeTranslatedOrRetouchedSubtitles();
                return ifNeedTranslation ?
                        grokAiService.callForYtbAndCache(decodedUrl, pendingToBeTranslatedOrRetouchedSubtitles,
                                AiPromptModeEnum.SUBTITLE_TRANSLATOR, lang) :
                        GlobalConstants.EMPTY;
            }
            case LARGE_PROFESSIONAL_RETURN_LIST: {
                List<String> pendingToBeTranslatedOrRetouchedSubtitlesList = (List) ytbSubtitlesResult.getPendingToBeTranslatedOrRetouchedSubtitles();
                return ifNeedTranslation ?
                        grokAiService.batchCallForYtbAndCache(decodedUrl, pendingToBeTranslatedOrRetouchedSubtitlesList,
                                AiPromptModeEnum.SUBTITLE_TRANSLATOR, lang) :
                        GlobalConstants.EMPTY;
            }
            default:
                return GlobalConstants.EMPTY;
        }
    }
}