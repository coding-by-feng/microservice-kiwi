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
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.*;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * Notes User Lock Entity - Stores passcode and lock status for notes feature
 *
 * @author codingByFeng
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("notes_user_lock")
@Accessors(chain = true)
public class NotesUserLock extends Model<NotesUserLock> {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.INPUT)
    private Integer userId;

    private String passcodeHash;

    private Boolean isLocked;

    private LocalDateTime lockTime;

    private Integer failedAttempts;

    private LocalDateTime lastFailedTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
