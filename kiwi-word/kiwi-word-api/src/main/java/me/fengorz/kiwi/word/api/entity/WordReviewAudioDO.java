/*
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
 */
package me.fengorz.kiwi.word.api.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.time.LocalDateTime;

/**
 * 单词复习音频记录
 *
 * @author zhanShiFeng
 * @date 2022-07-03 20:42:11
 */
@Data
@ApiModel
@TableName("word_review_audio")
@Accessors(chain = true)
public class WordReviewAudioDO extends Model<WordReviewAudioDO> {

    private static final long serialVersionUID = -209199639564010112L;

    /**
     *
     */
    @TableId
    @ApiModelProperty("主键id，编辑时必须传")
    private Integer id;

    private Integer sourceId;

    /**
     * 0：en, 1：ch.
     */
    private Integer type;

    private String sourceUrl;

    private String filePath;

    private String groupName;

    private String sourceText;

    private LocalDateTime createTime;

    private Integer isDel;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        WordReviewAudioDO that = (WordReviewAudioDO)o;

        return new EqualsBuilder().append(id, that.id).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(id).toHashCode();
    }
}
