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
package me.fengorz.kiwi.domain.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.*;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * AI Call History Entity - tracks AI API calls
 *
 * @author codingByFeng
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("ai_call_history")
@Accessors(chain = true)
public class AiCallHistory extends Model<AiCallHistory> {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String aiUrl;

    private String prompt;

    private String promptMode;

    private String targetLanguage;

    private String nativeLanguage;

    private LocalDateTime timestamp;

    private Boolean isDelete;

    private Boolean isArchive;

    private Boolean isFavorite;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    public void archive() {
        this.isArchive = true;
    }

    public void unarchive() {
        this.isArchive = false;
    }

    public void toggleFavorite() {
        this.isFavorite = !Boolean.TRUE.equals(this.isFavorite);
    }
}
