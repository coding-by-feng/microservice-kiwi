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
package me.fengorz.kiwi.upms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import me.fengorz.kiwi.upms.api.dto.UserFullInfoDTO;
import me.fengorz.kiwi.upms.api.entity.SysUser;

/**
 * 用户表
 *
 * @author zhanshifeng
 * @date 2019-09-26 09:37:54
 */
public interface SysUserService extends IService<SysUser> {

    /**
     * 查询用户全部的信息，包含权限和角色信息
     *
     * @param sysUser
     * @return
     */
    UserFullInfoDTO getUserFullInfo(SysUser sysUser);

    SysUser oneClickRegister();
}
