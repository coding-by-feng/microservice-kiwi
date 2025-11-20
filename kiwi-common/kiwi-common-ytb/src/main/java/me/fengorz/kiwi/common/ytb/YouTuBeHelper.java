package me.fengorz.kiwi.common.ytb;

import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.sdk.annotation.cache.KiwiCacheKey;
import me.fengorz.kiwi.common.sdk.annotation.cache.KiwiCacheKeyPrefix;
import me.fengorz.kiwi.common.sdk.constant.CacheConstants;
import me.fengorz.kiwi.common.sdk.constant.GlobalConstants;
import me.fengorz.kiwi.common.sdk.exception.ServiceException;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("LoggingSimilarMessage")
@Slf4j
@Service
@KiwiCacheKeyPrefix(YtbConstants.CACHE_KEY_PREFIX_YTB.CLASS)
public class YouTuBeHelper implements YouTubeClient {

    @Value("${youtube.video.download.path}")
    private String downloadPath;

    @Value("${youtube.video.large-subtitles.threshold}")
    private int largeSubtitlesThreshold;

    @Value("${youtube.video.command}")
    private String command = "yt-dlp";

    @Value("${youtube.video.subtitles.langs:en,en-GB,en-US}")
    private String subtitlesLangs;

    @Override
    public FileInputStream downloadVideo(String videoUrl) {
        try {
            List<String> command = new ArrayList<>();
            command.add(this.command);
            command.add("-o");
            String currentDownloadPath = getDownloadPath();
            command.add(currentDownloadPath + "/%(title)s.%(ext)s");
            command.add(videoUrl);

            Process process = prepareProcess(command);

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("Failed to download video, exit code: " + exitCode);
            }

            // Get the latest downloaded file
            String fileName = getLatestFileName(currentDownloadPath);
            if (fileName == null) {
                throw new RuntimeException("No file downloaded for URL: " + videoUrl);
            }

            log.info("Video downloaded successfully: {}", fileName);
            File downloadedFile = new File(currentDownloadPath, fileName);
            return new FileInputStream(downloadedFile);
        } catch (Exception e) {
            log.error("Error downloading video for URL: {}", videoUrl, e);
            throw new RuntimeException("Failed to download video: " + e.getMessage(), e);
        }
    }

    private static Process prepareProcess(List<String> command) throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        log.info("Executing command: {}", String.join(" ", command));
        return processBuilder.start();
    }

    @SuppressWarnings("unused")
    @KiwiCacheKeyPrefix(YtbConstants.CACHE_KEY_PREFIX_YTB.SUBTITLES)
    @CacheEvict(cacheNames = YtbConstants.CACHE_NAMES, keyGenerator = CacheConstants.CACHE_KEY_GENERATOR_BEAN)
    public void cleanSubtitles(@KiwiCacheKey(1) String videoUrl) {
    }

    @KiwiCacheKeyPrefix(YtbConstants.CACHE_KEY_PREFIX_YTB.SUBTITLES)
    @Cacheable(cacheNames = YtbConstants.CACHE_NAMES, keyGenerator = CacheConstants.CACHE_KEY_GENERATOR_BEAN,
            unless = "#result == null")
    @Override
    public YtbSubtitlesResult downloadSubtitles(@KiwiCacheKey(1) String videoUrl) {
        try {
           List<String> command = new ArrayList<>();
            command.add(this.command);
            command.add("--write-subs");
            command.add("--write-auto-sub");
            command.add("--sub-lang");
            command.add(subtitlesLangs); // languages preference order
            command.add("--skip-download");
            command.add("-o");
            String currentDownloadPath = getDownloadPath();
            command.add(currentDownloadPath + "/%(title)s.%(ext)s");
            command.add(videoUrl);

            Process process = prepareProcess(command);

            int exitCode = process.waitFor();

            // Capture the output
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
                    result.setPendingToBeTranslatedOrRetouchedSubtitles(String.join(GlobalConstants.SYMBOL_LINE, VttFileCleaner.cleanTimestamp(subtitles)));
                }
                return result;
            }
        } catch (Exception e) {
            log.error("Error downloading subtitles for URL: {}", videoUrl, e);
            throw new RuntimeException("Failed to download subtitles: " + e.getMessage(), e);
        }
    }

    private YtbSubtitlesResult buildAutoGeneratedYtbSubtitlesResult(String videoUrl, List<String> subtitles, String langCode) {
        List<String> clearSubtitles = VttFileCleaner.cleanTimestamp(subtitles);
        YtbSubtitlesResult result = YtbSubtitlesResult.builder()
                .videoUrl(videoUrl)
                .scrollingSubtitles(String.join(GlobalConstants.SYMBOL_LINE, VttFileCleaner.cleanDuplicatedLines(subtitles)))
                .langCode(langCode)
                .build();
        if (subtitles.size() > this.largeSubtitlesThreshold) {
            result.setType(SubtitleTypeEnum.LARGE_AUTO_GENERATED_RETURN_LIST);
            result.setPendingToBeTranslatedOrRetouchedSubtitles(clearSubtitles);
        } else {
            result.setType(SubtitleTypeEnum.SMALL_AUTO_GENERATED_RETURN_STRING);
            result.setPendingToBeTranslatedOrRetouchedSubtitles(String.join(GlobalConstants.SYMBOL_LINE, clearSubtitles));
        }
        return result;
    }

    private boolean processSubtitleFile(File subtitleFile, List<String> subtitles) throws IOException {
        boolean ifAutoGeneratedSubtitles = false;
        String previousLine = null;

        try (BufferedReader reader = Files.newBufferedReader(subtitleFile.toPath(), StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Skip empty lines
                if (line.trim().isEmpty()) {
                    continue;
                }

                if (StringUtils.contains(previousLine, "-->") && StringUtils.contains(line, "-->")) {
                    continue;
                }

                if (line.contains("<c>") && line.contains("</c>")) {
                    ifAutoGeneratedSubtitles = true;
                    continue;
                }

                // Check if this is a timestamp line
                if (line.matches("\\d{2}:\\d{2}:\\d{2}\\.\\d{3}\\s+-->\\s+\\d{2}:\\d{2}:\\d{2}\\.\\d{3}.*")) {
                    // Remove position formatting
                    String cleanSubtitleLine = cleanSubtitleLine(line);
                    subtitles.add(cleanSubtitleLine);
                    previousLine = cleanSubtitleLine;
                    continue;
                }

                // Clean the subtitle line
                String cleanedLine = cleanSubtitleLine(line);
                // Skip if this is a duplicate of the previous line
                if (StringUtils.equals(previousLine, cleanedLine)) {
                    continue;
                }

                subtitles.add(cleanedLine);
                previousLine = cleanedLine;
            }
        }

        log.info("Subtitles content retrieved and cleaned");
        return ifAutoGeneratedSubtitles;
    }

    @Override
    public String getVideoTitle(String videoUrl) {
        try {
            return getVideoTitleInternal(videoUrl);
        } catch (Exception e) {
            log.warn("Failed to get video title for {} via yt-dlp: {}", videoUrl, e.getMessage());
            return null;
        }
    }

    // Existing public method name changed to avoid collision if needed
    private String getVideoTitleInternal(String videoUrl) {
        try {
            List<String> command = new ArrayList<>();
            command.add(this.command);
            command.add("--skip-download");
            command.add("--print" );
            command.add("title");
            command.add(videoUrl);
            Process process = prepareProcess(command);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String title = reader.readLine();
            process.waitFor();
            return title;
        } catch (Exception e) {
            log.error("Error getting video title via yt-dlp", e);
            return null;
        }
    }

    private static StringBuilder buildTitleOutput(Process process) throws IOException {
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (skipWarning(line)) continue;
                output.append(line);
            }
        }
        return output;
    }

    private static boolean skipWarning(String line) {
        // Skip warning lines and empty lines
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
        return files[0].getName(); // Return the latest file (simplistic approach)
    }

    private String getPreferredSubtitleFileName(String currentDownloadPath, String preferredLangsCsv) {
        log.info("Selecting preferred subtitle file from directory: {} with langs: {}", currentDownloadPath, preferredLangsCsv);
        File dir = new File(currentDownloadPath);
        File[] files = dir.listFiles((d, name) -> name.endsWith(".vtt") || name.endsWith(".srt"));
        if (files == null || files.length == 0) {
            return null;
        }

        String[] langs = Optional.ofNullable(preferredLangsCsv)
                .map(s -> s.split(","))
                .orElse(new String[]{"en"});

        // Try to find file that contains a preferred language code
        for (String langRaw : langs) {
            String lang = langRaw.trim();
            if (lang.isEmpty()) continue;
            for (File f : files) {
                String n = f.getName();
                if (containsLangToken(n, lang)) {
                    log.info("Picked subtitle file by language '{}': {}", lang, n);
                    return n;
                }
            }
        }

        // Fallback to the first file
        return files[0].getName();
    }

    private String extractLangFromFileName(String filename, String preferredLangsCsv) {
        String[] langs = Optional.ofNullable(preferredLangsCsv)
                .map(s -> s.split(","))
                .orElse(new String[]{"en"});
        for (String langRaw : langs) {
            String lang = langRaw.trim();
            if (lang.isEmpty()) continue;
            if (containsLangToken(filename, lang)) return lang;
        }
        // Try to guess by taking the token before extension when pattern like *.en.vtt
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
        if (lower.contains(token)) return true;
        // also check separators '-' and '_'
        if (lower.contains("-" + lang.toLowerCase() + ".")) return true;
        if (lower.contains("_" + lang.toLowerCase() + ".")) return true;
        return false;
    }

    private String getDownloadPath() {
        String path = downloadPath + "/" + System.currentTimeMillis();
        File directory = new File(path);

        // Create the directory if it doesn't exist
        if (!directory.exists()) {
            boolean created = directory.mkdirs();
            if (!created) {
                log.warn("Failed to create directory: {}", path);
                // You might want to handle this case depending on your requirements
                // Options: throw an exception, use a default path, etc.
            }
        }

        return path;
    }

    /**
     * Cleans subtitle line by removing HTML entities and returns null if the line
     * contains formatting tags or timestamps.
     *
     * @param line The raw subtitle line to clean
     * @return The cleaned subtitle text or null if line contains formatting tags or timestamps
     */
    private String cleanSubtitleLine(String line) {
        // Check if line contains formatting tags or timestamps
        // Remove all HTML entities
        String result = line;
        result = result.replaceAll("&nbsp;", "")
                .replaceAll("\\s+align:start position:0%$", "");

        // Clean up multiple spaces and trim the result
        result = result.replaceAll("\\s+", " ").trim();

        return result;
    }

    /**
     * Extract channel name using yt-dlp command-line tool
     *
     * @param channelUrl YouTube channel URL
     * @return Channel name
     * @throws ServiceException If extraction fails
     */
    public String extractChannelNameWithYtDlp(String channelUrl) throws ServiceException {
        ProcessBuilder processBuilder = new ProcessBuilder(
                this.command,
                "--skip-download",
                "--print", "channel",
                "--playlist-items", "1",
                channelUrl
        );

        processBuilder.redirectErrorStream(true);

        try {
            Process process = processBuilder.start();

            StringBuilder output = extractChannelName(process);

            // Wait for the process to complete
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

    @NotNull
    private static StringBuilder extractChannelName(Process process) throws IOException {
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (skipWarning(line)) {
                    continue;
                }
                // Found a non-warning, non-empty line (likely the channel name)
                output.append(line.trim());
                // Don't break here - continue reading to drain the process output
                // but we'll only keep the first valid line
            }
        }
        return output;
    }

    /**
     * Extract all video links from a YouTube channel
     *
     * @param channelLink YouTube channel URL
     * @return List of unique video URLs from the channel
     * @throws ServiceException If extraction fails
     */
    public List<String> extractAllVideoLinks(String channelLink) throws ServiceException {
        List<String> videoLinks = new ArrayList<>();

        ProcessBuilder processBuilder = new ProcessBuilder(
                this.command,
                "--flat-playlist",
                "--get-id",
                channelLink
        );

        processBuilder.redirectErrorStream(true);

        try {
            Process process = processBuilder.start();

            // Read the output (video IDs)
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // Skip warning lines and empty lines
                    if (skipWarning(line)) {
                        continue;
                    }

                    // Create full YouTube URL from video ID
                    String videoLink = "https://www.youtube.com/watch?v=" + line.trim();
                    videoLinks.add(videoLink);
                }
            }

            // Wait for the process to complete
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

    /**
     * Try to get video publication datetime using yt-dlp as a fallback.
     * Priority order: release_timestamp, timestamp, upload_date.
     *
     * @param videoUrl YouTube video URL or ID
     * @return LocalDateTime if parsed; null otherwise
     */
    @Override
    public LocalDateTime getVideoPublishedAt(String videoUrl) {
        try {
            List<String> cmd = new ArrayList<>();
            cmd.add(this.command);
            cmd.add("--print");
            // Try multiple fields; yt-dlp will print first non-empty due to | operator
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
                        Integer.parseInt(output.substring(6, 8))
                );
                return date.atStartOfDay();
            }
            try {
                return LocalDateTime.ofInstant(Instant.parse(output), ZoneId.systemDefault());
            } catch (Exception ignored) {
            }
            log.warn("Unrecognized publish time format from yt-dlp: '{}' for {}", output, videoUrl);
            return null;
        } catch (Exception e) {
            log.warn("Failed to get publish time via yt-dlp for {}: {}", videoUrl, e.getMessage());
            return null;
        }
    }

}
