package me.fengorz.kiwi.common.ytb.metadata;

import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.sdk.exception.ServiceException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Legacy metadata provider that shells out to yt-dlp when the API integration is disabled.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "youtube.api.enabled", havingValue = "false")
public class YtDlpMetadataProvider implements YouTubeMetadataProvider {

    private String command = "yt-dlp";

    public YtDlpMetadataProvider() {
    }

    @Value("${youtube.video.command:yt-dlp}")
    public void setCommand(String command) {
        this.command = command;
    }

    @Override
    public String getVideoTitle(String videoUrl) {
        try {
            List<String> cmd = new ArrayList<>();
            cmd.add(this.command);
            cmd.add("--get-title");
            cmd.add(videoUrl);

            Process process = startProcess(cmd);
            StringBuilder output = readAllLines(process);

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("Failed to get video title, exit code: " + exitCode);
            }

            String fullOutput = output.toString().trim();
            int lastNewLineIndex = fullOutput.lastIndexOf('\n');
            String title;
            if (lastNewLineIndex >= 0) {
                title = fullOutput.substring(lastNewLineIndex + 1).trim();
            } else {
                title = fullOutput;
            }

            log.info("Video title retrieved via yt-dlp: {}", title);
            return title;
        } catch (Exception e) {
            log.error("Error getting video title for URL: {}", videoUrl, e);
            throw new RuntimeException("Failed to get video title: " + e.getMessage(), e);
        }
    }

    @Override
    public String getChannelName(String channelUrl) {
        List<String> cmd = new ArrayList<>();
        cmd.add(this.command);
        cmd.add("--skip-download");
        cmd.add("--print");
        cmd.add("channel");
        cmd.add("--playlist-items");
        cmd.add("1");
        cmd.add(channelUrl);

        try {
            Process process = startProcess(cmd);
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

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                String channelName = output.toString().trim();
                log.info("Channel name retrieved via yt-dlp: {}", channelName);
                return channelName;
            }

            log.error("yt-dlp exited with code {} while fetching channel name: {}", exitCode, channelUrl);
            throw new ServiceException("Failed to extract channel name: yt-dlp exited with code " + exitCode);
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Error executing yt-dlp for channel name", e);
            throw new ServiceException("Failed to extract channel name using yt-dlp", e);
        }
    }

    @Override
    public List<String> listChannelVideoLinks(String channelUrl) {
        List<String> videoLinks = new ArrayList<>();
        List<String> cmd = new ArrayList<>();
        cmd.add(this.command);
        cmd.add("--flat-playlist");
        cmd.add("--get-id");
        cmd.add(channelUrl);

        try {
            Process process = startProcess(cmd);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (skipWarning(line)) {
                        continue;
                    }
                    videoLinks.add("https://www.youtube.com/watch?v=" + line.trim());
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.error("yt-dlp exited with code {} when extracting video links from channel: {}", exitCode, channelUrl);
                throw new ServiceException("Failed to extract video links: yt-dlp exited with code " + exitCode);
            }

            log.info("Successfully extracted {} video links via yt-dlp for channel: {}", videoLinks.size(), channelUrl);
            return videoLinks;
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Error executing yt-dlp to extract video links", e);
            throw new ServiceException("Failed to extract video links using yt-dlp", e);
        }
    }

    @Override
    public LocalDateTime getVideoPublishedAt(String videoUrl) {
        try {
            List<String> cmd = new ArrayList<>();
            cmd.add(this.command);
            cmd.add("--print");
            cmd.add("%(release_timestamp|timestamp|upload_date)s");
            cmd.add(videoUrl);

            Process process = startProcess(cmd);
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

    private Process startProcess(List<String> command) throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        log.info("Executing command: {}", String.join(" ", command));
        return processBuilder.start();
    }

    private StringBuilder readAllLines(Process process) throws IOException {
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (skipWarning(line)) {
                    continue;
                }
                output.append(line);
            }
        }
        return output;
    }

    private boolean skipWarning(String line) {
        if (line == null) {
            return true;
        }
        if (line.startsWith("WARNING:") || line.trim().isEmpty()) {
            return true;
        }
        if (line.contains("n = ") && line.contains("player = https://www.youtube.com")) {
            return true;
        }
        return line.contains("Install PhantomJS");
    }
}
