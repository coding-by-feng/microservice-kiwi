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
package me.fengorz.kiwi.domain.ai.entity.conversation;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.*;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI Conversation Entity
 *
 * @author codingByFeng
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("ai_conversation")
@Accessors(chain = true)
public class Conversation extends Model<Conversation> {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String topic;

    private String prompt;

    private String accent;

    private Integer durationMinutes;

    private Integer speakerCount;

    private String status;

    private Integer totalMessages;

    private Long totalAudioDurationMs;

    @TableLogic(value = "N", delval = "Y")
    private String isDel;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Boolean favorited;

    @TableField(exist = false)
    private List<ConversationSpeaker> speakers;

    @TableField(exist = false)
    private List<ConversationMessage> messages;

    public ConversationStatus getStatusEnum() {
        return ConversationStatus.fromCode(this.status);
    }

    public void setStatusEnum(ConversationStatus status) {
        this.status = status.getCode();
    }

    public boolean isCompleted() {
        return ConversationStatus.COMPLETED.getCode().equals(this.status);
    }

    public boolean isFailed() {
        return ConversationStatus.FAILED.getCode().equals(this.status);
    }

    public void toggleFavorite() {
        this.favorited = !Boolean.TRUE.equals(this.favorited);
    }
}
