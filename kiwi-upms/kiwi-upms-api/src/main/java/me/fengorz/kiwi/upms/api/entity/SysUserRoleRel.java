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
package me.fengorz.kiwi.upms.api.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户角色表
 *
 * @author zhanshifeng
 * @date 2019-09-26 14:39:35
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user_role_rel")
public class SysUserRoleRel extends Model<SysUserRoleRel> {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    @TableId
    private Integer userId;
    /**
     * 角色ID
     */
    private Integer roleId;
}
