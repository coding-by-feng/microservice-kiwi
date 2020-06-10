/*
 *
 * Copyright [2019~2025] [zhanshifeng]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 *
 */
package me.fengorz.kiwi.admin.api.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色菜单表
 *
 * @author zhanshifeng
 * @date 2019-09-26 16:03:15
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role_menu_rel")
public class SysRoleMenuRel extends Model<SysRoleMenuRel> {

    private static final long serialVersionUID = 1L;

    /**
     * 角色ID
     */
    @TableId
    private Integer roleId;
    /**
     * 菜单ID
     */
    private Integer menuId;

}
