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
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import me.fengorz.kiwi.common.api.valid.ValidTypeInsert;
import me.fengorz.kiwi.common.api.valid.ValidTypeUpdate;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * @author zhanShiFeng
 * @date 2020-05-31 22:22:22
 */
@Data
@ApiModel
@EqualsAndHashCode(callSuper = true)
@TableName("word_paraphrase_phrase")
@Accessors(chain = true)
public class ParaphrasePhraseDO extends Model<ParaphrasePhraseDO> {

    private static final long serialVersionUID = 1590934942724L;

    /**
     *
     */
    @TableId
    @ApiModelProperty("主键id，编辑时必须传")
    @NotNull(groups = {ValidTypeUpdate.class})
    private Integer id;

    /**
     *
     */
    @ApiModelProperty("")
    @NotNull(groups = {ValidTypeInsert.class})
    private Integer paraphraseId;

    /**
     *
     */
    @ApiModelProperty("")
    @NotBlank(groups = {ValidTypeInsert.class})
    private String phrase;

    /**
     *
     */
    @ApiModelProperty("")
    private LocalDateTime createTime;

    /**
     *
     */
    @ApiModelProperty("")
    private Integer isValid;
}
