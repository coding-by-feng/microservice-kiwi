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
package me.fengorz.kiwi.api.tools;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.R;
import me.fengorz.kiwi.domain.tools.dto.*;
import me.fengorz.kiwi.domain.tools.entity.TodoTask;
import me.fengorz.kiwi.domain.tools.mapper.TodoDtoMapper;
import me.fengorz.kiwi.domain.tools.service.TodoService;
import me.fengorz.kiwi.security.KiwiUser;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Todo Controller
 * Task management operations with full functionality
 *
 * @author codingByFeng
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/tools/todo")
@Tag(name = "Todo", description = "Task management operations")
public class TodoController {

    private final TodoService todoService;

    @GetMapping("/tasks")
    @Operation(summary = "List tasks with filter/sort/pagination")
    public R<TaskListResponse> listTasks(
            @AuthenticationPrincipal KiwiUser user,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String frequency,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false, name = "date") String dateStr) {
        return R.ok(todoService.listTasks(user.getUserId(), page, pageSize, status, frequency, search, sort, dateStr));
    }

    @PostMapping(value = "/tasks", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create a new task")
    public ResponseEntity<R<SingleTaskResponse>> createTask(
            @AuthenticationPrincipal KiwiUser user,
            @RequestBody TaskCreateRequest body) {
        TodoTask created = todoService.createTask(user.getUserId(), body);
        String etag = todoService.computeETag(created);
        SingleTaskResponse resp = new SingleTaskResponse();
        resp.setData(TodoDtoMapper.toTaskDTO(created));
        return ResponseEntity.status(HttpStatus.CREATED)
                .header("ETag", etag)
                .body(R.ok(resp));
    }

    @GetMapping("/tasks/{id}")
    @Operation(summary = "Get task by ID")
    public ResponseEntity<R<SingleTaskResponse>> getTask(
            @AuthenticationPrincipal KiwiUser user,
            @PathVariable String id) {
        TodoTask t = todoService.getTask(user.getUserId(), id);
        String etag = todoService.computeETag(t);
        SingleTaskResponse resp = new SingleTaskResponse();
        resp.setData(TodoDtoMapper.toTaskDTO(t));
        return ResponseEntity.ok().header("ETag", etag).body(R.ok(resp));
    }

    @PatchMapping(value = "/tasks/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update a task")
    public ResponseEntity<R<SingleTaskResponse>> updateTask(
            @AuthenticationPrincipal KiwiUser user,
            @PathVariable String id,
            @RequestHeader("If-Match") String ifMatch,
            @RequestBody TaskUpdateRequest body) {
        TodoTask updated = todoService.updateTask(user.getUserId(), id, body, ifMatch);
        String etag = todoService.computeETag(updated);
        SingleTaskResponse resp = new SingleTaskResponse();
        resp.setData(TodoDtoMapper.toTaskDTO(updated));
        return ResponseEntity.ok().header("ETag", etag).body(R.ok(resp));
    }

    @DeleteMapping("/tasks/{id}")
    @Operation(summary = "Delete task (move to trash)")
    public R<DeleteOkResponse> deleteTask(
            @AuthenticationPrincipal KiwiUser user,
            @PathVariable String id) {
        Map<String, Boolean> m = todoService.deleteTaskToTrash(user.getUserId(), id);
        DeleteOkResponse resp = new DeleteOkResponse();
        resp.setData(m);
        return R.ok(resp);
    }

    @PostMapping(value = "/tasks/{id}/complete", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Complete a task")
    public R<CompleteTaskResponse> completeTask(
            @AuthenticationPrincipal KiwiUser user,
            @PathVariable String id,
            @RequestBody CompleteTaskRequest body) {
        Map<String, Object> data = todoService.completeTask(user.getUserId(), id, body.getStatus());
        CompleteTaskResponse resp = new CompleteTaskResponse();
        resp.setData(data);
        return R.ok(resp);
    }

    @PostMapping("/tasks/{id}/reset-status")
    @Operation(summary = "Reset task status")
    public R<SingleTaskResponse> resetStatus(
            @AuthenticationPrincipal KiwiUser user,
            @PathVariable String id) {
        TodoTask updated = todoService.resetTaskStatus(user.getUserId(), id);
        SingleTaskResponse resp = new SingleTaskResponse();
        resp.setData(TodoDtoMapper.toTaskDTO(updated));
        return R.ok(resp);
    }

    @PostMapping("/tasks/reset-statuses")
    @Operation(summary = "Reset all task statuses")
    public R<Map<String, Integer>> resetAll(@AuthenticationPrincipal KiwiUser user) {
        Map<String, Integer> m = todoService.resetAllTasks(user.getUserId());
        return R.ok(m);
    }

    @PostMapping("/tasks/demo")
    @Operation(summary = "Seed demo tasks")
    public R<DemoSeedResponse> seedDemo(@AuthenticationPrincipal KiwiUser user) {
        int tasksCreated = todoService.seedDemo(user.getUserId());
        Map<String, Integer> m = new HashMap<>();
        m.put("tasksCreated", tasksCreated);
        m.put("historyCreated", 0);
        m.put("trashCreated", 0);
        DemoSeedResponse resp = new DemoSeedResponse();
        resp.setData(m);
        return R.ok(resp);
    }

    // -------- Trash --------

    @GetMapping("/trash")
    @Operation(summary = "List deleted tasks")
    public R<TrashListResponse> listTrash(
            @AuthenticationPrincipal KiwiUser user,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize) {
        return R.ok(todoService.listTrash(user.getUserId(), page, pageSize));
    }

    @DeleteMapping("/trash")
    @Operation(summary = "Clear trash (permanent delete)")
    public R<ClearTrashResponse> clearTrash(@AuthenticationPrincipal KiwiUser user) {
        Map<String, Integer> m = todoService.clearTrash(user.getUserId());
        ClearTrashResponse resp = new ClearTrashResponse();
        resp.setData(m);
        return R.ok(resp);
    }

    @DeleteMapping("/trash/{id}")
    @Operation(summary = "Permanently delete a task from trash")
    public R<DeleteOkResponse> deleteTrashItem(
            @AuthenticationPrincipal KiwiUser user,
            @PathVariable String id) {
        Map<String, Boolean> m = todoService.deleteTrashItem(user.getUserId(), id);
        DeleteOkResponse resp = new DeleteOkResponse();
        resp.setData(m);
        return R.ok(resp);
    }

    @PostMapping("/trash/{id}/restore")
    @Operation(summary = "Restore task from trash")
    public R<SingleTaskResponse> restoreFromTrash(
            @AuthenticationPrincipal KiwiUser user,
            @PathVariable String id) {
        TodoTask t = todoService.restoreFromTrash(user.getUserId(), id);
        SingleTaskResponse resp = new SingleTaskResponse();
        resp.setData(TodoDtoMapper.toTaskDTO(t));
        return R.ok(resp);
    }

    // -------- History --------

    @GetMapping("/history")
    @Operation(summary = "List completed task history")
    public R<HistoryListResponse> listHistory(
            @AuthenticationPrincipal KiwiUser user,
            @RequestParam(name = "date") String dateStr,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize) {
        return R.ok(todoService.listHistory(user.getUserId(), dateStr, page, pageSize));
    }

    @DeleteMapping("/history/{id}")
    @Operation(summary = "Delete history record")
    public R<DeleteOkWithRankingMetaResponse> deleteHistory(
            @AuthenticationPrincipal KiwiUser user,
            @PathVariable String id) {
        Map<String, Object> wrapper = todoService.deleteHistory(user.getUserId(), id);
        DeleteOkWithRankingMetaResponse resp = new DeleteOkWithRankingMetaResponse();
        @SuppressWarnings("unchecked")
        Map<String, Boolean> data = (Map<String, Boolean>) wrapper.getOrDefault("data", Collections.singletonMap("ok", true));
        @SuppressWarnings("unchecked")
        Map<String, Object> meta = (Map<String, Object>) wrapper.getOrDefault("meta", Collections.emptyMap());
        resp.setData(data);
        resp.setMeta(meta);
        return R.ok(resp);
    }

    // -------- Analytics --------

    @GetMapping("/analytics/monthly")
    @Operation(summary = "Get monthly analytics")
    public R<AnalyticsMonthlyResponse> analyticsMonthly(
            @AuthenticationPrincipal KiwiUser user,
            @RequestParam(required = false) Integer months) {
        AnalyticsMonthlyResponse resp = new AnalyticsMonthlyResponse();
        resp.setData(todoService.analyticsMonthly(user.getUserId(), months));
        return R.ok(resp);
    }

    @GetMapping("/analytics/summary")
    @Operation(summary = "Get analytics summary")
    public R<AnalyticsSummaryResponse> analyticsSummary(
            @AuthenticationPrincipal KiwiUser user,
            @RequestParam(name = "month", required = false) String month) {
        AnalyticsSummaryResponse resp = new AnalyticsSummaryResponse();
        resp.setData(todoService.analyticsSummary(user.getUserId(), month));
        return R.ok(resp);
    }

    // -------- Ranking --------

    @GetMapping("/ranking/current")
    @Operation(summary = "Get current user ranking")
    public R<RankingResponse> rankingCurrent(@AuthenticationPrincipal KiwiUser user) {
        RankingResponse resp = new RankingResponse();
        resp.setData(todoService.computeRanking(user.getUserId()));
        return R.ok(resp);
    }

    @GetMapping("/ranking/ranks")
    @Operation(summary = "Get rank definitions")
    public R<List<RankDefinitionDTO>> rankingDefs() {
        return R.ok(todoService.getRankDefs());
    }

}
