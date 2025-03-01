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
package me.fengorz.kiwi.upms.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import me.fengorz.kiwi.upms.api.entity.SysRole;
import me.fengorz.kiwi.upms.mapper.SysRoleMapper;
import me.fengorz.kiwi.upms.service.SysRoleService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 系统角色表
 *
 * @author zhanshifeng
 * @date 2019-09-26 14:21:47
 */
@Service("sysRoleService")
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements SysRoleService {

    @Override
    public List<SysRole> listRolesByUserId(Integer userId) {
        return baseMapper.listRolesByUserId(userId);
    }
}
