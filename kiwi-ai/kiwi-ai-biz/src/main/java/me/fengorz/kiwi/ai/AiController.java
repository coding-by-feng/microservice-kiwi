package me.fengorz.kiwi.ai;

import jodd.net.URLDecoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.ai.api.vo.AiResponseVO;
import me.fengorz.kiwi.common.api.R;
import me.fengorz.kiwi.common.sdk.annotation.log.LogMarker;
import me.fengorz.kiwi.common.sdk.controller.BaseController;
import me.fengorz.kiwi.common.sdk.enumeration.AiPromptModeEnum;
import me.fengorz.kiwi.common.sdk.enumeration.LanguageEnum;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @LogMarker(isPrintParameter = true)
    @GetMapping("/directly-translation/{language}/{originalText}")
    public R<AiResponseVO> directlyTranslation(@PathVariable(value = "originalText") String originalText,
                                               @PathVariable(value = "language") String language) {
        String decodedOriginalText = getDecodedOriginalText(originalText);
        return R.success(PojoBuilder.buildDirectlyTranslationVO(decodedOriginalText, LanguageConvertor.convertLanguageToEnum(language),
                aiService.call(decodedOriginalText, AiPromptModeEnum.DIRECTLY_TRANSLATION, LanguageEnum.LANGUAGE_MAP.get(language))));
    }

    @LogMarker(isPrintParameter = true)
    @GetMapping("/translation-and-explanation/{language}/{originalText}")
    public R<AiResponseVO> translationAndExplanation(@PathVariable(value = "originalText") String originalText,
                                               @PathVariable(value = "language") String language) {
        String decodedOriginalText = getDecodedOriginalText(originalText);
        return R.success(PojoBuilder.buildDirectlyTranslationVO(decodedOriginalText, LanguageConvertor.convertLanguageToEnum(language),
                aiService.call(decodedOriginalText, AiPromptModeEnum.TRANSLATION_AND_EXPLANATION, LanguageEnum.LANGUAGE_MAP.get(language))));
    }

    @LogMarker(isPrintParameter = true)
    @GetMapping("/grammar-explanation/{language}/{originalText}")
    public R<AiResponseVO> grammarExplanation(@PathVariable(value = "originalText") String originalText,
                                               @PathVariable(value = "language") String language) {
        String decodedOriginalText = getDecodedOriginalText(originalText);
        return R.success(PojoBuilder.buildDirectlyTranslationVO(decodedOriginalText, LanguageConvertor.convertLanguageToEnum(language),
                aiService.call(decodedOriginalText, AiPromptModeEnum.GRAMMAR_EXPLANATION, LanguageEnum.LANGUAGE_MAP.get(language))));
    }

    @LogMarker(isPrintParameter = true)
    @GetMapping("/grammar-correction/{language}/{originalText}")
    public R<AiResponseVO> grammarCorrection(@PathVariable(value = "originalText") String originalText,
                                               @PathVariable(value = "language") String language) {
        String decodedOriginalText = getDecodedOriginalText(originalText);
        return R.success(PojoBuilder.buildDirectlyTranslationVO(decodedOriginalText, LanguageConvertor.convertLanguageToEnum(language),
                aiService.call(decodedOriginalText, AiPromptModeEnum.GRAMMAR_CORRECTION, LanguageEnum.LANGUAGE_MAP.get(language))));
    }

    @LogMarker(isPrintParameter = true)
    @GetMapping("/vocabulary-explanation/{language}/{originalText}")
    public R<AiResponseVO> vocabularyExplanation(@PathVariable(value = "originalText") String originalText,
                                               @PathVariable(value = "language") String language) {
        String decodedOriginalText = getDecodedOriginalText(originalText);
        return R.success(PojoBuilder.buildDirectlyTranslationVO(decodedOriginalText, LanguageConvertor.convertLanguageToEnum(language),
                aiService.call(decodedOriginalText, AiPromptModeEnum.VOCABULARY_EXPLANATION, LanguageEnum.LANGUAGE_MAP.get(language))));
    }

    private static String getDecodedOriginalText(String originalText) {
        return URLDecoder.decode(originalText);
    }

}
