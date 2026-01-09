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
package me.fengorz.kiwi.domain.word.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import me.fengorz.kiwi.common.constant.GlobalConstants;

/**
 * Word Character Entity - stores part of speech information
 *
 * @author codingByFeng
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("word_character")
@Accessors(chain = true)
public class WordCharacter extends Model<WordCharacter> {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Integer characterId;

    private Integer wordId;

    private String characterCode;

    private String tag;

    @TableLogic(value = "N", delval = "Y")
    private String isDel;

    @JsonIgnore
    public boolean isDeleted() {
        return GlobalConstants.FLAG_Y.equals(this.isDel);
    }
}
