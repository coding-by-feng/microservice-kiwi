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
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.*;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * Word Main Variant Entity - stores word tense, plural forms, etc.
 *
 * @author codingByFeng
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("word_main_variant")
@Accessors(chain = true)
public class WordMainVariant extends Model<WordMainVariant> {

    private static final long serialVersionUID = 1L;

    public static final Integer TYPE_PAST_TENSE = 1;
    public static final Integer TYPE_PAST_PARTICIPLE = 2;
    public static final Integer TYPE_PRESENT_PARTICIPLE = 3;
    public static final Integer TYPE_THIRD_PERSON = 4;
    public static final Integer TYPE_PLURAL = 5;
    public static final Integer TYPE_COMPARATIVE = 6;
    public static final Integer TYPE_SUPERLATIVE = 7;

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer wordId;

    private String variantName;

    private Integer type;

    private LocalDateTime createTime;

    private Integer isValid;

    private String remark;
}
