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

package me.fengorz.kiwi.word.biz.enumeration;

import lombok.Getter;

/**
 * @Description TODO
 * @Author zhanshifeng
 * @Date 2022/7/12 09:20
 */
public enum ReviewAudioTypeEnum {

    WORD_SPELLING(0), PARAPHRASE_EN(1), PARAPHRASE_CH(2), EXAMPLE_EN(3), EXAMPLE_CH(4), ALL(5), CHARACTER_EN(6),
    CHARACTER_CH(7);

    @Getter
    private final Integer type;

    ReviewAudioTypeEnum(Integer type) {
        this.type = type;
    }

    public static boolean isParaphrase(int type) {
        return PARAPHRASE_EN.getType() == type || PARAPHRASE_CH.getType() == type;
    }

    public static boolean isExample(int type) {
        return EXAMPLE_EN.getType() == type || EXAMPLE_CH.getType() == type;
    }

    public static boolean isAll(int type) {
        return ALL.getType() == type;
    }

    public static boolean isEnglish(int type) {
        return PARAPHRASE_EN.getType() == type || EXAMPLE_EN.getType() == type;
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

}
