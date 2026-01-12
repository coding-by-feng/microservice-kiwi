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
package me.fengorz.kiwi.api.word;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.R;
import me.fengorz.kiwi.domain.word.dto.FuzzyQueryResultDTO;
import me.fengorz.kiwi.domain.word.entity.Paraphrase;
import me.fengorz.kiwi.domain.word.entity.WordMain;
import me.fengorz.kiwi.domain.word.service.FetchQueueService;
import me.fengorz.kiwi.domain.word.service.WordMainService;
import me.fengorz.kiwi.domain.word.service.WordMainService.FuzzyQueryResult;
import me.fengorz.kiwi.domain.word.service.WordQueryService;
import me.fengorz.kiwi.domain.word.vo.WordQueryVO;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * WordMain REST Controller
 * Aligned with original microservice WordMainController
 *
 * @author codingByFeng
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/word/main")
@RequiredArgsConstructor
@Tag(name = "Word Management", description = "Word dictionary operations")
public class WordMainController {

    private static final Pattern CHINESE_PATTERN = Pattern.compile("[\\u4e00-\\u9fa5]");

    private final WordMainService wordMainService;
    private final WordQueryService wordQueryService;
    private final FetchQueueService fetchQueueService;

    /**
     * Remove word by name and trigger re-fetch
     */
    @DeleteMapping("/removeByWordName/{wordName}")
    @Operation(summary = "Remove word by name and re-fetch")
    public R<Boolean> removeByWordName(@PathVariable String wordName) {
        fetchQueueService.addToQueue(decode(wordName), 100);
        return R.ok(true);
    }

    /**
     * Query gate - routes to Chinese search or English word query
     */
    @PostMapping("/query/gate/{keyword}")
    @Operation(summary = "Smart query gate for word search")
    public R<Page<WordQueryVO>> queryGate(
            @PathVariable("keyword") String keyword,
            @RequestParam(value = "current", defaultValue = "1") long current,
            @RequestParam(value = "size", defaultValue = "20") long size) {

        String decodedKeyword = decode(keyword).toLowerCase();
        log.info("queryGate[{}]", decodedKeyword);

        if (containsChinese(decodedKeyword)) {
            // Chinese search
            Page<Paraphrase> page = new Page<>(current, size);
            return R.ok(wordQueryService.searchByChineseMeaning(decodedKeyword, page));
        } else {
            // English word query
            return queryWord(decodedKeyword);
        }
    }

    /**
     * Query word by name
     */
    @GetMapping("/query/{wordName}")
    @Operation(summary = "Query word by name with full details")
    public R<Page<WordQueryVO>> queryWord(@PathVariable("wordName") String wordName) {
        String decodedWordName = decode(wordName).toLowerCase();
        List<WordQueryVO> list = new ArrayList<>();

        wordQueryService.queryWord(decodedWordName)
                .ifPresent(list::add);

        Page<WordQueryVO> page = new Page<>(1, 1, list.size());
        page.setRecords(list);
        return R.ok(page);
    }

    /**
     * Query word by ID
     */
    @GetMapping("/queryById/{wordId}")
    @Operation(summary = "Query word by ID with full details")
    public R<WordQueryVO> queryWordById(@PathVariable Integer wordId) {
        String wordName = wordMainService.findById(wordId)
                .map(w -> w.getWordName())
                .orElse(null);

        if (wordName == null) {
            return R.failed("Word not found");
        }

        return wordQueryService.queryWord(wordName)
                .map(R::ok)
                .orElse(R.failed("Word not found"));
    }

    /**
     * Fuzzy query word list
     */
    @PostMapping("/fuzzyQueryList")
    @Operation(summary = "Fuzzy search words")
    public R<List<FuzzyQueryResultDTO>> fuzzyQueryList(
            @NotBlank @RequestParam String wordName,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size) {

        Page<WordMain> page = new Page<>(current, size);
        Page<WordMain> resultPage = wordMainService.fuzzyQuery(decode(wordName), page);

        List<FuzzyQueryResultDTO> dtoList = resultPage.getRecords().stream()
                .map(r -> new FuzzyQueryResultDTO().setValue(r.getWordName()))
                .toList();

        return R.ok(dtoList);
    }

    /**
     * List all overlapping words
     */
    @GetMapping("/listOverlapAnyway")
    @Operation(summary = "List overlapping words")
    public R<List<String>> listOverlapAnyway() {
        // TODO: Implement overlapping word detection
        return R.ok(List.of());
    }

    /**
     * Insert word variant relationship
     */
    @PostMapping("/variant/insertVariant/{inputWordName}/{fetchWordName}")
    @Operation(summary = "Insert word variant")
    public R<Void> insertVariant(
            @PathVariable String inputWordName,
            @PathVariable String fetchWordName) {

        boolean success = wordQueryService.insertVariant(
                decode(inputWordName), decode(fetchWordName));

        return success ? R.ok() : R.failed("Failed to insert variant");
    }

    // ==================== Helper Methods ====================

    private String decode(String value) {
        if (value == null) {
            return null;
        }
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return value;
        }
    }

    private boolean containsChinese(String str) {
        if (str == null) {
            return false;
        }
        return CHINESE_PATTERN.matcher(str).find();
    }
}
