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
import me.fengorz.kiwi.domain.upms.entity.SysMenu;
import me.fengorz.kiwi.domain.upms.mapper.SysMenuMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * SysMenu Service - manages menu and permission operations
 *
 * @author codingByFeng
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class SysMenuService extends ServiceImpl<SysMenuMapper, SysMenu> {

    private static final String CACHE_NAME = "menu";

    /**
     * Find menu by ID
     */
    public Optional<SysMenu> findById(Integer menuId) {
        return Optional.ofNullable(getById(menuId));
    }

    /**
     * Find all menus
     */
    public List<SysMenu> findAll() {
        return list();
    }

    /**
     * Find all root menus (top-level)
     */
    @Cacheable(value = CACHE_NAME, key = "'root'")
    public List<SysMenu> findRootMenus() {
        return list(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getParentId, -1)
                .orderByAsc(SysMenu::getSort));
    }

    /**
     * Find children menus by parent ID
     */
    @Cacheable(value = CACHE_NAME, key = "'children:' + #parentId")
    public List<SysMenu> findByParentId(Integer parentId) {
        return list(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getParentId, parentId)
                .orderByAsc(SysMenu::getSort));
    }

    /**
     * Find menus by type
     */
    public List<SysMenu> findByType(String type) {
        return list(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getType, type)
                .orderByAsc(SysMenu::getSort));
    }

    /**
     * Find menu by permission code
     */
    public Optional<SysMenu> findByPermission(String permission) {
        return Optional.ofNullable(getOne(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getPermission, permission)));
    }

    /**
     * Build menu tree from flat list
     */
    public List<SysMenu> buildMenuTree() {
        List<SysMenu> allMenus = findAll();
        return buildTree(allMenus);
    }

    /**
     * Build tree structure from flat menu list
     */
    private List<SysMenu> buildTree(List<SysMenu> menus) {
        Map<Integer, SysMenu> menuMap = menus.stream()
                .collect(Collectors.toMap(SysMenu::getMenuId, m -> m));

        List<SysMenu> rootMenus = new ArrayList<>();

        for (SysMenu menu : menus) {
            if (menu.isRoot()) {
                rootMenus.add(menu);
            } else {
                SysMenu parent = menuMap.get(menu.getParentId());
                if (parent != null) {
                    parent.getChildren().add(menu);
                }
            }
        }

        // Sort root menus and their children
        rootMenus.sort(Comparator.comparingInt(m -> m.getSort() != null ? m.getSort() : 0));
        sortChildrenRecursively(rootMenus);

        return rootMenus;
    }

    private void sortChildrenRecursively(List<SysMenu> menus) {
        for (SysMenu menu : menus) {
            if (menu.getChildren() != null && !menu.getChildren().isEmpty()) {
                menu.getChildren().sort(Comparator.comparingInt(m -> m.getSort() != null ? m.getSort() : 0));
                sortChildrenRecursively(menu.getChildren());
            }
        }
    }

    /**
     * Save menu
     */
    @Transactional
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public SysMenu saveMenu(SysMenu menu) {
        saveOrUpdate(menu);
        return menu;
    }

    /**
     * Delete menu by ID
     */
    @Transactional
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public void deleteById(Integer menuId) {
        removeById(menuId);
    }

    /**
     * Check if menu has children
     */
    public boolean hasChildren(Integer menuId) {
        return count(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getParentId, menuId)) > 0;
    }

    /**
     * Get all buttons (permission types) for menus
     */
    public List<SysMenu> findAllButtons() {
        return findByType(SysMenu.TYPE_BUTTON);
    }

    /**
     * Get all menu items (non-button types)
     */
    public List<SysMenu> findAllMenuItems() {
        return findByType(SysMenu.TYPE_MENU);
    }
}
