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
import me.fengorz.kiwi.upms.api.entity.SysMenu;
import me.fengorz.kiwi.upms.mapper.SysMenuMapper;
import me.fengorz.kiwi.upms.service.SysMenuService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 菜单权限表
 *
 * @author zhanshifeng
 * @date 2019-09-26 15:59:10
 */
@Service("sysMenuService")
public class SysMenuServiceImpl extends ServiceImpl<SysMenuMapper, SysMenu> implements SysMenuService {

    @Override
    public List<SysMenu> listMenusByRoleId(Integer roleId) {
        return baseMapper.listMenusByRoleId(roleId);
    }
}
