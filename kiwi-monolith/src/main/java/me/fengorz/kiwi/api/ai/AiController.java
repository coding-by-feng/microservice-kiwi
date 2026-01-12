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
package me.fengorz.kiwi.api.ai;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.R;
import me.fengorz.kiwi.domain.ai.service.AiChatService;
import me.fengorz.kiwi.domain.ai.vo.AiResponseVO;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * AI Controller
 * Aligned with original microservice AiController
 *
 * @author codingByFeng
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ai")
@Tag(name = "AI", description = "AI-powered language learning operations")
public class AiController {

    private final AiChatService aiChatService;

    /**
     * Direct translation
     */
    @GetMapping("/directly-translation/{language}/{originalText}")
    @Operation(summary = "Direct translation")
    public R<AiResponseVO> directlyTranslation(
            @PathVariable("originalText") String originalText,
            @PathVariable("language") String language) {
        String decodedText = decode(originalText);
        String result = aiChatService.call(decodedText, "DIRECTLY_TRANSLATION", language);
        return R.ok(buildAiResponseVO(decodedText, language, result));
    }

    /**
     * Translation with explanation
     */
    @GetMapping("/translation-and-explanation/{language}/{originalText}")
    @Operation(summary = "Translation with explanation")
    public R<AiResponseVO> translationAndExplanation(
            @PathVariable("originalText") String originalText,
            @PathVariable("language") String language) {
        String decodedText = decode(originalText);
        String result = aiChatService.call(decodedText, "TRANSLATION_AND_EXPLANATION", language);
        return R.ok(buildAiResponseVO(decodedText, language, result));
    }

    /**
     * Grammar explanation
     */
    @GetMapping("/grammar-explanation/{language}/{originalText}")
    @Operation(summary = "Grammar explanation")
    public R<AiResponseVO> grammarExplanation(
            @PathVariable("originalText") String originalText,
            @PathVariable("language") String language) {
        String decodedText = decode(originalText);
        String result = aiChatService.call(decodedText, "GRAMMAR_EXPLANATION", language);
        return R.ok(buildAiResponseVO(decodedText, language, result));
    }

    /**
     * Grammar correction
     */
    @GetMapping("/grammar-correction/{language}/{originalText}")
    @Operation(summary = "Grammar correction")
    public R<AiResponseVO> grammarCorrection(
            @PathVariable("originalText") String originalText,
            @PathVariable("language") String language) {
        String decodedText = decode(originalText);
        String result = aiChatService.call(decodedText, "GRAMMAR_CORRECTION", language);
        return R.ok(buildAiResponseVO(decodedText, language, result));
    }

    /**
     * Vocabulary explanation
     */
    @GetMapping("/vocabulary-explanation/{language}/{originalText}")
    @Operation(summary = "Vocabulary explanation")
    public R<AiResponseVO> vocabularyExplanation(
            @PathVariable("originalText") String originalText,
            @PathVariable("language") String language) {
        String decodedText = decode(originalText);
        String result = aiChatService.call(decodedText, "VOCABULARY_EXPLANATION", language);
        return R.ok(buildAiResponseVO(decodedText, language, result));
    }

    /**
     * Find synonyms
     */
    @GetMapping("/synonym/{language}/{originalText}")
    @Operation(summary = "Find synonyms")
    public R<AiResponseVO> synonym(
            @PathVariable("originalText") String originalText,
            @PathVariable("language") String language) {
        String decodedText = decode(originalText);
        String result = aiChatService.call(decodedText, "SYNONYM", language);
        return R.ok(buildAiResponseVO(decodedText, language, result));
    }

    /**
     * Find antonyms
     */
    @GetMapping("/antonym/{language}/{originalText}")
    @Operation(summary = "Find antonyms")
    public R<AiResponseVO> antonym(
            @PathVariable("originalText") String originalText,
            @PathVariable("language") String language) {
        String decodedText = decode(originalText);
        String result = aiChatService.call(decodedText, "ANTONYM", language);
        return R.ok(buildAiResponseVO(decodedText, language, result));
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

    private AiResponseVO buildAiResponseVO(String originalText, String language, String result) {
        return new AiResponseVO()
                .setOriginalText(originalText)
                .setLanguage(language)
                .setResult(result);
    }
}
