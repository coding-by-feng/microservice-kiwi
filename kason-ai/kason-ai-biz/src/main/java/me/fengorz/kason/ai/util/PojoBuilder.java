package me.fengorz.kason.ai.util;

import lombok.experimental.UtilityClass;
import me.fengorz.kason.ai.api.vo.AiResponseVO;
import me.fengorz.kason.common.sdk.enumeration.LanguageEnum;

/**
 * Utility class to build POJOs for translation-related operations.
 */
@UtilityClass
public class PojoBuilder {

    /**
     * Builds a AiResponseVO from the translate method's parameters and response.
     *
     * @param prompt      The original text to translate.
     * @param language    The target language (LanguageEnum).
     * @param translatedText The translated text returned by the translate method.
     * @return A AiResponseVO with the original text, language code, and translated text.
     * @throws IllegalArgumentException If language is null.
     */
    public AiResponseVO buildDirectlyTranslationVO(String prompt,
                                                   LanguageEnum language, String translatedText) {
        if (language == null) {
            throw new IllegalArgumentException("LanguageEnum cannot be null");
        }

        AiResponseVO vo = new AiResponseVO();
        vo.setOriginalText(prompt);
        vo.setLanguageCode(language.getCode());
        vo.setResponseText(translatedText);
        return vo;
    }

}