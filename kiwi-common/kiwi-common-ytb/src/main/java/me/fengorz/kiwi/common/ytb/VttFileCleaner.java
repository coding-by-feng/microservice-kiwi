package me.fengorz.kiwi.common.ytb;

import me.fengorz.kiwi.common.sdk.constant.GlobalConstants;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class VttFileCleaner {

    public static List<String> cleanDuplicatedLines(List<String> lines) {
        List<String> headers = Arrays.asList("WEBVTT", "Kind: captions");
        List<String> modifiedLines = new ArrayList<>();
        String prevLine = "";

        // Compile regex patterns for better performance
        Pattern timestampPattern = Pattern.compile("\\d{2}:\\d{2}:\\d{2}\\.\\d{3} --> \\d{2}:\\d{2}:\\d{2}\\.\\d{3}.*");
        Pattern timeTagPattern = Pattern.compile("<[^>]*>");

        for (String line : lines.stream().distinct().collect(Collectors.toList())) {
            if (line.trim().isEmpty()) {
                continue;
            }
            // Skip headers
            if (headers.contains(line.trim())) {
                modifiedLines.add(line);
                prevLine = line;
                continue;
            }

            // Skip timestamp lines and blank lines
            if (timestampPattern.matcher(line).matches()) {
                if (timestampPattern.matcher(prevLine).matches()) {
                    String prefix = prevLine.substring(0, prevLine.indexOf(TIMESTAMP_FLAG));
                    String suffix = line.substring(line.indexOf(TIMESTAMP_FLAG) + 5);
                    modifiedLines.remove(modifiedLines.size() - 1);
                    String newLine = prefix + TIMESTAMP_FLAG + suffix;
                    modifiedLines.add(newLine);
                    prevLine = newLine;
                    continue;
                }
                modifiedLines.add(line);
                prevLine = line;
                continue;
            }

            // Remove time tags
            String strippedLine = timeTagPattern.matcher(line).replaceAll("");

            // Compare with previous line
            if (!strippedLine.equals(prevLine) || prevLine.isEmpty()) {
                modifiedLines.add(line);
            }

            // Update previous line
            prevLine = strippedLine;
        }
        return modifiedLines;
    }

    public static List<String> cleanTimestamp(List<String> lines) {
        List<String> headers = Arrays.asList("WEBVTT", "Kind: captions");
        List<String> modifiedLines = new ArrayList<>();

        // Compile regex patterns for better performance
        Pattern timestampPattern = Pattern.compile("\\d{2}:\\d{2}:\\d{2}\\.\\d{3} --> \\d{2}:\\d{2}:\\d{2}\\.\\d{3}.*");

        String previousLine = GlobalConstants.EMPTY;
        for (String line : lines.stream().distinct().collect(Collectors.toList())) {
            String trimLine = line.trim();
            if (trimLine.isEmpty()) {
                continue;
            }
            // Skip headers
            // Skip headers
            if (headers.contains(line.trim())) {
                continue;
            }

            // Skip timestamp lines and blank lines
            if (timestampPattern.matcher(line).matches()) {
                continue;
            }

            if (StringUtils.equals(trimLine, previousLine.trim())) {
                continue;
            }

            modifiedLines.add(line);
            previousLine = line;
        }

        return modifiedLines;
    }

    private static final String TIMESTAMP_FLAG = " --> ";

}