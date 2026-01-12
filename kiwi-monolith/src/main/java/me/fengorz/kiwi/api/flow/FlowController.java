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
package me.fengorz.kiwi.api.flow;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.R;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Flow Controller
 * Flowable workflow operations
 *
 * @author codingByFeng
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/flow")
@Tag(name = "Flow", description = "Flowable workflow operations")
public class FlowController {

    // TODO: Inject FlowableService when Flowable is configured
    // private final RuntimeService runtimeService;
    // private final TaskService taskService;

    @PostMapping("/process")
    @Operation(summary = "Start a process instance")
    public R<Void> startProcessInstance() {
        log.info("Starting process instance");
        // TODO: Implement with Flowable
        // runtimeService.startProcessInstanceByKey("myProcess");
        return R.ok();
    }

    @GetMapping(value = "/tasks", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get tasks by assignee")
    public R<List<TaskRepresentation>> getTasks(@RequestParam String assignee) {
        log.info("Getting tasks for assignee: {}", assignee);
        // TODO: Implement with Flowable
        // List<Task> tasks = taskService.createTaskQuery().taskAssignee(assignee).list();
        List<TaskRepresentation> dtos = new ArrayList<>();
        // for (Task task : tasks) {
        //     dtos.add(new TaskRepresentation(task.getId(), task.getName()));
        // }
        return R.ok(dtos);
    }

    @PostMapping("/tasks/{taskId}/complete")
    @Operation(summary = "Complete a task")
    public R<Void> completeTask(@PathVariable String taskId) {
        log.info("Completing task: {}", taskId);
        // TODO: Implement with Flowable
        // taskService.complete(taskId);
        return R.ok();
    }

    @GetMapping("/process/{processId}/tasks")
    @Operation(summary = "Get tasks for a process instance")
    public R<List<TaskRepresentation>> getProcessTasks(@PathVariable String processId) {
        log.info("Getting tasks for process: {}", processId);
        // TODO: Implement with Flowable
        List<TaskRepresentation> dtos = new ArrayList<>();
        return R.ok(dtos);
    }

    @Data
    @AllArgsConstructor
    public static class TaskRepresentation {
        private String id;
        private String name;
    }
}
