package me.fengorz.kiwi.common.video;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class YouTuBeHelper {

    @Value("${youtube.video.download.path}")
    private String downloadPath;

    @Value("${youtube.video.command}")
    private String command;

    public FileInputStream downloadVideo(String videoUrl) {
        try {
            List<String> command = new ArrayList<>();
            command.add(this.command);
            command.add("-o");
            command.add(getDownloadPath() + "/%(title)s.%(ext)s");
            command.add(videoUrl);

            Process process = prepareProcess(command);

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("Failed to download video, exit code: " + exitCode);
            }

            // Get the latest downloaded file
            String fileName = getLatestFileName();
            if (fileName == null) {
                throw new RuntimeException("No file downloaded for URL: " + videoUrl);
            }

            log.info("Video downloaded successfully: {}", fileName);
            File downloadedFile = new File(getDownloadPath(), fileName);
            return new FileInputStream(downloadedFile);
        } catch (Exception e) {
            log.error("Error downloading video for URL: {}", videoUrl, e);
            throw new RuntimeException("Failed to download video: " + e.getMessage(), e);
        }
    }

    private static Process prepareProcess(List<String> command) throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        return processBuilder.start();
    }

    public String downloadSubtitles(String videoUrl) {
        try {
            List<String> command = new ArrayList<>();
            command.add(this.command);
            command.add("--write-subs");
            command.add("--sub-lang");
            command.add("en"); // Default to English subtitles; can be parameterized
            command.add("--skip-download");
            command.add("-o");
            command.add(getDownloadPath() + "/%(title)s.%(ext)s");
            command.add(videoUrl);

            Process process = prepareProcess(command);

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("Failed to download subtitles, exit code: " + exitCode);
            }

            String fileName = getLatestSubtitleFileName();
            if (fileName == null) {
                throw new RuntimeException("No subtitles downloaded for URL: " + videoUrl);
            }
            log.info("Subtitles downloaded successfully: {}", fileName);

            File subtitleFile = new File(getDownloadPath(), fileName);
            StringBuilder subtitlesContent = new StringBuilder();
            try (BufferedReader reader = Files.newBufferedReader(subtitleFile.toPath(), StandardCharsets.UTF_8)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    subtitlesContent.append(line).append("\n");
                }
            }
            log.info("Subtitles content retrieved: {}", subtitlesContent);
            return subtitlesContent.toString();
        } catch (Exception e) {
            log.error("Error downloading subtitles for URL: {}", videoUrl, e);
            throw new RuntimeException("Failed to download subtitles: " + e.getMessage(), e);
        }
    }

    public String getVideoTitle(String videoUrl) {
        try {
            List<String> command = new ArrayList<>();
            command.add(this.command);
            command.add("--get-title");
            command.add(videoUrl);

            Process process = prepareProcess(command);

            StringBuilder output = buildOutput(process);

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("Failed to get video title, exit code: " + exitCode);
            }

            String title = output.toString().trim();
            log.info("Video title retrieved: {}", title);
            return title;

        } catch (Exception e) {
            log.error("Error getting video title for URL: {}", videoUrl, e);
            throw new RuntimeException("Failed to get video title: " + e.getMessage(), e);
        }
    }

    private static StringBuilder buildOutput(Process process) throws IOException {
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }
        return output;
    }

    private String getLatestFileName() {
        File dir = new File(getDownloadPath());
        File[] files = dir.listFiles((d, name) -> name.endsWith(".mp4") || name.endsWith(".webm"));
        if (files == null || files.length == 0) {
            return null;
        }
        return files[0].getName(); // Return the latest file (simplistic approach)
    }

    private String getLatestSubtitleFileName() {
        File dir = new File(getDownloadPath());
        File[] files = dir.listFiles((d, name) -> name.endsWith(".vtt") || name.endsWith(".srt"));
        if (files == null || files.length == 0) {
            return null;
        }
        return files[0].getName();
    }

    private String getDownloadPath() {
        return downloadPath + "/" + DOWNLOAD_DIRECTORY.get();
    }

    private static final ThreadLocal<String> DOWNLOAD_DIRECTORY;

    static {
        // Create a unique directory for each download
        DOWNLOAD_DIRECTORY = ThreadLocal.withInitial(() -> UUID.randomUUID().toString());
    }

}