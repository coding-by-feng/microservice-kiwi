/*
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
 */
package me.fengorz.kiwi.word.api.entity;

import java.time.LocalDateTime;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

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

/**
 * 单词时态、单复数等的变化
 *
 * @Author zhanshifeng
 * @date 2020-05-24 00:24:38
 */
@Data
@ApiModel
@EqualsAndHashCode(callSuper = true)
@TableName("word_main_variant")
@Accessors(chain = true)
public class WordMainVariantDO extends Model<WordMainVariantDO> {

    private static final long serialVersionUID = 1590251078057L;

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
    private Integer wordId;

    /**
     *
     */
    @ApiModelProperty("")
    @NotBlank(groups = {ValidTypeInsert.class})
    private String variantName;

    /**
     * 类别：过去式、进行时、复数等
     */
    @ApiModelProperty("类别：过去式、进行时、复数等")
    @NotNull(groups = {ValidTypeInsert.class})
    private Integer type;

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

    /**
     *
     */
    @ApiModelProperty("")
    private String remark;

}
