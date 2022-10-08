/*
 *
 *   Copyright [2019~2025] [codingByFeng]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 *
 */

package me.fengorz.kiwi.word.api.common.enumeration;

import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Description TODO
 * @Author zhanshifeng
 * @Date 2022/7/12 09:20
 */
public enum RevisePermanentAudioEnum {

    TEST(404, ReviseAudioTypeEnum.PARAPHRASE_EN.getType(), "A, B, C, D"),
    WORD_CHARACTER(-1, ReviseAudioTypeEnum.CHARACTER_CH.getType(), "单词的词性是："),
    WORD_CHARACTER_EMPTY(-2, ReviseAudioTypeEnum.CHARACTER_CH.getType(), "词性丢失"),
    WORD_CHARACTER_ADJECTIVE(2, ReviseAudioTypeEnum.CHARACTER_CH.getType(), "形容词"),
    WORD_CHARACTER_ADJ(3, ReviseAudioTypeEnum.CHARACTER_CH.getType(), "形容词"),
    WORD_CHARACTER_NOUN(4, ReviseAudioTypeEnum.CHARACTER_CH.getType(), "名词"),
    WORD_CHARACTER_ADVERB(5, ReviseAudioTypeEnum.CHARACTER_CH.getType(), "副词"),
    WORD_CHARACTER_VERB(6, ReviseAudioTypeEnum.CHARACTER_CH.getType(), "动词"),
    WORD_CHARACTER_CONJUNCTION(7, ReviseAudioTypeEnum.CHARACTER_CH.getType(), "连词"),
    WORD_CHARACTER_PLURAL(8, ReviseAudioTypeEnum.CHARACTER_CH.getType(), "名词负数形式"),
    WORD_CHARACTER_PREPOSITION(9, ReviseAudioTypeEnum.CHARACTER_CH.getType(), "介词或者前置词"),
    WORD_CHARACTER_PHRASE(10, ReviseAudioTypeEnum.CHARACTER_CH.getType(), "短语"),
    WORD_CHARACTER_PHRASAL_VERB(23, ReviseAudioTypeEnum.CHARACTER_CH.getType(), "动词短语"),
    WORD_CHARACTER_SUFFIX(24, ReviseAudioTypeEnum.CHARACTER_CH.getType(), "后缀"),
    WORD_CHARACTER_PHRASAL(25, ReviseAudioTypeEnum.CHARACTER_CH.getType(), "短语的"),
    WORD_CHARACTER_EXCLAMATION(26, ReviseAudioTypeEnum.CHARACTER_CH.getType(), "语气助词，呼喊，惊叫"),
    WORD_CHARACTER_PREFIX(27, ReviseAudioTypeEnum.CHARACTER_CH.getType(), "前缀"),
    WORD_CHARACTER_DETERMINER(28, ReviseAudioTypeEnum.CHARACTER_CH.getType(), "限定词"),
    WORD_CHARACTER_PREDETERMINER(29, ReviseAudioTypeEnum.CHARACTER_CH.getType(), "前位限定词"),
    WORD_CHARACTER_PRONOUN(30, ReviseAudioTypeEnum.CHARACTER_CH.getType(), "代词"),
    WORD_CHARACTER_AUXILIARY(31, ReviseAudioTypeEnum.CHARACTER_CH.getType(), "助动词"),
    WORD_SPELLING_FIRST(11, ReviseAudioTypeEnum.WORD_SPELLING.getType(), "单词的拼写是："),
    WORD_SPELLING_AGAIN(12, ReviseAudioTypeEnum.WORD_SPELLING.getType(), "再读一次拼写："),
    WORD_CHINESE_PARAPHRASE(13, ReviseAudioTypeEnum.PARAPHRASE_CH.getType(), "中文释义是："),
    WORD_ENGLISH_PARAPHRASE(14, ReviseAudioTypeEnum.PARAPHRASE_CH.getType(), "英文释义是："),
    WORD_CHINESE_PARAPHRASE_AGAIN(15, ReviseAudioTypeEnum.PARAPHRASE_CH.getType(), "再读一次中文释义："),
    WORD_ENGLISH_PARAPHRASE_AGAIN(16, ReviseAudioTypeEnum.PARAPHRASE_CH.getType(), "再读一遍英文释义："),
    WORD_EXAMPLE(17, ReviseAudioTypeEnum.EXAMPLE_CH.getType(), "播报单词的例句："),
    NEXT_REVIEW_CHINESE_PARAPHRASE(18, ReviseAudioTypeEnum.PARAPHRASE_CH.getType(), "接下来复习的单词中文释义是："),
    READ_CHINESE_PARAPHRASE_AGAIN(19, ReviseAudioTypeEnum.PARAPHRASE_CH.getType(), "再读一遍中文释义是："),
    RECALL_WORDS_IN_MIND(20, ReviseAudioTypeEnum.PRONUNCIATION.getType(), "请在脑海回想对应的单词："),
    CORRESPONDING_ENGLISH_WORD_IS(21, ReviseAudioTypeEnum.PRONUNCIATION.getType(), "对应的英文单词是："),
    WORD_PARAPHRASE_MISSING(22, ReviseAudioTypeEnum.PARAPHRASE_CH.getType(), "中文释义缺失！"),
    ;

    @Getter
    private final Integer sourceId;
    /**
     * {@link  ReviseAudioTypeEnum}
     */
    @Getter
    private final Integer type;
    /**
     * audio播放的原文本，可以是中文或者英文
     */
    @Getter
    private final String text;

    RevisePermanentAudioEnum(Integer sourceId, Integer type, String text) {
        this.sourceId = sourceId;
        this.type = type;
        this.text = text;
    }

    private static final Map<Integer, RevisePermanentAudioEnum> SOURCE_ID_MAP;

    static {
        SOURCE_ID_MAP = Arrays.stream(values()).collect(Collectors.toMap(RevisePermanentAudioEnum::getSourceId, typeEnum -> typeEnum));
    }

    public static boolean isPermanent(Integer sourceId) {
        return SOURCE_ID_MAP.containsKey(sourceId);
    }

    public static RevisePermanentAudioEnum fromSourceId(Integer sourceId) {
        return SOURCE_ID_MAP.get(sourceId);
    }

}
