/*
 *
 * Copyright [2019~2025] [codingByFeng]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 *
 */

package me.fengorz.kiwi.word.biz.util;

import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.sdk.constant.GlobalConstants;
import me.fengorz.kiwi.common.sdk.util.CommonUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @Description TODO
 * @Author zhanshifeng
 * @Date 2022/9/18 19:39
 */
@Slf4j
public final class WordDataSetupUtils {

    public static Set<String> extractIeltsWordList() {
        final Set<String> wordList = new HashSet<>();
        try {
            Files.list(Paths.get(CommonUtils.getResourcePath() + "/word-list")).forEach(path -> {
                log.info("path: {}", path);
                try {
                    wordList.addAll(WordDataSetupUtils.extractWordList(path.toString()));
                } catch (IOException e) {
                    log.error("Method extractWordList invoked failed.", e);
                }
            });
        } catch (IOException e) {
            log.error("Method extractIeltsWordList invoked failed.", e);
        }
        return wordList;
    }

    public static Set<String> extractWordList(String file) throws IOException {
        Set<String> collect = Files.lines(Paths.get(file))
            .filter(line -> StringUtils.isNotBlank(line) && StringUtils.contains(line, "title: "))
            .map(line -> line.replaceAll("title: ", GlobalConstants.EMPTY).trim()).filter(StringUtils::isNotBlank)
            .peek(word -> log.info("word: {}", word)).collect(Collectors.toSet());
        Files.lines(Paths.get(file))
            .filter(line -> StringUtils.isNotBlank(line) && !StringUtils.startsWithAny(line, " ", "#"))
            .map(line -> line.replaceAll(":", GlobalConstants.EMPTY).trim())
            .filter(word -> StringUtils.isNotBlank(word) && !collect.contains(word)).peek(word -> {
                log.info("word: {}", word);
            }).collect(Collectors.toCollection(() -> collect));
        return collect;
    }

}
