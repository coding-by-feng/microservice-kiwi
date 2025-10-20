package me.fengorz.kiwi.tools.controller;

import me.fengorz.kiwi.common.api.R;
import me.fengorz.kiwi.common.sdk.web.security.SecurityUtils;
import me.fengorz.kiwi.tools.api.todo.dto.*;
import me.fengorz.kiwi.tools.exception.ToolsException;
import me.fengorz.kiwi.tools.mapper.TodoDtoMapper;
import me.fengorz.kiwi.tools.model.todo.TodoTask;
import me.fengorz.kiwi.tools.service.RateLimitService;
import me.fengorz.kiwi.tools.service.todo.TodoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

@RestController
@RequestMapping("/todo")
public class TodoController {

    private final TodoService service;
    private final RateLimitService rateLimitService;

    public TodoController(TodoService service, RateLimitService rateLimitService) {
        this.service = service;
        this.rateLimitService = rateLimitService;
    }

    // -------- Tasks --------

    @GetMapping("/tasks")
    public R<TaskListResponse> listTasks(HttpServletRequest request,
                                      @RequestParam(required = false) Integer page,
                                      @RequestParam(required = false) Integer pageSize,
                                      @RequestParam(required = false) String status,
                                      @RequestParam(required = false) String frequency,
                                      @RequestParam(required = false) String search,
                                      @RequestParam(required = false) String sort,
                                      @RequestParam(required = false, name = "date") String dateStr) {
        Integer userId = SecurityUtils.getCurrentUserId();
        return R.success(service.listTasks(userId, page, pageSize, status, frequency, search, sort, dateStr));
    }

    @PostMapping(value = "/tasks", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<R<SingleTaskResponse>> createTask(HttpServletRequest request,
                                                         @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                                                         @RequestBody TaskCreateRequest body) {
        Integer userId = SecurityUtils.getCurrentUserId();
        TodoTask created = service.createTask(userId, body, idempotencyKey);
        String etag = service.computeETag(created);
        SingleTaskResponse resp = new SingleTaskResponse();
        resp.setData(TodoDtoMapper.toTaskDTO(created));
        return ResponseEntity.status(HttpStatus.CREATED)
                .header("ETag", etag)
                .body(R.success(resp));
    }

    @GetMapping("/tasks/{id}")
    public ResponseEntity<R<SingleTaskResponse>> getTask(HttpServletRequest request, @PathVariable String id) {
        Integer userId = SecurityUtils.getCurrentUserId();
        TodoTask t = service.getTask(userId, id);
        String etag = service.computeETag(t);
        SingleTaskResponse resp = new SingleTaskResponse();
        resp.setData(TodoDtoMapper.toTaskDTO(t));
        return ResponseEntity.ok().header("ETag", etag).body(R.success(resp));
    }

    @PatchMapping(value = "/tasks/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<R<SingleTaskResponse>> updateTask(HttpServletRequest request,
                                                         @PathVariable String id,
                                                         @RequestHeader("If-Match") String ifMatch,
                                                         @RequestBody TaskUpdateRequest body) {
        Integer userId = SecurityUtils.getCurrentUserId();
        TodoTask updated = service.updateTask(userId, id, body, ifMatch);
        String etag = service.computeETag(updated);
        SingleTaskResponse resp = new SingleTaskResponse();
        resp.setData(TodoDtoMapper.toTaskDTO(updated));
        return ResponseEntity.ok().header("ETag", etag).body(R.success(resp));
    }

    @DeleteMapping("/tasks/{id}")
    public R<DeleteOkResponse> deleteTask(HttpServletRequest request, @PathVariable String id) {
        Integer userId = SecurityUtils.getCurrentUserId();
        Map<String, Boolean> m = service.deleteTaskToTrash(userId, id);
        DeleteOkResponse resp = new DeleteOkResponse();
        resp.setData(m);
        return R.success(resp);
    }

    @PostMapping(value = "/tasks/{id}/complete", consumes = MediaType.APPLICATION_JSON_VALUE)
    public R<CompleteTaskResponse> completeTask(HttpServletRequest request,
                                             @PathVariable String id,
                                             @RequestHeader("Idempotency-Key") String idempotencyKey,
                                             @RequestBody CompleteTaskRequest body) {
        Integer userId = SecurityUtils.getCurrentUserId();
        Map<String, Object> data = service.completeTask(userId, id, body.getStatus(), idempotencyKey);
        CompleteTaskResponse resp = new CompleteTaskResponse();
        resp.setData(data);
        return R.success(resp);
    }

    @PostMapping("/tasks/{id}/reset-status")
    public R<SingleTaskResponse> resetStatus(HttpServletRequest request, @PathVariable String id) {
        Integer userId = SecurityUtils.getCurrentUserId();
        TodoTask updated = service.resetTaskStatus(userId, id);
        SingleTaskResponse resp = new SingleTaskResponse();
        resp.setData(TodoDtoMapper.toTaskDTO(updated));
        return R.success(resp);
    }

    @PostMapping("/tasks/reset-statuses")
    public R<Map<String, Integer>> resetAll(HttpServletRequest request) {
        Integer userId = SecurityUtils.getCurrentUserId();
        Map<String, Integer> m = service.resetAllTasks(userId);
        return R.success(m);
    }

    @PostMapping("/tasks/demo")
    public R<DemoSeedResponse> seedDemo(HttpServletRequest request) {
        Integer userId = SecurityUtils.getCurrentUserId();
        int tasksCreated = service.seedDemo(userId);
        Map<String, Integer> m = new HashMap<>();
        m.put("tasksCreated", tasksCreated);
        m.put("historyCreated", 0);
        m.put("trashCreated", 0);
        DemoSeedResponse resp = new DemoSeedResponse();
        resp.setData(m);
        return R.success(resp);
    }

    // -------- Trash --------

    @GetMapping("/trash")
    public R<TrashListResponse> listTrash(HttpServletRequest request,
                                       @RequestParam(required = false) Integer page,
                                       @RequestParam(required = false) Integer pageSize) {
        Integer userId = SecurityUtils.getCurrentUserId();
        return R.success(service.listTrash(userId, page, pageSize));
    }

    @DeleteMapping("/trash")
    public R<ClearTrashResponse> clearTrash(HttpServletRequest request) {
        Integer userId = SecurityUtils.getCurrentUserId();
        Map<String, Integer> m = service.clearTrash(userId);
        ClearTrashResponse resp = new ClearTrashResponse();
        resp.setData(m);
        return R.success(resp);
    }

    @DeleteMapping("/trash/{id}")
    public R<DeleteOkResponse> deleteTrashItem(HttpServletRequest request, @PathVariable String id) {
        Integer userId = SecurityUtils.getCurrentUserId();
        Map<String, Boolean> m = service.deleteTrashItem(userId, id);
        DeleteOkResponse resp = new DeleteOkResponse();
        resp.setData(m);
        return R.success(resp);
    }

    @PostMapping("/trash/{id}/restore")
    public R<SingleTaskResponse> restoreFromTrash(HttpServletRequest request, @PathVariable String id) {
        Integer userId = SecurityUtils.getCurrentUserId();
        TodoTask t = service.restoreFromTrash(userId, id);
        SingleTaskResponse resp = new SingleTaskResponse();
        resp.setData(TodoDtoMapper.toTaskDTO(t));
        return R.success(resp);
    }

    // -------- History --------

    @GetMapping("/history")
    public R<HistoryListResponse> listHistory(HttpServletRequest request,
                                           @RequestParam(name = "date") String dateStr,
                                           @RequestParam(required = false) Integer page,
                                           @RequestParam(required = false) Integer pageSize) {
        Integer userId = SecurityUtils.getCurrentUserId();
        return R.success(service.listHistory(userId, dateStr, page, pageSize));
    }

    @DeleteMapping("/history/{id}")
    public R<DeleteOkWithRankingMetaResponse> deleteHistory(HttpServletRequest request, @PathVariable String id) {
        Integer userId = SecurityUtils.getCurrentUserId();
        Map<String, Object> wrapper = service.deleteHistory(userId, id);
        DeleteOkWithRankingMetaResponse resp = new DeleteOkWithRankingMetaResponse();
        @SuppressWarnings("unchecked")
        Map<String, Boolean> data = (Map<String, Boolean>) wrapper.getOrDefault("data", Collections.singletonMap("ok", true));
        @SuppressWarnings("unchecked")
        Map<String, Object> meta = (Map<String, Object>) wrapper.getOrDefault("meta", Collections.emptyMap());
        resp.setData(data);
        resp.setMeta(meta);
        return R.success(resp);
    }

    // -------- Analytics --------

    @GetMapping("/analytics/monthly")
    public R<AnalyticsMonthlyResponse> analyticsMonthly(HttpServletRequest request,
                                                     @RequestParam(required = false) Integer months) {
        Integer userId = SecurityUtils.getCurrentUserId();
        AnalyticsMonthlyResponse resp = new AnalyticsMonthlyResponse();
        resp.setData(service.analyticsMonthly(userId, months));
        return R.success(resp);
    }

    @GetMapping("/analytics/summary")
    public R<AnalyticsSummaryResponse> analyticsSummary(HttpServletRequest request,
                                                     @RequestParam(name = "month", required = false) String month) {
        Integer userId = SecurityUtils.getCurrentUserId();
        AnalyticsSummaryResponse resp = new AnalyticsSummaryResponse();
        resp.setData(service.analyticsSummary(userId, month));
        return R.success(resp);
    }

    // -------- Ranking --------

    @GetMapping("/ranking/current")
    public R<RankingResponse> rankingCurrent(HttpServletRequest request) {
        Integer userId = SecurityUtils.getCurrentUserId();
        RankingResponse resp = new RankingResponse();
        resp.setData(service.computeRanking(userId));
        return R.success(resp);
    }

    @GetMapping("/ranking/ranks")
    public R<List<RankDefinitionDTO>> rankingDefs() {
        return R.success(service.getRankDefs());
    }

    // -------- Import / Export --------

    @GetMapping("/export/todo")
    public R<TodoExportResponse> exportTodo(HttpServletRequest request) {
        Integer userId = SecurityUtils.getCurrentUserId();
        TodoExportResponse resp = new TodoExportResponse();
        resp.setData(service.exportAll(userId));
        return R.success(resp);
    }

    @PostMapping(value = "/import/todo", consumes = { MediaType.APPLICATION_JSON_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE })
    public R<TodoImportResponse> importTodo(HttpServletRequest request,
                                         @RequestBody(required = false) Map<String, Object> json,
                                         @RequestPart(name = "file", required = false) MultipartFile file) throws IOException {
        enforceWriteRateLimit(request);
        Integer userId = SecurityUtils.getCurrentUserId();
        Map<String, Object> body = json;
        if (body == null && file != null && !file.isEmpty()) {
            String text = new String(file.getBytes(), StandardCharsets.UTF_8);
            body = new com.fasterxml.jackson.databind.ObjectMapper().readValue(text, Map.class);
        }
        if (body == null) {
            throw new ToolsException(HttpStatus.BAD_REQUEST, "validation_error", "import body or file is required");
        }
        Map<String, Integer> result = service.importAll(userId, body);
        TodoImportResponse resp = new TodoImportResponse();
        resp.setData(result);
        return R.success(resp);
    }

    // -------- helpers --------

    // Use global rate limit helper, keep as-is
    private void enforceWriteRateLimit(HttpServletRequest request) {
        String ip = Optional.ofNullable(request.getHeader("X-Forwarded-For")).orElse(request.getRemoteAddr());
        if (!rateLimitService.allowWrite(ip)) {
            throw new ToolsException(HttpStatus.TOO_MANY_REQUESTS, "rate_limited", "Too many requests");
        }
    }
}
