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

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 单词本与单词的关联表
 *
 * @author codingByFeng
 * @date 2020-01-04 21:04:36
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("word_star_rel")
@Accessors(chain = true)
public class WordStarRelDO extends Model<WordStarRelDO> {

    private static final long serialVersionUID = 1L;

    /**
     *
     */
    private Integer listId;
    /**
     *
     */
    private Integer wordId;
    /**
     *
     */
    private LocalDateTime createTime;

}
