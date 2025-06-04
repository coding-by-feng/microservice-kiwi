package me.fengorz.kiwi.ai.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.ai.api.model.request.AiTranslationRequestDTO;
import me.fengorz.kiwi.ai.api.vo.AiResponseVO;
import me.fengorz.kiwi.ai.service.AiChatService;
import me.fengorz.kiwi.ai.util.LanguageConvertor;
import me.fengorz.kiwi.ai.util.PojoBuilder;
import me.fengorz.kiwi.common.api.R;
import me.fengorz.kiwi.common.sdk.annotation.log.LogMarker;
import me.fengorz.kiwi.common.sdk.controller.BaseController;
import me.fengorz.kiwi.common.sdk.enumeration.AiPromptModeEnum;
import me.fengorz.kiwi.common.sdk.enumeration.LanguageEnum;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * @Author Kason Zhan
 * @Date 06/03/2025
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/ai")
public class AiController extends BaseController {

    private final AiChatService aiService;

    // POST method for handling long text safely
    @LogMarker(isPrintParameter = true)
    @PostMapping("/translation")
    public R<AiResponseVO> processTranslation(@Valid @RequestBody AiTranslationRequestDTO request) {
        AiPromptModeEnum mode = AiPromptModeEnum.valueOf(request.getMode().toUpperCase());
        LanguageEnum language = LanguageEnum.LANGUAGE_MAP.get(request.getLanguage());

        return R.success(PojoBuilder.buildDirectlyTranslationVO(
                request.getOriginalText(),
                LanguageConvertor.convertLanguageToEnum(request.getLanguage()),
                aiService.call(request.getOriginalText(), mode, language)
        ));
    }

    // Keep GET methods for simple/short text (with length validation)
    @LogMarker(isPrintParameter = true)
    @GetMapping("/directly-translation/{language}/{originalText}")
    public R<AiResponseVO> directlyTranslation(@PathVariable(value = "originalText") String originalText,
                                               @PathVariable(value = "language") String language) {
        // Validate text length to prevent URL issues
        if (originalText.length() > 200) {
            return R.failed("Text too long. Please use POST /ai/translation endpoint for long text.");
        }

        return processAiRequest(originalText, language, AiPromptModeEnum.DIRECTLY_TRANSLATION);
    }

    @LogMarker(isPrintParameter = true)
    @GetMapping("/translation-and-explanation/{language}/{originalText}")
    public R<AiResponseVO> translationAndExplanation(@PathVariable(value = "originalText") String originalText,
                                                     @PathVariable(value = "language") String language) {
        if (originalText.length() > 200) {
            return R.failed("Text too long. Please use POST /ai/translation endpoint for long text.");
        }

        return processAiRequest(originalText, language, AiPromptModeEnum.TRANSLATION_AND_EXPLANATION);
    }

    @LogMarker(isPrintParameter = true)
    @GetMapping("/grammar-explanation/{language}/{originalText}")
    public R<AiResponseVO> grammarExplanation(@PathVariable(value = "originalText") String originalText,
                                              @PathVariable(value = "language") String language) {
        if (originalText.length() > 200) {
            return R.failed("Text too long. Please use POST /ai/translation endpoint for long text.");
        }

        return processAiRequest(originalText, language, AiPromptModeEnum.GRAMMAR_EXPLANATION);
    }

    @LogMarker(isPrintParameter = true)
    @GetMapping("/grammar-correction/{language}/{originalText}")
    public R<AiResponseVO> grammarCorrection(@PathVariable(value = "originalText") String originalText,
                                             @PathVariable(value = "language") String language) {
        if (originalText.length() > 200) {
            return R.failed("Text too long. Please use POST /ai/translation endpoint for long text.");
        }

        return processAiRequest(originalText, language, AiPromptModeEnum.GRAMMAR_CORRECTION);
    }

    @LogMarker(isPrintParameter = true)
    @GetMapping("/vocabulary-explanation/{language}/{originalText}")
    public R<AiResponseVO> vocabularyExplanation(@PathVariable(value = "originalText") String originalText,
                                                 @PathVariable(value = "language") String language) {
        if (originalText.length() > 200) {
            return R.failed("Text too long. Please use POST /ai/translation endpoint for long text.");
        }

        return processAiRequest(originalText, language, AiPromptModeEnum.VOCABULARY_EXPLANATION);
    }

    @LogMarker(isPrintParameter = true)
    @GetMapping("/synonym/{language}/{originalText}")
    public R<AiResponseVO> synonym(@PathVariable(value = "originalText") String originalText,
                                   @PathVariable(value = "language") String language) {
        if (originalText.length() > 200) {
            return R.failed("Text too long. Please use POST /ai/translation endpoint for long text.");
        }

        return processAiRequest(originalText, language, AiPromptModeEnum.SYNONYM);
    }

    @LogMarker(isPrintParameter = true)
    @GetMapping("/antonym/{language}/{originalText}")
    public R<AiResponseVO> antonym(@PathVariable(value = "originalText") String originalText,
                                   @PathVariable(value = "language") String language) {
        if (originalText.length() > 200) {
            return R.failed("Text too long. Please use POST /ai/translation endpoint for long text.");
        }

        return processAiRequest(originalText, language, AiPromptModeEnum.ANTONYM);
    }

    // Refactored common logic
    private R<AiResponseVO> processAiRequest(String originalText, String language, AiPromptModeEnum mode) {
        try {
            // URL decode the text
            String decodedOriginalText = java.net.URLDecoder.decode(originalText, "UTF-8");

            return R.success(PojoBuilder.buildDirectlyTranslationVO(
                    decodedOriginalText,
                    LanguageConvertor.convertLanguageToEnum(language),
                    aiService.call(decodedOriginalText, mode, LanguageEnum.LANGUAGE_MAP.get(language))
            ));
        } catch (Exception e) {
            log.error("Error processing AI request", e);
            return R.failed("Failed to process request: " + e.getMessage());
        }
    }
}