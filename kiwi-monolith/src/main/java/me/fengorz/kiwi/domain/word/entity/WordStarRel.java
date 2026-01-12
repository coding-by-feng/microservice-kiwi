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

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * Word Star Relation Entity - junction table for word star list and words
 *
 * @author codingByFeng
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("word_star_rel")
@Accessors(chain = true)
public class WordStarRel extends Model<WordStarRel> {

    private static final long serialVersionUID = 1L;

    private Integer listId;

    private Integer wordId;

    private LocalDateTime createTime;

    private Integer isRemember;

    private LocalDateTime rememberTime;

    @JsonIgnore
    public boolean isRemembered() {
        return this.isRemember != null && this.isRemember == 1;
    }

    public void markRemembered() {
        this.isRemember = 1;
        this.rememberTime = LocalDateTime.now();
    }

    public void markForgotten() {
        this.isRemember = 0;
        this.rememberTime = null;
    }
}
