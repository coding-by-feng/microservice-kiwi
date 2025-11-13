/*
 *
 * Copyright [2019~2025] [codingByFeng]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 *
 */

package me.fengorz.kason.word.api.common.enumeration;

import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Description TODO
 * @Author Kason Zhan
 * @Date 2022/7/12 09:20
 */
public enum ReviseAudioTypeEnum {

    WORD_SPELLING(0), PARAPHRASE_EN(1), PARAPHRASE_CH(2), EXAMPLE_EN(3), EXAMPLE_CH(4), COMBO(5), CHARACTER_EN(6),
    CHARACTER_CH(7), NON_REVIEW_SPELL(8), PRONUNCIATION(9), PHRASE_PRONUNCIATION(10);

    @Getter
    private final Integer type;

    ReviseAudioTypeEnum(Integer type) {
        this.type = type;
    }

    private static final Map<Integer, ReviseAudioTypeEnum> VALUES_MAP;
    static {
        VALUES_MAP =
            Arrays.stream(values()).collect(Collectors.toMap(ReviseAudioTypeEnum::getType, typeEnum -> typeEnum));
    }

    public static boolean isParaphrase(int type) {
        return PARAPHRASE_EN.getType() == type || PARAPHRASE_CH.getType() == type;
    }

    public static boolean isWord(int type) {
        return NON_REVIEW_SPELL.getType() == type || WORD_SPELLING.getType() == type || PHRASE_PRONUNCIATION.getType() == type;
    }

    public static boolean isExample(int type) {
        return EXAMPLE_EN.getType() == type || EXAMPLE_CH.getType() == type;
    }

    public static boolean isAll(int type) {
        return COMBO.getType() == type;
    }

    public static boolean isEnglish(int type) {
        return PARAPHRASE_EN.getType() == type || EXAMPLE_EN.getType() == type || NON_REVIEW_SPELL.getType() == type;
    }

    public static boolean isChinese(int type) {
        return PARAPHRASE_CH.getType() == type || EXAMPLE_CH.getType() == type;
    }

    public static boolean isSpelling(int type) {
        return WORD_SPELLING.getType() == type;
    }

    public static boolean isCharacter(int type) {
        return CHARACTER_EN.getType() == type || CHARACTER_CH.getType() == type;
    }

    public static ReviseAudioTypeEnum fromValue(int type) {
        return VALUES_MAP.get(type);
    }

}
