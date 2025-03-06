package me.fengorz.kiwi.ai;

import lombok.experimental.UtilityClass;
import me.fengorz.kiwi.ai.api.vo.DirectlyTranslationVO;
import me.fengorz.kiwi.common.sdk.enumeration.LanguageEnum;

/**
 * Utility class to build POJOs for translation-related operations.
 */
@UtilityClass
public class PojoBuilder {

    /**
     * Builds a DirectlyTranslationVO from the translate method's parameters and response.
     *
     * @param prompt      The original text to translate.
     * @param language    The target language (LanguageEnum).
     * @param translatedText The translated text returned by the translate method.
     * @return A DirectlyTranslationVO with the original text, language code, and translated text.
     * @throws IllegalArgumentException If language is null.
     */
    public DirectlyTranslationVO buildDirectlyTranslationVO(String prompt,
                                                            LanguageEnum language, String translatedText) {
        if (language == null) {
            throw new IllegalArgumentException("LanguageEnum cannot be null");
        }

        DirectlyTranslationVO vo = new DirectlyTranslationVO();
        vo.setOriginalText(prompt);
        vo.setLanguageCode(language.getCode());
        vo.setTranslatedText(translatedText);
        return vo;
    }
}