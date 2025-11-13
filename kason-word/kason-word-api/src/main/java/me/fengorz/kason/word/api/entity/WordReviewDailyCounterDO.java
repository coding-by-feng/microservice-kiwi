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
package me.fengorz.kason.word.api.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDate;

/**
 * 单词复习计数器
 *
 * @author zhanShiFeng
 * @date 2021-08-19 20:42:11
 */
@Data
@ApiModel
@EqualsAndHashCode(callSuper = true)
@TableName("word_review_daily_counter")
@Accessors(chain = true)
public class WordReviewDailyCounterDO extends Model<WordReviewDailyCounterDO> {

    private static final long serialVersionUID = -2091996395640104712L;

    /**
     *
     */
    @TableId
    @ApiModelProperty("主键id，编辑时必须传")
    private Integer id;

    /**
     * 1：remember 2：keep in mind 3：review
     */
    @ApiModelProperty("")
    private Integer type;

    /**
     *
     */
    @ApiModelProperty("")
    private Integer userId;

    /**
     *
     */
    @ApiModelProperty("")
    private LocalDate today;

    /**
     *
     */
    @ApiModelProperty("")
    private Integer reviewCount;
}
