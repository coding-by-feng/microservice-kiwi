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
    DIRECTLY_TRANSLATION("directly-translation"),
    /**
     * "Directly Translate my prompt to %s language first, and provide an explanation of the translation."
     */
    TRANSLATION_AND_EXPLANATION("translation-and-explanation"),
    /**
     * "Translate my prompt to %s language, and provide an explanation of the grammar."
     */
    GRAMMAR_EXPLANATION("grammar-explanation"),
    /**
     * "Translate my prompt to %s language, and provide an explanation of the grammar correction."
     */
    GRAMMAR_CORRECTION("grammar-correction"),
    /**
     * "Explain the vocabulary in %s language, please provide the paraphrase in English and %s of the vocabulary in different vocabulary classes,
     * for example, verb, noun, adverb, etc. and also provide the sentences that use this vocabulary with its translation in %s language of each class."
     */
    VOCABULARY_EXPLANATION("vocabulary-explanation"),
    ;

    private final String mode;

    AiPromptModeEnum(String mode) {
        this.mode = mode;
    }

}
