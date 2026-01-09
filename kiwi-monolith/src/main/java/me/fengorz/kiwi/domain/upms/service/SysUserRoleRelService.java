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
package me.fengorz.kiwi.domain.upms.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.domain.upms.entity.SysUserRoleRel;
import me.fengorz.kiwi.domain.upms.mapper.SysUserRoleRelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * SysUserRoleRel Service - manages user-role relationships
 *
 * @author codingByFeng
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class SysUserRoleRelService extends ServiceImpl<SysUserRoleRelMapper, SysUserRoleRel> {

    /**
     * Find role IDs by user ID
     */
    public List<Integer> findRoleIdsByUserId(Integer userId) {
        return list(new LambdaQueryWrapper<SysUserRoleRel>()
                .eq(SysUserRoleRel::getUserId, userId))
                .stream()
                .map(SysUserRoleRel::getRoleId)
                .collect(Collectors.toList());
    }

    /**
     * Find user IDs by role ID
     */
    public List<Integer> findUserIdsByRoleId(Integer roleId) {
        return list(new LambdaQueryWrapper<SysUserRoleRel>()
                .eq(SysUserRoleRel::getRoleId, roleId))
                .stream()
                .map(SysUserRoleRel::getUserId)
                .collect(Collectors.toList());
    }

    /**
     * Check if user has role
     */
    public boolean existsByUserIdAndRoleId(Integer userId, Integer roleId) {
        return count(new LambdaQueryWrapper<SysUserRoleRel>()
                .eq(SysUserRoleRel::getUserId, userId)
                .eq(SysUserRoleRel::getRoleId, roleId)) > 0;
    }

    /**
     * Add role to user
     */
    @Transactional
    public void addRoleToUser(Integer userId, Integer roleId) {
        if (!existsByUserIdAndRoleId(userId, roleId)) {
            save(SysUserRoleRel.builder()
                    .userId(userId)
                    .roleId(roleId)
                    .build());
        }
    }

    /**
     * Remove role from user
     */
    @Transactional
    public void removeRoleFromUser(Integer userId, Integer roleId) {
        remove(new LambdaQueryWrapper<SysUserRoleRel>()
                .eq(SysUserRoleRel::getUserId, userId)
                .eq(SysUserRoleRel::getRoleId, roleId));
    }

    /**
     * Remove all roles from user
     */
    @Transactional
    public void removeAllRolesFromUser(Integer userId) {
        remove(new LambdaQueryWrapper<SysUserRoleRel>()
                .eq(SysUserRoleRel::getUserId, userId));
    }

    /**
     * Assign roles to user (replaces existing)
     */
    @Transactional
    public void assignRolesToUser(Integer userId, List<Integer> roleIds) {
        removeAllRolesFromUser(userId);
        for (Integer roleId : roleIds) {
            save(SysUserRoleRel.builder()
                    .userId(userId)
                    .roleId(roleId)
                    .build());
        }
    }
}
