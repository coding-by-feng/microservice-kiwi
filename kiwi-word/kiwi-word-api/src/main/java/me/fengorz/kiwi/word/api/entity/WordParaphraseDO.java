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
package me.fengorz.kiwi.word.api.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 单词释义表
 *
 * @author codingByFeng
 * @date 2019-11-04 16:13:24
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("word_paraphrase")
@Accessors(chain = true)
public class WordParaphraseDO extends Model<WordParaphraseDO> {

    private static final long serialVersionUID = 1L;

    /**
     * 释义ID
     */
    @TableId
    private Integer paraphraseId;
    /**
     *
     */
    private Integer wordId;
    /**
     *
     */
    private Integer characterId;
    /**
     * 英文释义
     */
    private String paraphraseEnglish;
    /**
     * 英文释义翻译
     */
    private String paraphraseEnglishTranslate;
    /**
     * 中文词义
     */
    private String meaningChinese;
    /**
     * 翻译语种
     */
    private String translateLanguage;
    /**
     * 逻辑删除标记(Y--正常 N--删除)
     */
    private String isDel;

}
