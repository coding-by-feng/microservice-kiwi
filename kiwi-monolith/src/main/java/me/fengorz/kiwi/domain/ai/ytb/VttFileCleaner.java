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

import me.fengorz.kiwi.common.constant.GlobalConstants;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * VTT File Cleaner - cleans and processes VTT subtitle files
 *
 * @author codingByFeng
 */
public final class VttFileCleaner {

    private static final String TIMESTAMP_FLAG = " --> ";

    private VttFileCleaner() {}

    public static List<String> cleanDuplicatedLines(List<String> lines) {
        List<String> headers = Arrays.asList("WEBVTT", "Kind: captions");
        List<String> modifiedLines = new ArrayList<>();
        String prevLine = "";

        Pattern timestampPattern = Pattern.compile("\\d{2}:\\d{2}:\\d{2}\\.\\d{3} --> \\d{2}:\\d{2}:\\d{2}\\.\\d{3}.*");
        Pattern timeTagPattern = Pattern.compile("<[^>]*>");

        for (String line : lines.stream().distinct().collect(Collectors.toList())) {
            if (line.trim().isEmpty()) {
                continue;
            }
            if (headers.contains(line.trim())) {
                modifiedLines.add(line);
                prevLine = line;
                continue;
            }

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

            String strippedLine = timeTagPattern.matcher(line).replaceAll("");

            if (!strippedLine.equals(prevLine) || prevLine.isEmpty()) {
                modifiedLines.add(line);
            }

            prevLine = strippedLine;
        }
        return modifiedLines;
    }

    public static List<String> cleanTimestamp(List<String> lines) {
        List<String> headers = Arrays.asList("WEBVTT", "Kind: captions");
        List<String> modifiedLines = new ArrayList<>();

        String previousLine = GlobalConstants.EMPTY;
        for (String line : lines.stream().distinct().collect(Collectors.toList())) {
            String trimLine = line.trim();
            if (trimLine.isEmpty()) {
                continue;
            }
            if (headers.contains(line.trim())) {
                continue;
            }

            if (line.contains(TIMESTAMP_FLAG)) {
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
}
