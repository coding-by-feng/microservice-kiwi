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

/**
 * @Description TODO
 * @Author zhanshifeng
 * @Date 2022/7/12 09:20
 */
public enum ReviewPermanentAudioEnum {

    TEST(404, ReviewAudioTypeEnum.PARAPHRASE_EN.getType(), "A, B, C, D"),
    WELL_DONE(0, ReviewAudioTypeEnum.PARAPHRASE_EN.getType(), "Very well, you've done your job for today!"),
    WORD_CHARACTER(1, ReviewAudioTypeEnum.PARAPHRASE_CH.getType(), "单词的词性是："),
    WORD_CHARACTER_ADJECTIVE(2, ReviewAudioTypeEnum.PARAPHRASE_CH.getType(), "形容词"),
    WORD_CHARACTER_ADJ(3, ReviewAudioTypeEnum.PARAPHRASE_CH.getType(), "形容词"),
    WORD_CHARACTER_NOUN(4, ReviewAudioTypeEnum.PARAPHRASE_CH.getType(), "名词"),
    WORD_CHARACTER_ADVERB(5, ReviewAudioTypeEnum.PARAPHRASE_CH.getType(), "副词"),
    WORD_CHARACTER_VERB(6, ReviewAudioTypeEnum.PARAPHRASE_CH.getType(), "动词"),
    WORD_CHARACTER_CONJUNCTION(7, ReviewAudioTypeEnum.PARAPHRASE_CH.getType(), "连词"),
    WORD_CHARACTER_PLURAL(8, ReviewAudioTypeEnum.PARAPHRASE_CH.getType(), "名词负数形式"),
    WORD_CHARACTER_PREPOSITION(9, ReviewAudioTypeEnum.PARAPHRASE_CH.getType(), "介词或者前置词"),
    WORD_CHARACTER_PHRASE(10, ReviewAudioTypeEnum.PARAPHRASE_CH.getType(), "短语"),
    WORD_SPELLING_FIRST(11, ReviewAudioTypeEnum.PARAPHRASE_CH.getType(), "单词的拼写是："),
    WORD_SPELLING_AGAIN(12, ReviewAudioTypeEnum.PARAPHRASE_CH.getType(), "再读一次拼写："),
    WORD_CHINESE_PARAPHRASE(13, ReviewAudioTypeEnum.PARAPHRASE_CH.getType(), "中文释义是："),
    WORD_ENGLISH_PARAPHRASE(14, ReviewAudioTypeEnum.PARAPHRASE_CH.getType(), "英文释义是："),
    WORD_CHINESE_PARAPHRASE_AGAIN(15, ReviewAudioTypeEnum.PARAPHRASE_CH.getType(), "再读一次中文释义："),
    WORD_ENGLISH_PARAPHRASE_AGAIN(16, ReviewAudioTypeEnum.PARAPHRASE_CH.getType(), "再读一遍英文释义："),
    WORD_EXAMPLE(17, ReviewAudioTypeEnum.PARAPHRASE_CH.getType(), "播报单词的例句："),
    NEXT_REVIEW_CHINESE_PARAPHRASE(18, ReviewAudioTypeEnum.PARAPHRASE_CH.getType(), "接下来复习的单词中文释义是："),
    READ_CHINESE_PARAPHRASE_AGAIN(19, ReviewAudioTypeEnum.PARAPHRASE_CH.getType(), "再读一遍中文释义是："),
    RECALL_WORDS_IN_MIND(20, ReviewAudioTypeEnum.PARAPHRASE_CH.getType(), "请在脑海回想对应的单词："),
    CORRESPONDING_ENGLISH_WORD_IS(21, ReviewAudioTypeEnum.PARAPHRASE_CH.getType(), "对应的英文单词是："),
    WORD_PARAPHRASE_MISSING(22, ReviewAudioTypeEnum.PARAPHRASE_CH.getType(), "中文释义缺失！"),
    ;

    //     .set('adjective', '形容词')
    // .set('adj', '形容词')
    // .set('noun', '名词')
    // .set('verb', '动词')
    // .set('adverb', '副词')
    // .set('conjunction', '连词')
    // .set('plural', '名词复数形式')
    // .set('preposition', '介词或者前置词')
    // .set('phrase', '短语')


    @Getter
    private final Integer sourceId;
    /**
     * {@link  ReviewAudioTypeEnum}
     */
    @Getter
    private final Integer type;
    /**
     * audio播放的原文本，可以是中文或者英文
     */
    @Getter
    private final String text;

    ReviewPermanentAudioEnum(Integer sourceId, Integer type, String text) {
        this.sourceId = sourceId;
        this.type = type;
        this.text = text;
    }
}
