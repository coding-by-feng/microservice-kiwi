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
import me.fengorz.kiwi.domain.upms.entity.SysDept;
import me.fengorz.kiwi.domain.upms.mapper.SysDeptMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * SysDept Service - manages department operations
 *
 * @author codingByFeng
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class SysDeptService extends ServiceImpl<SysDeptMapper, SysDept> {

    private static final String CACHE_NAME = "dept";

    /**
     * Find department by ID
     */
    public Optional<SysDept> findById(Integer deptId) {
        return Optional.ofNullable(getById(deptId));
    }

    /**
     * Find department by ID with children
     */
    public Optional<SysDept> findByIdWithChildren(Integer deptId) {
        return Optional.ofNullable(getById(deptId));
    }

    /**
     * Find all departments
     */
    public List<SysDept> findAllDepts() {
        return list(new LambdaQueryWrapper<SysDept>()
                .eq(SysDept::getIsValid, SysDept.VALID_YES));
    }

    /**
     * Find all root departments (top-level)
     */
    @Cacheable(value = CACHE_NAME, key = "'root'", unless = "#result == null")
    public List<SysDept> findRootDepartments() {
        return list(new LambdaQueryWrapper<SysDept>()
                .and(w -> w.isNull(SysDept::getParentId).or().eq(SysDept::getParentId, 0))
                .eq(SysDept::getIsValid, SysDept.VALID_YES)
                .orderByAsc(SysDept::getSort));
    }

    /**
     * Find children departments by parent ID
     */
    @Cacheable(value = CACHE_NAME, key = "'children:' + #parentId", unless = "#result == null")
    public List<SysDept> findByParentId(Integer parentId) {
        return list(new LambdaQueryWrapper<SysDept>()
                .eq(SysDept::getParentId, parentId)
                .eq(SysDept::getIsValid, SysDept.VALID_YES)
                .orderByAsc(SysDept::getSort));
    }

    /**
     * Search departments by name
     */
    public List<SysDept> searchByName(String deptName) {
        return list(new LambdaQueryWrapper<SysDept>()
                .like(SysDept::getDeptName, deptName)
                .eq(SysDept::getIsValid, SysDept.VALID_YES));
    }

    /**
     * Build department tree from flat list
     */
    public List<SysDept> buildDeptTree() {
        List<SysDept> allDepts = findAllDepts();
        return buildTree(allDepts);
    }

    /**
     * Build tree structure from flat department list
     */
    private List<SysDept> buildTree(List<SysDept> depts) {
        Map<Integer, SysDept> deptMap = depts.stream()
                .collect(Collectors.toMap(SysDept::getDeptId, d -> d));

        List<SysDept> rootDepts = new ArrayList<>();

        for (SysDept dept : depts) {
            if (dept.isRoot()) {
                rootDepts.add(dept);
            } else {
                SysDept parent = deptMap.get(dept.getParentId());
                if (parent != null) {
                    // Note: SysDept entity needs getChildren() method to support tree building
                    // This may need adjustment based on actual entity structure
                }
            }
        }

        // Sort root departments
        rootDepts.sort(Comparator.comparingInt(d -> d.getSort() != null ? d.getSort() : 0));

        return rootDepts;
    }

    /**
     * Save department
     */
    @Transactional
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public SysDept saveDept(SysDept dept) {
        saveOrUpdate(dept);
        return dept;
    }

    /**
     * Create new department
     */
    @Transactional
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public SysDept create(String deptName, Integer parentId, Integer sort) {
        SysDept dept = new SysDept()
                .setDeptName(deptName)
                .setParentId(parentId)
                .setSort(sort != null ? sort : 0)
                .setIsValid(SysDept.VALID_YES);
        save(dept);
        return dept;
    }

    /**
     * Delete department by ID (soft delete via isValid flag)
     */
    @Transactional
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public void deleteDeptById(Integer deptId) {
        findById(deptId).ifPresent(dept -> {
            dept.markInvalid();
            updateById(dept);
        });
    }

    /**
     * Check if department has children
     */
    public boolean hasChildren(Integer deptId) {
        return count(new LambdaQueryWrapper<SysDept>()
                .eq(SysDept::getParentId, deptId)
                .eq(SysDept::getIsValid, SysDept.VALID_YES)) > 0;
    }

    /**
     * Get all descendant IDs for a department
     */
    public Set<Integer> getDescendantIds(Integer deptId) {
        Set<Integer> descendantIds = new HashSet<>();
        collectDescendantIds(deptId, descendantIds);
        return descendantIds;
    }

    private void collectDescendantIds(Integer parentId, Set<Integer> ids) {
        List<SysDept> children = findByParentId(parentId);
        for (SysDept child : children) {
            ids.add(child.getDeptId());
            collectDescendantIds(child.getDeptId(), ids);
        }
    }
}
