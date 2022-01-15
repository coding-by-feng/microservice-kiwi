/*
 * Copyright [2019~2025] [codingByFeng]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

import java.time.LocalDateTime;

/**
 * @author zhanShiFeng
 * @date 2020-09-16 16:56:42
 */
@Data
@ApiModel
@EqualsAndHashCode(callSuper = true)
@TableName("star_rel_his")
@Accessors(chain = true)
public class StarRelHisDO extends Model<StarRelHisDO> {

  private static final long serialVersionUID = 1600886602527L;

  /** */
  @TableId
  @ApiModelProperty("主键id，编辑时必须传")
  private Integer id;

  /** */
  @ApiModelProperty("")
  private String wordName;

  /** */
  @ApiModelProperty("")
  private Integer userId;

  private Integer listId;

  /** 收藏类型：1、单词；2、释义；3、例句。 */
  @ApiModelProperty("收藏类型：1、单词；2、释义；3、例句。")
  private Integer type;

  /** */
  @ApiModelProperty("")
  private LocalDateTime inTime;

  /** */
  @ApiModelProperty("")
  private Integer serialNum;

  /** 如果收藏被删除要标记为1。 */
  @ApiModelProperty("如果收藏被删除要标记为1。")
  private Integer isDel;
}
