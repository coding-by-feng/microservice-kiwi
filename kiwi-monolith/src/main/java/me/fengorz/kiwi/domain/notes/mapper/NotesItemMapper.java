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
package me.fengorz.kiwi.domain.notes.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import me.fengorz.kiwi.domain.notes.entity.NotesItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * Notes Item Mapper
 *
 * @author codingByFeng
 */
@Mapper
public interface NotesItemMapper extends BaseMapper<NotesItem> {

    @Select("SELECT * FROM notes_item WHERE category_id = #{categoryId} " +
            "AND display_order > #{currentOrder} AND is_del = 'N' " +
            "ORDER BY display_order ASC LIMIT 1")
    NotesItem findNextItem(@Param("categoryId") Long categoryId,
                           @Param("currentOrder") Integer currentOrder);

    @Select("SELECT * FROM notes_item WHERE category_id = #{categoryId} " +
            "AND display_order < #{currentOrder} AND is_del = 'N' " +
            "ORDER BY display_order DESC LIMIT 1")
    NotesItem findPreviousItem(@Param("categoryId") Long categoryId,
                               @Param("currentOrder") Integer currentOrder);

    @Select("SELECT COALESCE(MAX(display_order), 0) FROM notes_item " +
            "WHERE category_id = #{categoryId} AND is_del = 'N'")
    Integer findMaxDisplayOrder(@Param("categoryId") Long categoryId);

    @Select("SELECT * FROM notes_item WHERE category_id = #{categoryId} " +
            "AND is_del = 'N' ORDER BY display_order ASC LIMIT 1")
    NotesItem findFirstItem(@Param("categoryId") Long categoryId);

    @Select("SELECT * FROM notes_item WHERE category_id = #{categoryId} " +
            "AND is_del = 'N' ORDER BY display_order DESC LIMIT 1")
    NotesItem findLastItem(@Param("categoryId") Long categoryId);
}
