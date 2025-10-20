package me.fengorz.kiwi.tools.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import me.fengorz.kiwi.tools.api.todo.dto.*;
import me.fengorz.kiwi.tools.model.todo.TodoHistory;
import me.fengorz.kiwi.tools.model.todo.TodoTask;
import me.fengorz.kiwi.tools.model.todo.TodoTrash;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class TodoDtoMapper {
    private static final ObjectMapper om = new ObjectMapper();

    @SneakyThrows
    public static TaskDTO toTaskDTO(TodoTask t) {
        if (t == null) return null;
        TaskDTO dto = new TaskDTO();
        dto.setId(t.getId());
        dto.setUserId(t.getUserId() == null ? null : String.valueOf(t.getUserId()));
        dto.setTitle(t.getTitle());
        dto.setDescription(t.getDescription());
        dto.setSuccessPoints(t.getSuccessPoints());
        dto.setFailPoints(t.getFailPoints());
        dto.setFrequency(t.getFrequency());
        dto.setCustomDays(t.getCustomDays());
        dto.setStatus(t.getStatus());
        dto.setCreatedAt(t.getCreatedAt());
        dto.setUpdatedAt(t.getUpdatedAt());
        dto.setDeletedAt(t.getDeletedAt());
        if (t.getMetadata() != null && !t.getMetadata().isEmpty()) {
            dto.setMetadata(om.readValue(t.getMetadata(), new TypeReference<Map<String,Object>>(){}));
        }
        return dto;
    }

    @SneakyThrows
    public static TodoTask toEntity(TaskCreateRequest req, Integer userId) {
        TodoTask t = new TodoTask();
        t.setUserId(userId);
        t.setTitle(req.getTitle());
        t.setDescription(req.getDescription());
        t.setSuccessPoints(Optional.ofNullable(req.getSuccessPoints()).orElse(10));
        t.setFailPoints(Optional.ofNullable(req.getFailPoints()).orElse(-5));
        t.setFrequency(Optional.ofNullable(req.getFrequency()).orElse("once"));
        t.setCustomDays(req.getCustomDays());
        t.setStatus("pending");
        t.setMetadata(om.writeValueAsString(new HashMap<String,Object>()));
        return t;
    }

    public static void applyUpdate(TodoTask t, TaskUpdateRequest req) {
        if (req.getTitle() != null) t.setTitle(req.getTitle());
        if (req.getDescription() != null) t.setDescription(req.getDescription());
        if (req.getSuccessPoints() != null) t.setSuccessPoints(req.getSuccessPoints());
        if (req.getFailPoints() != null) t.setFailPoints(req.getFailPoints());
        if (req.getFrequency() != null) t.setFrequency(req.getFrequency());
        if (req.getCustomDays() != null) t.setCustomDays(req.getCustomDays());
    }

    public static HistoryRecordDTO toHistoryDTO(TodoHistory h) {
        if (h == null) return null;
        HistoryRecordDTO dto = new HistoryRecordDTO();
        dto.setId(h.getId());
        dto.setUserId(h.getUserId() == null ? null : String.valueOf(h.getUserId()));
        dto.setTaskId(h.getTaskId());
        dto.setTitle(h.getTitle());
        dto.setDescription(h.getDescription());
        dto.setSuccessPoints(h.getSuccessPoints());
        dto.setFailPoints(h.getFailPoints());
        dto.setStatus(h.getStatus());
        dto.setPointsApplied(h.getPointsApplied());
        dto.setCompletedAt(h.getCompletedAt());
        return dto;
    }

    public static TrashItemDTO toTrashDTO(TodoTrash tr) {
        if (tr == null) return null;
        TrashItemDTO dto = new TrashItemDTO();
        dto.setId(tr.getId());
        dto.setTitle(tr.getTitle());
        dto.setDescription(tr.getDescription());
        dto.setSuccessPoints(tr.getSuccessPoints());
        dto.setFailPoints(tr.getFailPoints());
        dto.setFrequency(tr.getFrequency());
        dto.setCustomDays(tr.getCustomDays());
        dto.setStatus(tr.getStatus());
        dto.setOriginalDate(tr.getOriginalDate());
        dto.setDeletedDate(tr.getDeletedDate());
        return dto;
    }

    public static TaskListResponse toTaskListResponse(List<TodoTask> items, int page, int pageSize, long total) {
        TaskListResponse resp = new TaskListResponse();
        List<TaskDTO> data = items.stream().map(TodoDtoMapper::toTaskDTO).collect(Collectors.toList());
        resp.setData(data);
        PageMeta meta = new PageMeta();
        meta.setPage(page);
        meta.setPageSize(pageSize);
        meta.setTotal(total);
        resp.setMeta(meta);
        return resp;
    }

    public static TrashListResponse toTrashListResponse(List<TodoTrash> items, int page, int pageSize, long total) {
        TrashListResponse resp = new TrashListResponse();
        List<TrashItemDTO> data = items.stream().map(TodoDtoMapper::toTrashDTO).collect(Collectors.toList());
        resp.setData(data);
        PageMeta meta = new PageMeta();
        meta.setPage(page);
        meta.setPageSize(pageSize);
        meta.setTotal(total);
        resp.setMeta(meta);
        return resp;
    }

    public static HistoryListResponse toHistoryListResponse(List<TodoHistory> items, int page, int pageSize, long total, LocalDate date) {
        HistoryListResponse resp = new HistoryListResponse();
        List<HistoryRecordDTO> data = items.stream().map(TodoDtoMapper::toHistoryDTO).collect(Collectors.toList());
        resp.setData(data);
        Map<String,Object> meta = new HashMap<>();
        meta.put("page", page);
        meta.put("pageSize", pageSize);
        meta.put("total", total);
        meta.put("date", date.toString());
        resp.setMeta(meta);
        return resp;
    }
}

