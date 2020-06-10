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
package me.fengorz.kiwi.admin.service.impl;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import me.fengorz.kiwi.admin.api.dto.UserFullInfoDTO;
import me.fengorz.kiwi.admin.api.entity.SysMenu;
import me.fengorz.kiwi.admin.api.entity.SysRole;
import me.fengorz.kiwi.admin.api.entity.SysUser;
import me.fengorz.kiwi.admin.mapper.SysUserMapper;
import me.fengorz.kiwi.admin.service.SysMenuService;
import me.fengorz.kiwi.admin.service.SysRoleService;
import me.fengorz.kiwi.admin.service.SysUserRoleRelService;
import me.fengorz.kiwi.admin.service.SysUserService;
import me.fengorz.kiwi.bdf.core.service.ISeqService;
import me.fengorz.kiwi.common.api.constant.MapperConstant;

/**
 * 用户表
 *
 * @author zhanshifeng
 * @date 2019-09-26 09:37:54
 */
@Service()
@RequiredArgsConstructor
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    private final SysRoleService sysRoleService;
    private final SysUserRoleRelService sysUserRoleRelService;
    private final SysMenuService sysMenuService;
    private final ISeqService seqService;

    @Override
    public UserFullInfoDTO getUserFullInfo(SysUser sysUser) {
        UserFullInfoDTO userFullInfoDTO = new UserFullInfoDTO();
        userFullInfoDTO.setSysUser(sysUser);

        List<Integer> roleIdList = sysRoleService.listRolesByUserId(sysUser.getUserId()).stream()
            .map(SysRole::getRoleId).collect(Collectors.toList());
        userFullInfoDTO.setRoles(ArrayUtil.toArray(roleIdList, Integer.class));

        Set<String> permissionSet = new HashSet<>();
        roleIdList.forEach(roleId -> {
            List<String> permissionList = sysMenuService.listMenusByRoleId(roleId).stream()
                .filter(sysMenu -> StrUtil.isNotBlank(sysMenu.getPermission())).map(SysMenu::getPermission)
                .collect(Collectors.toList());
            permissionSet.addAll(permissionList);
        });
        userFullInfoDTO.setPermissions(ArrayUtil.toArray(permissionSet, String.class));

        return userFullInfoDTO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysUser oneClickRegister() {
        SysUser sysUser = new SysUser();
        sysUser.setUserId(seqService.genIntSequence(MapperConstant.T_INS_SEQUENCE));
        sysUser.setUsername(this.randomUserName());
        sysUser.setCreateTime(LocalDateTime.now());
        sysUser.setPassword(new BCryptPasswordEncoder().encode("123456"));
        this.save(sysUser);

        return new SysUser().setUsername(sysUser.getUsername()).setPassword("123456");
    }

    private String randomUserName() {
        String userName = null;
        do {
            userName = RandomUtil.randomString(5);
        } while (this.getOne(Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, userName)) != null);
        return userName;
    }
}
