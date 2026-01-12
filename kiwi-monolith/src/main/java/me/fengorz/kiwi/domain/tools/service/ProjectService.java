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
package me.fengorz.kiwi.domain.tools.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.domain.tools.entity.Project;
import me.fengorz.kiwi.domain.tools.mapper.ProjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Project Service - manages project operations
 *
 * @author codingByFeng
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class ProjectService extends ServiceImpl<ProjectMapper, Project> {

    /**
     * Find project by ID
     */
    public Optional<Project> findById(String id) {
        return Optional.ofNullable(getById(id));
    }

    /**
     * Check if project exists by ID
     */
    public boolean existsById(String id) {
        return count(new LambdaQueryWrapper<Project>()
                .eq(Project::getId, id)) > 0;
    }

    /**
     * Find projects by archived status with pagination
     */
    public Page<Project> findByArchived(Boolean archived, Page<Project> page) {
        return page(page, new LambdaQueryWrapper<Project>()
                .eq(Project::getArchived, archived));
    }

    /**
     * Find all projects with pagination
     */
    public Page<Project> findAllProjects(Page<Project> page) {
        return page(page);
    }

    /**
     * Save project
     */
    @Transactional
    public Project saveProject(Project project) {
        saveOrUpdate(project);
        return project;
    }

    /**
     * Delete project by ID
     */
    @Transactional
    public void deleteProjectById(String id) {
        removeById(id);
    }
}
