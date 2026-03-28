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
package me.fengorz.kiwi.domain.ai.ytb;

import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.constant.GlobalConstants;
import me.fengorz.kiwi.common.exception.ServiceException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * YouTube Helper Service - handles YouTube video and subtitle operations using yt-dlp
 *
 * @author codingByFeng
 */
@Slf4j
@Service
public class YouTuBeHelper implements YouTubeClient {

    @Value("${kiwi.youtube.video.download.path:/tmp/youtube}")
    private String downloadPath;

    @Value("${kiwi.youtube.video.large-subtitles.threshold:1000}")
    private int largeSubtitlesThreshold;

    @Value("${kiwi.youtube.video.command:yt-dlp}")
    private String command;

    @Value("${kiwi.youtube.video.proxy.enabled:false}")
    private boolean proxyEnabled;

    @Value("${kiwi.youtube.video.proxy.value:}")
    private String proxyValue;

    @Value("${kiwi.youtube.video.subtitles.langs:en}")
    private String subtitlesLangs;

    @Value("${kiwi.youtube.video.cookies.enabled:false}")
    private boolean cookiesEnabled;

    @Value("${kiwi.youtube.video.cookies.path:}")
    private String cookiesPath;

    private static final List<String> ALLOWED_YOUTUBE_HOSTS = List.of(
            "youtube.com", "www.youtube.com", "youtu.be", "m.youtube.com", "music.youtube.com");

    private void validateYouTubeUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("URL cannot be empty");
        }
        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            if (host == null) {
                throw new IllegalArgumentException("Invalid URL: no host");
            }
            host = host.toLowerCase();
            if (ALLOWED_YOUTUBE_HOSTS.stream().noneMatch(host::equals)) {
                throw new IllegalArgumentException("Only YouTube URLs are allowed");
            }
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid URL format: " + e.getMessage());
        }
        if (url.matches(".*[`$;|&><\\n\\r].*")) {
            throw new IllegalArgumentException("URL contains forbidden characters");
        }
    }

    @Override
    public FileInputStream downloadVideo(String videoUrl) {
        validateYouTubeUrl(videoUrl);
        try {
            List<String> cmdList = new ArrayList<>();
            cmdList.add(this.command);
            applyProxyIfEnabled(cmdList);
            cmdList.add("-o");
            String currentDownloadPath = getDownloadPath();
            cmdList.add(currentDownloadPath + "/%(title)s.%(ext)s");
            cmdList.add(videoUrl);

            Process process = prepareProcess(cmdList);

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("Failed to download video, exit code: " + exitCode);
            }

            String fileName = getLatestFileName(currentDownloadPath);
            if (fileName == null) {
                throw new RuntimeException("No file downloaded for URL: " + videoUrl);
            }

            log.info("Video downloaded successfully: {}", fileName);
            File downloadedFile = new File(currentDownloadPath, fileName);
            return new FileInputStream(downloadedFile);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error downloading video for URL: {}", videoUrl, e);
            throw new RuntimeException("Failed to download video: " + e.getMessage(), e);
        }
    }

    private void applyProxyIfEnabled(List<String> cmdList) {
        if (!proxyEnabled) {
            return;
        }
        if (StringUtils.isBlank(proxyValue)) {
            log.warn("yt-dlp proxy is enabled but proxy value is blank, skipping proxy configuration.");
            return;
        }
        cmdList.add("--proxy");
        cmdList.add(proxyValue);
    }

    private void applyCookiesIfEnabled(List<String> cmdList) {
        if (!cookiesEnabled) {
            return;
        }
        if (StringUtils.isBlank(cookiesPath)) {
            log.warn("yt-dlp cookies enabled but path is blank, skipping.");
            return;
        }
        cmdList.add("--cookies");
        cmdList.add(cookiesPath);
    }

    private static Process prepareProcess(List<String> cmdList) throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder(cmdList);
        processBuilder.redirectErrorStream(true);
        log.info("Executing command: {}", String.join(" ", cmdList));
        return processBuilder.start();
    }

    public void cleanSubtitles(String videoUrl) {
        // Cache eviction handled by caller
    }

    @Override
    public YtbSubtitlesResult downloadSubtitles(String videoUrl) {
        validateYouTubeUrl(videoUrl);
        try {
            List<String> cmdList = new ArrayList<>();
            cmdList.add(this.command);
            applyProxyIfEnabled(cmdList);
            cmdList.add("--write-subs");
            cmdList.add("--write-auto-sub");
            cmdList.add("--sub-lang");
            cmdList.add(subtitlesLangs);
            cmdList.add("--skip-download");
            cmdList.add("-o");
            String currentDownloadPath = getDownloadPath();
            cmdList.add(currentDownloadPath + "/%(title)s.%(ext)s");
            cmdList.add(videoUrl);

            Process process = prepareProcess(cmdList);

            int exitCode = process.waitFor();

            BufferedReader outputReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder commandOutput = new StringBuilder();
            String line;
            while ((line = outputReader.readLine()) != null) {
                commandOutput.append(line).append("\n");
            }

            if (exitCode != 0) {
                log.error("Command execution failed with exit code: {}. Error output: {}", exitCode, commandOutput);
                throw new RuntimeException("Failed to download subtitles, exit code: " + exitCode +
                        ". Error details: " + commandOutput);
            }

            log.info("Subtitles command output: {}", commandOutput);

            String fileName = getPreferredSubtitleFileName(currentDownloadPath, subtitlesLangs);
            if (fileName == null) {
                throw new RuntimeException("No subtitles downloaded for URL: " + videoUrl);
            }
            String langCode = extractLangFromFileName(fileName, subtitlesLangs);
            log.info("Subtitles downloaded successfully: {}, lang: {}", fileName, langCode);

            File subtitleFile = new File(currentDownloadPath, fileName);
            List<String> subtitles = new ArrayList<>();
            boolean ifAutoGeneratedSubtitles = processSubtitleFile(subtitleFile, subtitles);
            if (ifAutoGeneratedSubtitles) {
                return buildAutoGeneratedYtbSubtitlesResult(videoUrl, subtitles, langCode);
            } else {
                YtbSubtitlesResult result = YtbSubtitlesResult.builder()
                        .videoUrl(videoUrl)
                        .scrollingSubtitles(String.join(GlobalConstants.SYMBOL_LINE, subtitles))
                        .langCode(langCode)
                        .build();
                if (subtitles.size() > this.largeSubtitlesThreshold) {
                    result.setType(SubtitleTypeEnum.LARGE_PROFESSIONAL_RETURN_LIST);
                    result.setPendingToBeTranslatedOrRetouchedSubtitles(VttFileCleaner.cleanTimestamp(subtitles));
                } else {
                    result.setType(SubtitleTypeEnum.SMALL_PROFESSIONAL_RETURN_STRING);
                    result.setPendingToBeTranslatedOrRetouchedSubtitles(
                            String.join(GlobalConstants.SYMBOL_LINE, VttFileCleaner.cleanTimestamp(subtitles)));
                }
                return result;
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error downloading subtitles for URL: {}", videoUrl, e);
            throw new RuntimeException("Failed to download subtitles: " + e.getMessage(), e);
        }
    }

    private YtbSubtitlesResult buildAutoGeneratedYtbSubtitlesResult(String videoUrl, List<String> subtitles,
                                                                     String langCode) {
        List<String> clearSubtitles = VttFileCleaner.cleanTimestamp(subtitles);
        YtbSubtitlesResult result = YtbSubtitlesResult.builder()
                .videoUrl(videoUrl)
                .scrollingSubtitles(
                        String.join(GlobalConstants.SYMBOL_LINE, VttFileCleaner.cleanDuplicatedLines(subtitles)))
                .langCode(langCode)
                .build();
        if (subtitles.size() > this.largeSubtitlesThreshold) {
            result.setType(SubtitleTypeEnum.LARGE_AUTO_GENERATED_RETURN_LIST);
            result.setPendingToBeTranslatedOrRetouchedSubtitles(clearSubtitles);
        } else {
            result.setType(SubtitleTypeEnum.SMALL_AUTO_GENERATED_RETURN_STRING);
            result.setPendingToBeTranslatedOrRetouchedSubtitles(
                    String.join(GlobalConstants.SYMBOL_LINE, clearSubtitles));
        }
        return result;
    }

    private boolean processSubtitleFile(File subtitleFile, List<String> subtitles) throws IOException {
        SubtitleExtractor extractor = new SubtitleExtractor();
        SubtitleExtractor.SubtitleAnalysisResult result = extractor.analyze(subtitleFile);
        subtitles.addAll(result.getSubtitles());
        log.info("Subtitles content retrieved and cleaned");
        return result.isAutoGenerated();
    }

    /**
     * Fetch video title and publish date in a single yt-dlp call.
     * Returns [title, publishedAt] - either may be null on failure.
     */
    public String[] getVideoMetadata(String videoUrl) {
        try {
            List<String> cmd = new ArrayList<>();
            cmd.add(this.command);
            applyProxyIfEnabled(cmd);
            applyCookiesIfEnabled(cmd);
            cmd.add("--skip-download");
            cmd.add("--print");
            cmd.add("%(title)s\t%(release_timestamp|timestamp|upload_date)s");
            cmd.add(videoUrl);

            Process process = prepareProcess(cmd);
            String line;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                line = reader.readLine();
            }
            process.waitFor();

            if (StringUtils.isBlank(line)) {
                return new String[]{null, null};
            }

            String[] parts = line.split("\t", 2);
            String title = parts.length > 0 && !"NA".equals(parts[0]) ? parts[0] : null;
            String dateStr = parts.length > 1 && !"NA".equals(parts[1]) ? parts[1].trim() : null;
            return new String[]{title, dateStr};
        } catch (Exception e) {
            log.warn("Failed to get video metadata for {}: {}", videoUrl, e.getMessage());
            return new String[]{null, null};
        }
    }

    /**
     * Parse a date string from yt-dlp (unix timestamp or YYYYMMDD) into LocalDateTime.
     */
    public LocalDateTime parseDateString(String dateStr) {
        if (StringUtils.isBlank(dateStr)) {
            return null;
        }
        try {
            if (dateStr.matches("\\d{10}") || dateStr.matches("\\d{13}")) {
                long epoch = dateStr.length() == 13 ? Long.parseLong(dateStr) / 1000L : Long.parseLong(dateStr);
                return LocalDateTime.ofInstant(Instant.ofEpochSecond(epoch), ZoneId.systemDefault());
            }
            if (dateStr.matches("\\d{8}")) {
                LocalDate date = LocalDate.of(
                        Integer.parseInt(dateStr.substring(0, 4)),
                        Integer.parseInt(dateStr.substring(4, 6)),
                        Integer.parseInt(dateStr.substring(6, 8)));
                return date.atStartOfDay();
            }
            return LocalDateTime.ofInstant(Instant.parse(dateStr), ZoneId.systemDefault());
        } catch (Exception e) {
            log.warn("Unrecognized date format: '{}'", dateStr);
            return null;
        }
    }

    @Override
    public String getVideoTitle(String videoUrl) {
        validateYouTubeUrl(videoUrl);
        try {
            return getVideoTitleInternal(videoUrl);
        } catch (Exception e) {
            log.warn("Failed to get video title for {} via yt-dlp: {}", videoUrl, e.getMessage());
            return null;
        }
    }

    private String getVideoTitleInternal(String videoUrl) {
        try {
            List<String> cmdList = new ArrayList<>();
            cmdList.add(this.command);
            applyProxyIfEnabled(cmdList);
            cmdList.add("--skip-download");
            cmdList.add("--print");
            cmdList.add("title");
            cmdList.add(videoUrl);
            Process process = prepareProcess(cmdList);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String title = reader.readLine();
            process.waitFor();
            return title;
        } catch (Exception e) {
            log.error("Error getting video title via yt-dlp", e);
            return null;
        }
    }

    private static boolean skipWarning(String line) {
        if (line.startsWith("WARNING:") || line.trim().isEmpty()) {
            return true;
        }
        if (line.contains("n = ") && line.contains("player = https://www.youtube.com")) {
            return true;
        }
        if (line.contains("Install PhantomJS")) {
            return true;
        }
        return false;
    }

    private String getLatestFileName(String currentDownloadPath) {
        File dir = new File(currentDownloadPath);
        File[] files = dir.listFiles((d, name) -> name.endsWith(".mp4") || name.endsWith(".webm"));
        if (files == null || files.length == 0) {
            return null;
        }
        return files[0].getName();
    }

    private String getPreferredSubtitleFileName(String currentDownloadPath, String preferredLangsCsv) {
        log.info("Selecting preferred subtitle file from directory: {} with langs: {}", currentDownloadPath,
                preferredLangsCsv);
        File dir = new File(currentDownloadPath);
        File[] files = dir.listFiles((d, name) -> name.endsWith(".vtt") || name.endsWith(".srt"));
        if (files == null || files.length == 0) {
            return null;
        }

        String[] langs = Optional.ofNullable(preferredLangsCsv)
                .map(s -> s.split(","))
                .orElse(new String[]{"en"});

        for (String langRaw : langs) {
            String lang = langRaw.trim();
            if (lang.isEmpty())
                continue;
            for (File f : files) {
                String n = f.getName();
                if (containsLangToken(n, lang)) {
                    log.info("Picked subtitle file by language '{}': {}", lang, n);
                    return n;
                }
            }
        }

        return files[0].getName();
    }

    private String extractLangFromFileName(String filename, String preferredLangsCsv) {
        String[] langs = Optional.ofNullable(preferredLangsCsv)
                .map(s -> s.split(","))
                .orElse(new String[]{"en"});
        for (String langRaw : langs) {
            String lang = langRaw.trim();
            if (lang.isEmpty())
                continue;
            if (containsLangToken(filename, lang))
                return lang;
        }
        int lastDot = filename.lastIndexOf('.');
        if (lastDot > 0) {
            String base = filename.substring(0, lastDot);
            int prevDot = base.lastIndexOf('.');
            if (prevDot > 0) {
                String guess = base.substring(prevDot + 1);
                if (guess.length() >= 2 && guess.length() <= 10) {
                    return guess;
                }
            }
        }
        return null;
    }

    private boolean containsLangToken(String filename, String lang) {
        String lower = filename.toLowerCase();
        String token = "." + lang.toLowerCase() + ".";
        if (lower.contains(token))
            return true;
        if (lower.contains("-" + lang.toLowerCase() + "."))
            return true;
        if (lower.contains("_" + lang.toLowerCase() + "."))
            return true;
        return false;
    }

    private String getDownloadPath() {
        String path = downloadPath + "/" + System.currentTimeMillis();
        File directory = new File(path);

        if (!directory.exists()) {
            boolean created = directory.mkdirs();
            if (!created) {
                log.warn("Failed to create directory: {}", path);
            }
        }

        return path;
    }

    public String extractChannelNameWithYtDlp(String channelUrl) throws ServiceException {
        validateYouTubeUrl(channelUrl);
        List<String> cmd = new ArrayList<>();
        cmd.add(this.command);
        applyProxyIfEnabled(cmd);
        cmd.add("--skip-download");
        cmd.add("--print");
        cmd.add("channel");
        cmd.add("--playlist-items");
        cmd.add("1");
        cmd.add(channelUrl);

        ProcessBuilder processBuilder = new ProcessBuilder(cmd);
        processBuilder.redirectErrorStream(true);

        try {
            Process process = processBuilder.start();

            StringBuilder output = extractChannelName(process);

            int exitCode = process.waitFor();

            if (exitCode == 0) {
                String channelName = output.toString().trim();
                log.info("Successfully extracted channel name using yt-dlp: {}", channelName);
                return channelName;
            } else {
                log.error("yt-dlp exited with code {}: {}", exitCode, output);
                throw new ServiceException("Failed to extract channel name: yt-dlp exited with code " + exitCode);
            }

        } catch (IOException | InterruptedException e) {
            log.error("Error executing yt-dlp", e);
            throw new ServiceException("Failed to extract channel name using yt-dlp", e);
        }
    }

    private static StringBuilder extractChannelName(Process process) throws IOException {
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (skipWarning(line)) {
                    continue;
                }
                output.append(line.trim());
            }
        }
        return output;
    }

    public List<String> extractAllVideoLinks(String channelLink) throws ServiceException {
        validateYouTubeUrl(channelLink);
        List<String> videoLinks = new ArrayList<>();

        List<String> cmd = new ArrayList<>();
        cmd.add(this.command);
        applyProxyIfEnabled(cmd);
        cmd.add("--flat-playlist");
        cmd.add("--get-id");
        cmd.add(channelLink);

        ProcessBuilder processBuilder = new ProcessBuilder(cmd);
        processBuilder.redirectErrorStream(true);

        try {
            Process process = processBuilder.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (skipWarning(line)) {
                        continue;
                    }

                    String videoLink = "https://www.youtube.com/watch?v=" + line.trim();
                    videoLinks.add(videoLink);
                }
            }

            int exitCode = process.waitFor();

            if (exitCode != 0) {
                log.error("yt-dlp exited with code {} when extracting video links from channel: {}",
                        exitCode, channelLink);
                throw new ServiceException("Failed to extract video links: yt-dlp exited with code " + exitCode);
            }

            log.info("Successfully extracted {} video links from channel: {}", videoLinks.size(), channelLink);
            return videoLinks;

        } catch (IOException | InterruptedException e) {
            log.error("Error executing yt-dlp to extract video links", e);
            throw new ServiceException("Failed to extract video links using yt-dlp", e);
        }
    }

    @Override
    public LocalDateTime getVideoPublishedAt(String videoUrl) {
        validateYouTubeUrl(videoUrl);
        try {
            List<String> cmd = new ArrayList<>();
            cmd.add(this.command);
            applyProxyIfEnabled(cmd);
            cmd.add("--print");
            cmd.add("%(release_timestamp|timestamp|upload_date)s");
            cmd.add(videoUrl);

            Process process = prepareProcess(cmd);

            String output;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                output = reader.readLine();
            }

            int exit = process.waitFor();
            if (exit != 0) {
                log.warn("yt-dlp publish time extraction exit code {} for {}", exit, videoUrl);
            }

            if (StringUtils.isBlank(output)) {
                return null;
            }
            output = output.trim();

            if (output.matches("\\d{10}") || output.matches("\\d{13}")) {
                long epoch = output.length() == 13 ? Long.parseLong(output) / 1000L : Long.parseLong(output);
                return LocalDateTime.ofInstant(Instant.ofEpochSecond(epoch), ZoneId.systemDefault());
            }
            if (output.matches("\\d{8}")) {
                LocalDate date = LocalDate.of(
                        Integer.parseInt(output.substring(0, 4)),
                        Integer.parseInt(output.substring(4, 6)),
                        Integer.parseInt(output.substring(6, 8)));
                return date.atStartOfDay();
            }
            try {
                return LocalDateTime.ofInstant(Instant.parse(output), ZoneId.systemDefault());
            } catch (Exception ignored) {
            }
            log.warn("Unrecognized publish time format from yt-dlp: '{}' for {}", output, videoUrl);
            return null;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Failed to get publish time via yt-dlp for {}: {}", videoUrl, e.getMessage());
            return null;
        }
    }
}
