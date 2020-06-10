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
 * 单词词性表
 *
 * @author zhanshifeng
 * @date 2019-10-31 20:38:37
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("word_character")
@Accessors(chain = true)
public class WordCharacterDO extends Model<WordCharacterDO> {

    private static final long serialVersionUID = 1L;

    /**
     * 词性ID
     */
    @TableId
    private Integer characterId;
    /**
     *
     */
    private Integer wordId;
    /**
     * 词性
     */
    private String wordCharacter;
    /**
     * 词性的标签
     */
    private String wordLabel;
    /**
     * 逻辑删除标记(Y--正常 N--删除)
     */
    private String isDel;

}
