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
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.domain.tools.entity.ProjectPhoto;
import me.fengorz.kiwi.domain.tools.mapper.ProjectPhotoMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * ProjectPhoto Service - manages project photo operations
 *
 * @author codingByFeng
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class ProjectPhotoService extends ServiceImpl<ProjectPhotoMapper, ProjectPhoto> {

    /**
     * Find photo by ID
     */
    public Optional<ProjectPhoto> findById(String id) {
        return Optional.ofNullable(getById(id));
    }

    /**
     * Find photos by project ID ordered by created at desc
     */
    public List<ProjectPhoto> findByProjectIdOrderByCreatedAtDesc(String projectId) {
        return list(new LambdaQueryWrapper<ProjectPhoto>()
                .eq(ProjectPhoto::getProjectId, projectId)
                .orderByDesc(ProjectPhoto::getCreatedAt));
    }

    /**
     * Find photo by token
     */
    public Optional<ProjectPhoto> findByToken(String token) {
        return Optional.ofNullable(getOne(new LambdaQueryWrapper<ProjectPhoto>()
                .eq(ProjectPhoto::getToken, token)));
    }

    /**
     * Save photo
     */
    @Transactional
    public ProjectPhoto savePhoto(ProjectPhoto photo) {
        saveOrUpdate(photo);
        return photo;
    }

    /**
     * Delete photo by ID
     */
    @Transactional
    public void deletePhotoById(String id) {
        removeById(id);
    }

    /**
     * Delete all photos by project ID
     */
    @Transactional
    public void deleteByProjectId(String projectId) {
        remove(new LambdaQueryWrapper<ProjectPhoto>()
                .eq(ProjectPhoto::getProjectId, projectId));
    }
}
