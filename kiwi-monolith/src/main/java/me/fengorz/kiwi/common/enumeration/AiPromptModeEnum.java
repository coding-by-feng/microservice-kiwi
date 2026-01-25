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
package me.fengorz.kiwi.common.enumeration;

import lombok.Getter;

/**
 * AI Prompt Mode Enumeration
 *
 * @author codingByFeng
 */
@Getter
public enum AiPromptModeEnum {

    DIRECTLY_TRANSLATION("directly-translation", "NA", 1),
    TRANSLATION_AND_EXPLANATION("translation-and-explanation", "NA", 2),
    GRAMMAR_EXPLANATION("grammar-explanation", "NA", 1),
    GRAMMAR_CORRECTION("grammar-correction", "NA", 1),
    NATURAL_IDIOMATIC_RETOUCH("natural-idiomatic-retouch", "NA", 3),
    VOCABULARY_EXPLANATION("vocabulary-explanation", "NA", 3),
    SYNONYM("synonym", "NA", 2),
    ANTONYM("antonym", "NA", 2),
    SUBTITLE_TRANSLATOR("subtitle-translator", "NA", 1),
    SUBTITLE_RETOUCH("subtitle-retouch", "NA", 0),
    SUBTITLE_RETOUCH_TRANSLATOR("subtitle-retouch-translator", "NA", 1),
    SUBTITLE_PUNCTUATION_ONLY("subtitle-punctuation-only", "NA", 0),
    VOCABULARY_ASSOCIATION("vocabulary-association", "NA", 3),
    PHRASES_ASSOCIATION("phrases-association", "NA", 3),
    VOCABULARY_CHARACTER_EXPANSION("vocabulary-character-expansion", "NA", 5),
    AMBIGUOUS_ASSOCIATION_CORRECTION("ambiguous-association-correction", "NA", 1),
    SELECTION_EXPLANATION("selection-explanation", "#[SM]", 0),
    CHAT("chat", "NA", 1),
    CONVERSATION_GENERATION("conversation-generation", "NA", 0),
    TOPIC_GENERATION("topic-generation", "NA", 0);

    private final String mode;
    private final String tag;
    @Deprecated
    private final int languageWildcardCounts;

    AiPromptModeEnum(String mode, String tag, int languageWildcardCounts) {
        this.mode = mode;
        this.tag = tag;
        this.languageWildcardCounts = languageWildcardCounts;
    }

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
