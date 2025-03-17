package me.fengorz.kiwi.common.ytb;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class VttFileCleaner {

    private static final String TIMESTAMP_FLAG = " --> ";

    /**
     * Converts a subtitle string to a list of individual lines
     *
     * @param subtitles The subtitle string with line breaks
     * @return A List of individual lines
     */
    private static List<String> subtitleToLines(String subtitles) {
        // Split the subtitles by line break
        String[] linesArray = subtitles.split("\\r?\\n");

        // Convert to ArrayList
        List<String> linesList = new ArrayList<>();
        Collections.addAll(linesList, linesArray);

        return linesList;
    }

    public static <T> T cleanVtt(String subtitles, Class<T> clazz) {
        // Read the file
        List<String> lines = subtitleToLines(subtitles);

        // Define headers
        List<String> modifiedLines = modifySubtitles(lines);

        List<String> distinctModifiedLines = modifiedLines.stream().distinct().collect(Collectors.toList());

        List<String> reModifiedLines = modifySubtitles(distinctModifiedLines);

        if (List.class.equals(clazz)) {
            return clazz.cast(reModifiedLines);
        }

        return clazz.cast(String.join("\n", reModifiedLines));
    }

    @NotNull
    private static List<String> modifySubtitles(List<String> lines) {
        List<String> headers = Arrays.asList("WEBVTT", "Kind: captions", "Language: en");
        List<String> modifiedLines = new ArrayList<>();
        String prevLine = "";

        // Compile regex patterns for better performance
        Pattern timestampPattern = Pattern.compile("\\d{2}:\\d{2}:\\d{2}\\.\\d{3} --> \\d{2}:\\d{2}:\\d{2}\\.\\d{3}.*");
        Pattern timeTagPattern = Pattern.compile("<[^>]*>");

        for (String line : lines) {
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
                    String prefix = prevLine.substring(0, prevLine.indexOf(" --> "));
                    String suffix = line.substring(line.indexOf(" --> ") + 5);
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

}