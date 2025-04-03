package me.fengorz.kiwi.common.sdk.enumeration;

import lombok.Getter;

/**
 * @Author Kason Zhan
 * @Date 06/03/2025
 */
@Getter
public enum AiPromptModeEnum {

    /**
     * "Directly Translate my prompt to %s language."
     */
    DIRECTLY_TRANSLATION("directly-translation", 1),
    /**
     * "Directly Translate my prompt to %s language first, and provide an explanation of the translation."
     */
    TRANSLATION_AND_EXPLANATION("translation-and-explanation", 2),
    /**
     * "Translate my prompt to %s language, and provide an explanation of the grammar."
     */
    GRAMMAR_EXPLANATION("grammar-explanation", 1),
    /**
     * "Translate my prompt to %s language, and provide an explanation of the grammar correction."
     */
    GRAMMAR_CORRECTION("grammar-correction", 1),
    /**
     * "Explain the vocabulary in %s language, please provide the paraphrase in English and %s of the vocabulary in different vocabulary classes,
     * for example, verb, noun, adverb, etc. and also provide the sentences that use this vocabulary with its translation in %s language of each class."
     */
    VOCABULARY_EXPLANATION("vocabulary-explanation", 3),
    /**
     * "Provide the synonym for the given word, please provide the paraphrase in English and %s of the synonym,
     * and also provide the sentences that use this synonym with its translation in %s language."
     */
    SYNONYM("synonym", 2),
    /**
     * "Provide the antonym for the given word, please provide the paraphrase in English and %s of the antonym,
     * and also provide the sentences that use this antonym with its translation in %s language."
     */
    ANTONYM("antonym", 2),
    SUBTITLE_TRANSLATOR("subtitle-translator", 1),
    SUBTITLE_RETOUCH("subtitle-retouch", 0),
    SUBTITLE_RETOUCH_TRANSLATOR("subtitle-retouch-translator", 1),
    CHAT("chat", 1),
    ;

    private final String mode;
    private final int languageWildcardCounts;

    AiPromptModeEnum(String mode, int languageWildcardCounts) {
        this.mode = mode;
        this.languageWildcardCounts = languageWildcardCounts;
    }

}
