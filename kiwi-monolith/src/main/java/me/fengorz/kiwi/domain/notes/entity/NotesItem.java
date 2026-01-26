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
package me.fengorz.kiwi.domain.notes.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.*;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * Notes Item Entity
 *
 * @author codingByFeng
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("notes_item")
@Accessors(chain = true)
public class NotesItem extends Model<NotesItem> {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long categoryId;

    private Integer userId;

    private String content;

    private Integer displayOrder;

    // Image fields
    private String imagePrompt;
    private String imageStyle;
    private String imageUrl;
    private String imageStatus;

    // Audio fields
    private String audioUrl;
    private String audioAccent;
    private String audioVoice;
    private Integer audioDurationMs;
    private String audioStatus;

    @TableLogic(value = "N", delval = "Y")
    private String isDel;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    public MediaStatus getImageStatusEnum() {
        return MediaStatus.fromCode(this.imageStatus);
    }

    public void setImageStatusEnum(MediaStatus status) {
        this.imageStatus = status.getCode();
    }

    public MediaStatus getAudioStatusEnum() {
        return MediaStatus.fromCode(this.audioStatus);
    }

    public void setAudioStatusEnum(MediaStatus status) {
        this.audioStatus = status.getCode();
    }
}
