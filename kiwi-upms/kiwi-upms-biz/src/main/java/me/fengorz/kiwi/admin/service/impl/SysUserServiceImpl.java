/*
 *
 *   Copyright [2019~2025] [codingByFeng]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 *
 */
package me.fengorz.kiwi.admin.service.impl;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import me.fengorz.kiwi.admin.api.dto.UserFullInfoDTO;
import me.fengorz.kiwi.admin.api.entity.SysMenu;
import me.fengorz.kiwi.admin.api.entity.SysRole;
import me.fengorz.kiwi.admin.api.entity.SysUser;
import me.fengorz.kiwi.admin.mapper.SysUserMapper;
import me.fengorz.kiwi.admin.service.SysMenuService;
import me.fengorz.kiwi.admin.service.SysRoleService;
import me.fengorz.kiwi.admin.service.SysUserRoleRelService;
import me.fengorz.kiwi.admin.service.SysUserService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户表
 *
 * @author codingByFeng
 * @date 2019-09-26 09:37:54
 */
@Service("sysUserService")
@AllArgsConstructor
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    private final SysRoleService sysRoleService;
    private final SysUserRoleRelService sysUserRoleRelService;
    private final SysMenuService sysMenuService;

    @Override
    public UserFullInfoDTO getUserFullInfo(SysUser sysUser) {
        UserFullInfoDTO userFullInfoDTO = new UserFullInfoDTO();
        userFullInfoDTO.setSysUser(sysUser);

        List<Integer> roleIdList = sysRoleService.listRolesByUserId(sysUser.getUserId())
                .stream()
                .map(SysRole::getRoleId)
                .collect(Collectors.toList());
        userFullInfoDTO.setRoles(ArrayUtil.toArray(roleIdList, Integer.class));

        Set<String> permissionSet = new HashSet<>();
        roleIdList.forEach(roleId -> {
            List<String> permissionList = sysMenuService.listMenusByRoleId(roleId)
                    .stream()
                    .filter(sysMenu -> StrUtil.isNotBlank(sysMenu.getPermission()))
                    .map(SysMenu::getPermission)
                    .collect(Collectors.toList());
            permissionSet.addAll(permissionList);
        });
        userFullInfoDTO.setPermissions(ArrayUtil.toArray(permissionSet, String.class));

        return userFullInfoDTO;
    }
}