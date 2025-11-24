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
    DIRECTLY_TRANSLATION("directly-translation", "NA", 1),
    /**
     * "Directly Translate my prompt to %s language first, and provide an explanation of the translation."
     */
    TRANSLATION_AND_EXPLANATION("translation-and-explanation", "NA", 2),
    /**
     * "Translate my prompt to %s language, and provide an explanation of the grammar."
     */
    GRAMMAR_EXPLANATION("grammar-explanation", "NA", 1),
    /**
     * "Translate my prompt to %s language, and provide an explanation of the grammar correction."
     */
    GRAMMAR_CORRECTION("grammar-correction", "NA", 1),
    /**
     * Natural, idiomatic rewrite in target language.
     */
    NATURAL_IDIOMATIC_RETOUCH("natural-idiomatic-retouch", "NA", 3),
    /**
     * "Explain the vocabulary in %s language, please provide the paraphrase in English and %s of the vocabulary in different vocabulary classes,
     * for example, verb, noun, adverb, etc. and also provide the sentences that use this vocabulary with its translation in %s language of each class."
     */
    VOCABULARY_EXPLANATION("vocabulary-explanation", "NA", 3),
    /**
     * "Provide the synonym for the given word, please provide the paraphrase in English and %s of the synonym,
     * and also provide the sentences that use this synonym with its translation in %s language."
     */
    SYNONYM("synonym", "NA", 2),
    /**
     * "Provide the antonym for the given word, please provide the paraphrase in English and %s of the antonym,
     * and also provide the sentences that use this antonym with its translation in %s language."
     */
    ANTONYM("antonym", "NA", 2),
    SUBTITLE_TRANSLATOR("subtitle-translator", "NA", 1),
    SUBTITLE_RETOUCH("subtitle-retouch", "NA", 0),
    SUBTITLE_RETOUCH_TRANSLATOR("subtitle-retouch-translator", "NA", 1),
    VOCABULARY_ASSOCIATION("vocabulary-association", "NA", 3),
    PHRASES_ASSOCIATION("phrases-association", "NA", 3),
    /**
     * Expand/associate a vocabulary item across parts of speech (verbs, nouns, adverbs, adjectives).
     */
    VOCABULARY_CHARACTER_EXPANSION("vocabulary-character-expansion", "NA", 5),
    /**
     * Ambiguous vocabulary or phrase association/correction (spell-correction and commonly-confused items).
     */
    AMBIGUOUS_ASSOCIATION_CORRECTION("ambiguous-association-correction", "NA", 1),
    SELECTION_EXPLANATION("selection-explanation", "#[SM]", 0),
    CHAT("chat", "NA", 1),
    ;

    private final String mode;
    @Getter
    private final String tag;
    @Deprecated
    private final int languageWildcardCounts;

    AiPromptModeEnum(String mode, String tag, int languageWildcardCounts) {
        this.mode = mode;
        this.tag = tag;
        this.languageWildcardCounts = languageWildcardCounts;
    }

    /**
     * Get enum instance by mode value
     * @param mode the mode string to search for
     * @return the matching AiPromptModeEnum instance
     * @throws IllegalArgumentException if no matching mode is found
     */
    public static AiPromptModeEnum fromMode(String mode) {
        if (mode == null) {
            throw new IllegalArgumentException("Mode cannot be null");
        }

        for (AiPromptModeEnum enumValue : values()) {
            if (enumValue.mode.equals(mode)) {
                return enumValue;
            }
        }

        throw new IllegalArgumentException("No enum constant found for mode: " + mode);
    }

    public static final String SPLITTER = "#[SPLITTER]";

}