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
package me.fengorz.kiwi.domain.upms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * System Menu Entity (for permissions)
 *
 * @author codingByFeng
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_menu")
@Accessors(chain = true)
public class SysMenu extends Model<SysMenu> {

    private static final long serialVersionUID = 1L;

    public static final String TYPE_MENU = "0";
    public static final String TYPE_BUTTON = "1";
    public static final String KEEP_ALIVE_YES = "0";
    public static final String KEEP_ALIVE_NO = "1";

    @TableId(type = IdType.AUTO)
    private Integer menuId;

    private String name;

    private String permission;

    private String path;

    private Integer parentId;

    private String icon;

    private String component;

    private Integer sort;

    private String keepAlive;

    private String type;

    private String delFlag;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /**
     * Transient field for menu children - not stored in database
     */
    @TableField(exist = false)
    private List<SysMenu> children = new ArrayList<>();

    @JsonIgnore
    public boolean isMenu() {
        return TYPE_MENU.equals(this.type);
    }

    @JsonIgnore
    public boolean isButton() {
        return TYPE_BUTTON.equals(this.type);
    }

    @JsonIgnore
    public boolean isRoot() {
        return this.parentId == null || this.parentId == -1;
    }

    @JsonIgnore
    public boolean shouldKeepAlive() {
        return KEEP_ALIVE_YES.equals(this.keepAlive);
    }
}
