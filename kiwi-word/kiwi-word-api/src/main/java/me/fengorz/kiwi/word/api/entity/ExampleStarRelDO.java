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

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 释义例句本与例句关系表
 *
 * @author zhanshifeng
 * @date 2020-01-03 14:48:48
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("word_example_star_rel")
@Accessors(chain = true)
public class ExampleStarRelDO extends Model<ExampleStarRelDO> {

    private static final long serialVersionUID = 1L;

    /**
     *
     */
    private Integer listId;
    /**
     *
     */
    private Integer exampleId;
    /**
     *
     */
    private LocalDateTime createTime;

    private Integer isRemember;

    private LocalDateTime rememberTime;
}
