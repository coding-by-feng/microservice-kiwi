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
package me.fengorz.kiwi.domain.notes.vo;

import lombok.Builder;
import lombok.Data;
import me.fengorz.kiwi.domain.notes.entity.NotesCategory;

import java.time.LocalDateTime;

/**
 * Notes Category Value Object
 *
 * @author codingByFeng
 */
@Data
@Builder
public class NotesCategoryVO {

    private Long id;

    private String name;

    private String description;

    private String color;

    private String icon;

    private Integer sortOrder;

    private Integer itemCount;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    public static NotesCategoryVO fromEntity(NotesCategory entity) {
        return NotesCategoryVO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .color(entity.getColor())
                .icon(entity.getIcon())
                .sortOrder(entity.getSortOrder())
                .createTime(entity.getCreateTime())
                .updateTime(entity.getUpdateTime())
                .build();
    }
}
