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
import me.fengorz.kiwi.domain.tools.entity.TodoTask;
import me.fengorz.kiwi.domain.tools.mapper.TodoTaskMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * TodoTask Service - manages todo task operations
 *
 * @author codingByFeng
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class TodoTaskService extends ServiceImpl<TodoTaskMapper, TodoTask> {

    /**
     * Find task by ID
     */
    public Optional<TodoTask> findById(String id) {
        return Optional.ofNullable(getById(id));
    }

    /**
     * Find tasks by user ID with pagination
     */
    public Page<TodoTask> findByUserId(Integer userId, Page<TodoTask> page) {
        return page(page, new LambdaQueryWrapper<TodoTask>()
                .eq(TodoTask::getUserId, userId)
                .eq(TodoTask::getDeleted, false));
    }

    /**
     * Find tasks by user ID and status with pagination
     */
    public Page<TodoTask> findByUserIdAndStatus(Integer userId, String status, Page<TodoTask> page) {
        return page(page, new LambdaQueryWrapper<TodoTask>()
                .eq(TodoTask::getUserId, userId)
                .eq(TodoTask::getStatus, status)
                .eq(TodoTask::getDeleted, false));
    }

    /**
     * Find task by ID and user ID
     */
    public Optional<TodoTask> findByIdAndUserId(String id, Integer userId) {
        return Optional.ofNullable(getOne(new LambdaQueryWrapper<TodoTask>()
                .eq(TodoTask::getId, id)
                .eq(TodoTask::getUserId, userId)));
    }

    /**
     * Find non-deleted tasks by user ID
     */
    public List<TodoTask> findByUserIdAndDeletedFalse(Integer userId) {
        return list(new LambdaQueryWrapper<TodoTask>()
                .eq(TodoTask::getUserId, userId)
                .eq(TodoTask::getDeleted, false));
    }

    /**
     * Find deleted tasks by user ID with pagination
     */
    public Page<TodoTask> findByUserIdAndDeletedTrue(Integer userId, Page<TodoTask> page) {
        return page(page, new LambdaQueryWrapper<TodoTask>()
                .eq(TodoTask::getUserId, userId)
                .eq(TodoTask::getDeleted, true));
    }

    /**
     * Find all deleted tasks by user ID
     */
    public List<TodoTask> findAllByUserIdAndDeletedTrue(Integer userId) {
        return list(new LambdaQueryWrapper<TodoTask>()
                .eq(TodoTask::getUserId, userId)
                .eq(TodoTask::getDeleted, true));
    }

    /**
     * Find task by ID, user ID, and deleted status true
     */
    public Optional<TodoTask> findByIdAndUserIdAndDeletedTrue(String id, Integer userId) {
        return Optional.ofNullable(getOne(new LambdaQueryWrapper<TodoTask>()
                .eq(TodoTask::getId, id)
                .eq(TodoTask::getUserId, userId)
                .eq(TodoTask::getDeleted, true)));
    }

    /**
     * Save task
     */
    @Transactional
    public TodoTask saveTask(TodoTask task) {
        saveOrUpdate(task);
        return task;
    }

    /**
     * Delete task by ID
     */
    @Transactional
    public void deleteTaskById(String id) {
        removeById(id);
    }

    /**
     * Delete all tasks in list
     */
    @Transactional
    public void deleteAllTasks(List<TodoTask> tasks) {
        removeBatchByIds(tasks.stream().map(TodoTask::getId).toList());
    }
}
