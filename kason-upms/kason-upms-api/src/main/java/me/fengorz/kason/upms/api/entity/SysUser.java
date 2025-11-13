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
package me.fengorz.kason.upms.api.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 用户表
 *
 * @author zhanshifeng
 * @date 2019-09-26 09:37:54
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
@Accessors(chain = true)
public class SysUser extends Model<SysUser> {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId
    private Integer userId;
    /**
     * 用户名
     */
    private String username;
    /**
     *
     */
    private String password;
    /**
     * 随机盐
     */
    private String salt;
    /**
     * 简介
     */
    private String phone;
    /**
     * 头像
     */
    private String avatar;
    /**
     * 部门ID
     */
    private Integer deptId;
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    /**
     * 修改时间
     */
    private LocalDateTime updateTime;
    /**
     * 0-正常，9-锁定
     */
    private Integer lockFlag;
    /**
     * 0-正常，1-删除
     */
    private Integer delFlag;
    /**
     * 微信openid
     */
    private String wxOpenid;
    /**
     * QQ openid
     */
    private String qqOpenid;
    /**
     * Google openid
     */
    private String googleOpenid;
    /**
     * 邮箱地址
     */
    private String email;
    /**
     * 用户真实姓名
     */
    private String realName;
    /**
     * 注册来源：local, google, wechat, qq
     */
    private String registerSource;
}