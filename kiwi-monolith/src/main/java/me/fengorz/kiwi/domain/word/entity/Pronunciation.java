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

/**
 * Pronunciation Entity - stores word pronunciation information
 *
 * @author codingByFeng
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("word_pronunciation")
@Accessors(chain = true)
public class Pronunciation extends Model<Pronunciation> {

    private static final long serialVersionUID = 1L;

    public static final String TYPE_UK = "UK";
    public static final String TYPE_US = "US";

    @TableId(type = IdType.AUTO)
    private Integer pronunciationId;

    private Integer wordId;

    private String soundmark;

    private String soundmarkType;

    private String voiceFilePath;

    private String groupName;

    private Integer characterId;

    private String sourceUrl;

    @TableLogic(value = "N", delval = "Y")
    private String isDel;

    @JsonIgnore
    public boolean isUkPronunciation() {
        return TYPE_UK.equalsIgnoreCase(this.soundmarkType);
    }

    @JsonIgnore
    public boolean isUsPronunciation() {
        return TYPE_US.equalsIgnoreCase(this.soundmarkType);
    }

    @JsonIgnore
    public String getFullFilePath() {
        if (groupName != null && voiceFilePath != null) {
            return groupName + "/" + voiceFilePath;
        }
        return voiceFilePath;
    }
}
