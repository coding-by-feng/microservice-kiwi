/*
 *
 *   Copyright [2019~2025] [codingByFeng]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 *
 */

package me.fengorz.kiwi.common.tts.service.impl;

import com.google.common.collect.Sets;
import lombok.SneakyThrows;
import me.fengorz.kiwi.common.sdk.constant.GlobalConstants;
import me.fengorz.kiwi.common.sdk.exception.tts.TtsException;
import me.fengorz.kiwi.common.tts.service.TtsService;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class TtsTest {

    private final TtsService voiceRssTtsService = new VoiceRssTtsServiceImpl(null);

    private static final Set<String> FILES = Sets.newHashSet(
            // "article.txt",
            // "characteristic-of-the-continuous-tense.txt",
            // "future-continuous-tense.txt",
            "modal-1.txt",
            "modal-2.txt",
            "nominal-clause.txt",
            "past-continuous-tense.txt",
            "past-perfect-continuous-tense.txt",
            "present-perfect-continuous-tense.txt",
            "present-progressive.txt",
            "simple-future-tense.txt",
            "simple-past-tense.txt",
            "simple-present-tense.txt",
            "simple-sentences-and-complex-sentences.txt",
            "subjunctive-mood.txt",
            "the-future-perfect.txt",
            "the-past-perfect.txt",
            "the-present-perfect.txt"
    );

    // @Test
    public void generateAll() {
        assertDoesNotThrow(() -> {
            for (String file : FILES) {
                generate(file);
            }
        });
    }

    // @Test
    public void test() {
        assertDoesNotThrow(() -> {
            generate("Dream.txt");
        });
    }

    @Test
    @SneakyThrows
    public void test_CH() {
        byte[] bytes = voiceRssTtsService.speechChinese( "a。b。p。d。u。p。a");
        FileUtils.writeByteArrayToFile(new File("testCh.mp3"), bytes);
    }

    private void generate(final String file) throws IOException, TtsException {
        String path = "/Users/zhanshifeng/Library/Mobile Documents/com~apple~CloudDocs/resources/end-game/" + file;
        // String path = "/Users/zhanshifeng/Documents/myDocument/idea-project/microservice-kiwi/kiwi-common/kiwi-common-tts/src/test/resources/" + file;
        String lines = Files.lines(Paths.get(path))
                .map(line -> {
                    String trim = StringUtils.trim(line);
                    if ("```".equals(trim) || "---".equals(trim)) {
                        return null;
                    }
                    return line.replaceAll(GlobalConstants.SYMBOL_HASHTAG, " ");
                })
                .filter(Objects::nonNull)
                .collect(Collectors.joining(";"));
        byte[] bytes = voiceRssTtsService.speechChinese(lines);
        FileUtils.writeByteArrayToFile(new File(file.replaceAll("\\.txt", ".mp3")), bytes);
    }

}