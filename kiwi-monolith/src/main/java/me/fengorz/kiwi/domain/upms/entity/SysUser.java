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
 * System User Entity
 *
 * @author codingByFeng
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
@Accessors(chain = true)
public class SysUser extends Model<SysUser> {

    private static final long serialVersionUID = 1L;

    public static final Integer LOCK_FLAG_NORMAL = 0;
    public static final Integer LOCK_FLAG_LOCKED = 9;

    @TableId(type = IdType.AUTO)
    private Integer userId;

    private String username;

    private String password;

    private String salt;

    private String phone;

    private String avatar;

    private Integer deptId;

    private Integer lockFlag;

    private Integer delFlag;

    private String wxOpenid;

    private String qqOpenid;

    private String googleOpenid;

    private String email;

    private String realName;

    private String registerSource;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /**
     * Transient field for roles - not stored in database
     */
    @TableField(exist = false)
    private List<SysRole> roles = new ArrayList<>();

    @JsonIgnore
    public boolean isLocked() {
        return LOCK_FLAG_LOCKED.equals(this.lockFlag);
    }

    public void lock() {
        this.lockFlag = LOCK_FLAG_LOCKED;
    }

    public void unlock() {
        this.lockFlag = LOCK_FLAG_NORMAL;
    }
}
