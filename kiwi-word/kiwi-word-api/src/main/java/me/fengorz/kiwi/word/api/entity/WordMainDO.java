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
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 单词主表
 *
 * @author zhanshifeng
 * @date 2019-10-31 20:32:07
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("word_main")
@Accessors(chain = true)
public class WordMainDO extends Model<WordMainDO> {

    private static final long serialVersionUID = 1L;

    /**
     * 单词ID
     */
    @TableId
    private Integer wordId;
    /**
     * 单词名称
     */
    @JsonProperty("value")
    private String wordName;

    private Integer infoType;
    /**
     * 入库时间
     */
    private LocalDateTime inTime;
    /**
     * 更新时间
     */
    private LocalDateTime lastUpdateTime;
    /**
     * 逻辑删除标记(Y--正常 N--删除)
     */
    private Integer isDel;

}
