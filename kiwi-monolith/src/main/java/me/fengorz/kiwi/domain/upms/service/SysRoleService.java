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
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.domain.upms.entity.SysRole;
import me.fengorz.kiwi.domain.upms.mapper.SysRoleMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * SysRole Service
 *
 * @author codingByFeng
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class SysRoleService extends ServiceImpl<SysRoleMapper, SysRole> {

    /**
     * Find role by ID
     */
    public Optional<SysRole> findById(Integer roleId) {
        return Optional.ofNullable(getById(roleId));
    }

    /**
     * Find role by code
     */
    public Optional<SysRole> findByRoleCode(String roleCode) {
        return Optional.ofNullable(getOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleCode, roleCode)));
    }

    /**
     * Find role with menus (simplified - menus are fetched separately in MyBatis Plus)
     */
    public Optional<SysRole> findByIdWithMenus(Integer roleId) {
        return findById(roleId);
    }

    /**
     * Find all roles
     */
    public List<SysRole> findAll() {
        return list();
    }

    /**
     * Find all roles with pagination
     */
    public Page<SysRole> findAllRoles(Page<SysRole> page) {
        return page(page);
    }

    /**
     * Find roles by codes
     */
    public List<SysRole> findByRoleCodes(List<String> roleCodes) {
        return list(new LambdaQueryWrapper<SysRole>()
                .in(SysRole::getRoleCode, roleCodes));
    }

    /**
     * Check if role code exists
     */
    public boolean existsByRoleCode(String roleCode) {
        return count(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleCode, roleCode)) > 0;
    }

    /**
     * Save role
     */
    @Transactional
    public SysRole saveRole(SysRole role) {
        saveOrUpdate(role);
        return role;
    }

    /**
     * Delete role
     */
    @Transactional
    public void deleteById(Integer roleId) {
        removeById(roleId);
    }
}
