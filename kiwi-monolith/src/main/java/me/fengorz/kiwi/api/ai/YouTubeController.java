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
package me.fengorz.kiwi.api.ai;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.R;
import me.fengorz.kiwi.domain.ai.service.YtbSubtitleService;
import me.fengorz.kiwi.domain.ai.ytb.YouTubeClient;
import me.fengorz.kiwi.domain.ai.ytb.YtbSubtitlesResult;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * YouTube Controller
 * Handles YouTube video and subtitle operations
 *
 * @author codingByFeng
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ai/ytb/video")
@Tag(name = "YouTube", description = "YouTube video and subtitle operations")
public class YouTubeController {

    private final YouTubeClient youTubeClient;
    private final YtbSubtitleService ytbSubtitleService;

    @GetMapping("/download")
    @Operation(summary = "Download YouTube video")
    public ResponseEntity<StreamingResponseBody> downloadVideo(@RequestParam("url") String videoUrl) {
        try {
            String decodedUrl = URLDecoder.decode(videoUrl, StandardCharsets.UTF_8);
            log.info("Download video request for: {}", decodedUrl);

            InputStream inputStream = youTubeClient.downloadVideo(decodedUrl);
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
        } catch (UnsupportedOperationException uoe) {
            log.warn("Video download not supported in current YouTube mode: {}", uoe.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            log.error("Error downloading video: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/subtitles/scrolling")
    @Operation(summary = "Get scrolling subtitles")
    public R<String> getScrollingSubtitles(@RequestParam("url") String videoUrl) {
        try {
            log.info("Get scrolling subtitles for: {}", videoUrl);
            YtbSubtitlesResult result = ytbSubtitleService.getScrollingSubtitles(videoUrl);
            if (result == null) {
                return R.failed("No subtitles available for this video");
            }
            return R.ok(result.getScrollingSubtitles());
        } catch (Exception e) {
            log.error("Error getting scrolling subtitles: {}", e.getMessage(), e);
            return R.failed("Failed to get scrolling subtitles: " + e.getMessage());
        }
    }

    @GetMapping("/subtitles/scrolling-enhancement")
    @Operation(summary = "Get enhanced scrolling subtitles with punctuation while preserving original timestamps")
    public R<String> getEnhancedScrollingSubtitles(@RequestParam("url") String videoUrl) {
        try {
            log.info("Get enhanced scrolling subtitles for: {}", videoUrl);
            String result = ytbSubtitleService.getEnhancedScrollingSubtitles(videoUrl);
            if (result == null) {
                return R.failed("No subtitles available for this video");
            }
            return R.ok(result);
        } catch (Exception e) {
            log.error("Error getting enhanced scrolling subtitles: {}", e.getMessage(), e);
            return R.failed("Failed to get enhanced scrolling subtitles: " + e.getMessage());
        }
    }

    @GetMapping("/subtitles/translated")
    @Operation(summary = "Get translated subtitles")
    public R<String> getTranslatedSubtitles(
            @RequestParam("url") String videoUrl,
            @RequestParam(value = "language", required = false) String language) {
        try {
            log.info("Get translated subtitles for: {}, language: {}", videoUrl, language);
            String translated = ytbSubtitleService.getTranslatedSubtitles(videoUrl, language);
            if (translated == null) {
                return R.failed("No subtitles available for this video");
            }
            return R.ok(translated);
        } catch (Exception e) {
            log.error("Error getting translated subtitles: {}", e.getMessage(), e);
            return R.failed("Failed to get translated subtitles: " + e.getMessage());
        }
    }

    @GetMapping("/subtitles/translated/download")
    @Operation(summary = "Download translated subtitles as text file")
    public ResponseEntity<StreamingResponseBody> downloadTranslatedSubtitles(
            @RequestParam("url") String videoUrl,
            @RequestParam(value = "language", required = false) String language) {
        try {
            log.info("Download translated subtitles for: {}, language: {}", videoUrl, language);

            String rawTitle;
            try {
                rawTitle = ytbSubtitleService.getVideoTitle(videoUrl);
            } catch (Exception ex) {
                rawTitle = UUID.randomUUID().toString();
            }
            String safeTitle;
            if (rawTitle == null || rawTitle.isEmpty()) {
                safeTitle = UUID.randomUUID().toString();
            } else {
                safeTitle = rawTitle
                        .replaceAll("[\\\\/:*?\"<>|\\r\\n%]", "_")
                        .replace("..", "_")
                        .trim();
                if (safeTitle.length() > 200) {
                    safeTitle = safeTitle.substring(0, 200);
                }
            }
            String filename = "subtitles-" + safeTitle + (language != null ? "-" + language : "") + ".txt";

            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.CONTENT_TYPE, "text/plain; charset=UTF-8");
            headers.setContentDispositionFormData("attachment", filename);

            StreamingResponseBody stream = outputStream -> {
                try {
                    String translated = ytbSubtitleService.getTranslatedSubtitles(videoUrl, language);
                    if (translated != null) {
                        outputStream.write(translated.getBytes(StandardCharsets.UTF_8));
                    } else {
                        outputStream.write("No subtitles available".getBytes(StandardCharsets.UTF_8));
                    }
                } catch (Exception e) {
                    log.error("Error writing subtitles to stream: {}", e.getMessage(), e);
                    outputStream.write(("Error: " + e.getMessage()).getBytes(StandardCharsets.UTF_8));
                }
            };

            return new ResponseEntity<>(stream, headers, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Error downloading translated subtitles: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/subtitles/translated/stream")
    @Operation(summary = "Get streaming info for translated subtitles")
    public R<String> getTranslatedSubtitlesStreamInfo() {
        return R.ok("For real-time subtitle translation with streaming support, " +
                "please use the WebSocket endpoint: ws://your-domain/api/ws/ytb/subtitle");
    }

    @DeleteMapping("/subtitles")
    @Operation(summary = "Clean subtitle cache")
    public R<Void> cleanSubtitles(
            @RequestParam("url") String videoUrl,
            @RequestParam(value = "language", required = false) String language) {
        try {
            log.info("Clean subtitle cache for: {}, language: {}", videoUrl, language);
            ytbSubtitleService.cleanSubtitlesCache(videoUrl, language);
            return R.ok();
        } catch (Exception e) {
            log.error("Error cleaning subtitles cache: {}", e.getMessage(), e);
            return R.failed("Failed to clean subtitles cache: " + e.getMessage());
        }
    }

    @GetMapping("/title")
    @Operation(summary = "Get video title")
    public R<String> getVideoTitle(@RequestParam("url") String videoUrl) {
        try {
            log.info("Get video title for: {}", videoUrl);
            String title = ytbSubtitleService.getVideoTitle(videoUrl);
            if (title == null) {
                return R.failed("Could not retrieve video title");
            }
            return R.ok(title);
        } catch (Exception e) {
            log.error("Error getting video title: {}", e.getMessage(), e);
            return R.failed("Failed to get video title: " + e.getMessage());
        }
    }
}
