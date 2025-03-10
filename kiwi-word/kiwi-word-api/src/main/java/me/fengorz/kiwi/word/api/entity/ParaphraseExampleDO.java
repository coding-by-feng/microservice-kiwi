/*
 *
 * Copyright [2019~2025] [zhanshifeng]
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
package me.fengorz.kiwi.word.api.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 单词例句表
 *
 * @author zhanshifeng
 * @date 2019-10-31 20:40:38
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("word_paraphrase_example")
@Accessors(chain = true)
public class ParaphraseExampleDO extends Model<ParaphraseExampleDO> {

    private static final long serialVersionUID = 1L;

    /**
     * 例句id
     */
    @TableId
    private Integer exampleId;
    /**
     *
     */
    private Integer wordId;
    /**
     * 释义ID
     */
    private Integer paraphraseId;
    /**
     * 英文例句
     */
    private String exampleSentence;
    /**
     * 例句翻译
     */
    private String exampleTranslate;
    /**
     * 翻译语种
     */
    private String translateLanguage;

    private Integer serialNumber;
    /**
     * 逻辑删除标记(Y--正常 N--删除)
     */
    private String isDel;
}
