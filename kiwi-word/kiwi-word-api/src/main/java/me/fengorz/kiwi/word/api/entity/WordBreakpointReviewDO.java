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
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import me.fengorz.kiwi.common.api.valid.ValidTypeInsert;
import me.fengorz.kiwi.common.api.valid.ValidTypeUpdate;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 断点复习记录表
 *
 * @author zhanShiFeng
 * @date 2021-06-06 14:53:44
 */
@Data
@ApiModel
@EqualsAndHashCode(callSuper = true)
@TableName("word_breakpoint_review")
@Accessors(chain = true)
public class WordBreakpointReviewDO extends Model<WordBreakpointReviewDO> {

    private static final long serialVersionUID = 1622962424639L;

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
    private Integer listId;

    /**
     *
     */
    @ApiModelProperty("")
    @NotNull(groups = {ValidTypeInsert.class})
    private Integer userId;

    /**
     *
     */
    @ApiModelProperty("")
    private Integer type;

    /**
     *
     */
    @ApiModelProperty("")
    @NotNull(groups = {ValidTypeInsert.class})
    private Integer lastPage;

    /**
     *
     */
    @ApiModelProperty("")
    private LocalDateTime createTime;

}
