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
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import me.fengorz.kiwi.domain.tools.config.TodoDemoProperties;
import me.fengorz.kiwi.domain.tools.config.TodoRankingProperties;
import me.fengorz.kiwi.domain.tools.dto.*;
import me.fengorz.kiwi.domain.tools.entity.TodoHistory;
import me.fengorz.kiwi.domain.tools.entity.TodoTask;
import me.fengorz.kiwi.domain.tools.entity.TodoTrash;
import me.fengorz.kiwi.domain.tools.exception.ToolsException;
import me.fengorz.kiwi.domain.tools.mapper.TodoDtoMapper;
import me.fengorz.kiwi.domain.tools.mapper.TodoHistoryMapper;
import me.fengorz.kiwi.domain.tools.mapper.TodoTaskMapper;
import me.fengorz.kiwi.domain.tools.mapper.TodoTrashMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;

/**
 * Todo Service - Full functionality for todo task management
 *
 * @author codingByFeng
 */
@Service
@RequiredArgsConstructor
public class TodoService {

    private final TodoTaskMapper taskMapper;
    private final TodoHistoryMapper historyMapper;
    private final TodoTrashMapper trashMapper;
    private final TodoRankingProperties rankingProperties;
    private final TodoDemoProperties demoProperties;

    public TaskListResponse listTasks(Integer userId, Integer page, Integer pageSize,
                                      String status, String frequency, String search,
                                      String sort, String dateStr) {
        int p = page == null || page < 1 ? 1 : page;
        int sz = pageSize == null || pageSize < 1 ? 20 : Math.min(pageSize, 100);
        LambdaQueryWrapper<TodoTask> qw = new LambdaQueryWrapper<>();
        qw.eq(TodoTask::getUserId, userId)
                .isNull(TodoTask::getDeletedAt);
        if (status != null && !"all".equals(status)) {
            qw.eq(TodoTask::getStatus, status);
        }
        if (frequency != null && !"all".equals(frequency)) {
            qw.eq(TodoTask::getFrequency, frequency);
        }
        if (search != null && !search.isEmpty()) {
            String like = "%" + search + "%";
            qw.and(w -> w.like(TodoTask::getTitle, like).or().like(TodoTask::getDescription, like));
        }
        if (dateStr != null && !dateStr.isEmpty()) {
            qw.apply("date(created_at) = {0}", dateStr);
        }
        String orderBy = "points_desc".equals(sort) ? "success_points desc" :
                ("created_desc".equals(sort) ? "created_at desc" :
                        ("updated_desc".equals(sort) ? "updated_at desc" : "success_points desc"));
        qw.last("order by " + orderBy);
        Page<TodoTask> mp = new Page<>(p, sz);
        List<TodoTask> items = taskMapper.selectPage(mp, qw).getRecords();
        long total = mp.getTotal();
        return TodoDtoMapper.toTaskListResponse(items, p, sz, total);
    }

    public TodoTask getTask(Integer userId, String id) {
        TodoTask t = taskMapper.selectById(id);
        if (t == null || !Objects.equals(t.getUserId(), userId)) {
            throw new ToolsException(HttpStatus.NOT_FOUND, "not_found", "Task not found");
        }
        return t;
    }

    public String computeETag(TodoTask t) {
        String basis = t.getId() + ":" + (t.getUpdatedAt() == null ? 0 : t.getUpdatedAt().toEpochSecond(java.time.ZoneOffset.UTC));
        return '"' + Integer.toHexString(basis.hashCode()) + '"';
    }

    private String normalizeETagToken(String token) {
        if (token == null) return "";
        String s = token.trim();
        if (s.startsWith("W/")) {
            s = s.substring(2).trim();
        }
        if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) {
            s = s.substring(1, s.length() - 1);
        }
        return s;
    }

    private boolean ifMatchSatisfied(String ifMatchHeader, TodoTask current) {
        if (ifMatchHeader == null || ifMatchHeader.trim().isEmpty()) return false;
        String raw = ifMatchHeader.trim();
        if ("*".equals(raw)) return true;
        String currentNorm = normalizeETagToken(computeETag(current));
        String[] parts = raw.split(",");
        for (String part : parts) {
            String norm = normalizeETagToken(part);
            if (!norm.isEmpty() && norm.equals(currentNorm)) {
                return true;
            }
        }
        return false;
    }

    @Transactional
    public TodoTask createTask(Integer userId, TaskCreateRequest req) {
        if (req.getTitle() == null || req.getTitle().trim().isEmpty()) {
            throw new ToolsException(HttpStatus.BAD_REQUEST, "validation_error", "title is required");
        }
        TodoTask t = TodoDtoMapper.toEntity(req, userId);
        t.setId(String.valueOf(IdWorker.getId()));
        t.setCreatedAt(LocalDateTime.now());
        t.setUpdatedAt(t.getCreatedAt());
        taskMapper.insert(t);
        return t;
    }

    @Transactional
    public TodoTask updateTask(Integer userId, String id, TaskUpdateRequest req, String ifMatch) {
        TodoTask cur = getTask(userId, id);
        if (!ifMatchSatisfied(ifMatch, cur)) {
            throw new ToolsException(HttpStatus.PRECONDITION_FAILED, "precondition_failed", "ETag mismatch");
        }
        TodoDtoMapper.applyUpdate(cur, req);
        cur.setUpdatedAt(LocalDateTime.now());
        taskMapper.updateById(cur);
        return cur;
    }

    @Transactional
    public Map<String, Boolean> deleteTaskToTrash(Integer userId, String id) {
        TodoTask cur = getTask(userId, id);
        TodoTrash tr = new TodoTrash();
        tr.setId(String.valueOf(IdWorker.getId()));
        tr.setUserId(userId);
        tr.setTitle(cur.getTitle());
        tr.setDescription(cur.getDescription());
        tr.setSuccessPoints(cur.getSuccessPoints());
        tr.setFailPoints(cur.getFailPoints());
        tr.setFrequency(cur.getFrequency());
        tr.setCustomDays(cur.getCustomDays());
        tr.setStatus(cur.getStatus());
        tr.setOriginalDate(cur.getCreatedAt());
        tr.setDeletedDate(LocalDateTime.now());
        trashMapper.insert(tr);
        cur.setDeletedAt(LocalDateTime.now());
        taskMapper.updateById(cur);
        return Collections.singletonMap("ok", true);
    }

    @Transactional
    public Map<String, Object> completeTask(Integer userId, String id, String status) {
        if (!"success".equals(status) && !"fail".equals(status)) {
            throw new ToolsException(HttpStatus.BAD_REQUEST, "validation_error", "status must be success|fail");
        }
        TodoTask cur = getTask(userId, id);
        TodoHistory h = new TodoHistory();
        h.setId(String.valueOf(IdWorker.getId()));
        h.setUserId(userId);
        h.setTaskId(cur.getId());
        h.setTitle(cur.getTitle());
        h.setDescription(cur.getDescription());
        h.setSuccessPoints(cur.getSuccessPoints());
        h.setFailPoints(cur.getFailPoints());
        h.setStatus(status);
        h.setPointsApplied("success".equals(status) ? cur.getSuccessPoints() : cur.getFailPoints());
        h.setCompletedAt(LocalDateTime.now());
        historyMapper.insert(h);
        cur.setStatus(status);
        cur.setUpdatedAt(LocalDateTime.now());
        taskMapper.updateById(cur);
        RankingDTO ranking = computeRanking(userId);
        Map<String, Object> resp = new HashMap<>();
        resp.put("task", TodoDtoMapper.toTaskDTO(cur));
        resp.put("history", TodoDtoMapper.toHistoryDTO(h));
        resp.put("ranking", ranking);
        return resp;
    }

    @Transactional
    public TodoTask resetTaskStatus(Integer userId, String id) {
        TodoTask cur = getTask(userId, id);
        cur.setStatus("pending");
        cur.setUpdatedAt(LocalDateTime.now());
        taskMapper.updateById(cur);
        return cur;
    }

    @Transactional
    public Map<String, Integer> resetAllTasks(Integer userId) {
        LambdaQueryWrapper<TodoTask> qw = new LambdaQueryWrapper<>();
        qw.eq(TodoTask::getUserId, userId).isNull(TodoTask::getDeletedAt);
        List<TodoTask> list = taskMapper.selectList(qw);
        int cnt = 0;
        for (TodoTask t : list) {
            if (!"pending".equals(t.getStatus())) {
                t.setStatus("pending");
                t.setUpdatedAt(LocalDateTime.now());
                taskMapper.updateById(t);
                cnt++;
            }
        }
        return Collections.singletonMap("resetCount", cnt);
    }

    public TrashListResponse listTrash(Integer userId, Integer page, Integer pageSize) {
        int p = page == null || page < 1 ? 1 : page;
        int sz = pageSize == null || pageSize < 1 ? 20 : Math.min(pageSize, 100);
        LambdaQueryWrapper<TodoTrash> qw = new LambdaQueryWrapper<>();
        qw.eq(TodoTrash::getUserId, userId).orderByDesc(TodoTrash::getDeletedDate);
        Page<TodoTrash> mp = new Page<>(p, sz);
        List<TodoTrash> items = trashMapper.selectPage(mp, qw).getRecords();
        return TodoDtoMapper.toTrashListResponse(items, p, sz, mp.getTotal());
    }

    @Transactional
    public Map<String, Integer> clearTrash(Integer userId) {
        LambdaQueryWrapper<TodoTrash> qw = new LambdaQueryWrapper<>();
        qw.eq(TodoTrash::getUserId, userId);
        int count = trashMapper.delete(qw);
        return Collections.singletonMap("deletedCount", count);
    }

    @Transactional
    public Map<String, Boolean> deleteTrashItem(Integer userId, String id) {
        TodoTrash tr = trashMapper.selectById(id);
        if (tr == null || !Objects.equals(tr.getUserId(), userId)) {
            throw new ToolsException(HttpStatus.NOT_FOUND, "not_found", "Trash item not found");
        }
        trashMapper.deleteById(id);
        return Collections.singletonMap("ok", true);
    }

    @Transactional
    public TodoTask restoreFromTrash(Integer userId, String id) {
        TodoTrash tr = trashMapper.selectById(id);
        if (tr == null || !Objects.equals(tr.getUserId(), userId)) {
            throw new ToolsException(HttpStatus.NOT_FOUND, "not_found", "Trash item not found");
        }
        TodoTask t = new TodoTask();
        t.setId(String.valueOf(IdWorker.getId()));
        t.setUserId(userId);
        t.setTitle(tr.getTitle());
        t.setDescription(tr.getDescription());
        t.setSuccessPoints(tr.getSuccessPoints());
        t.setFailPoints(tr.getFailPoints());
        t.setFrequency(tr.getFrequency());
        t.setCustomDays(tr.getCustomDays());
        t.setStatus("pending");
        t.setCreatedAt(LocalDateTime.now());
        t.setUpdatedAt(t.getCreatedAt());
        taskMapper.insert(t);
        trashMapper.deleteById(id);
        return t;
    }

    public HistoryListResponse listHistory(Integer userId, String dateStr, Integer page, Integer pageSize) {
        if (dateStr == null || dateStr.isEmpty()) {
            throw new ToolsException(HttpStatus.BAD_REQUEST, "validation_error", "date is required");
        }
        LocalDate date = LocalDate.parse(dateStr);
        int p = page == null || page < 1 ? 1 : page;
        int sz = pageSize == null || pageSize < 1 ? 20 : Math.min(pageSize, 100);
        LambdaQueryWrapper<TodoHistory> qw = new LambdaQueryWrapper<>();
        qw.eq(TodoHistory::getUserId, userId)
                .apply("date(completed_at) = {0}", date.toString())
                .orderByDesc(TodoHistory::getCompletedAt);
        Page<TodoHistory> mp = new Page<>(p, sz);
        List<TodoHistory> items = historyMapper.selectPage(mp, qw).getRecords();
        return TodoDtoMapper.toHistoryListResponse(items, p, sz, mp.getTotal(), date);
    }

    @Transactional
    public Map<String, Object> deleteHistory(Integer userId, String id) {
        TodoHistory h = historyMapper.selectById(id);
        if (h == null || !Objects.equals(h.getUserId(), userId)) {
            throw new ToolsException(HttpStatus.NOT_FOUND, "not_found", "History not found");
        }
        historyMapper.deleteById(id);
        RankingDTO ranking = computeRanking(userId);
        Map<String, Object> wrapper = new HashMap<>();
        wrapper.put("data", Collections.singletonMap("ok", true));
        wrapper.put("meta", Collections.singletonMap("ranking", ranking));
        return wrapper;
    }

    public RankingDTO computeRanking(Integer userId) {
        Integer total = historyMapper.selectList(new LambdaQueryWrapper<TodoHistory>().eq(TodoHistory::getUserId, userId))
                .stream().map(h -> Optional.ofNullable(h.getPointsApplied()).orElse(0)).reduce(0, Integer::sum);
        List<RankDefinitionDTO> ranks = getRankDefs();
        ranks.sort(Comparator.comparingInt(RankDefinitionDTO::getThreshold).thenComparingInt(RankDefinitionDTO::getLevel));
        RankDefinitionDTO current = ranks.get(0);
        RankDefinitionDTO next = null;
        for (RankDefinitionDTO r : ranks) {
            if (total >= r.getThreshold()) {
                current = r;
            } else {
                next = r;
                break;
            }
        }
        double progress = next == null ? 100.0 : (100.0 * (total - current.getThreshold()) / (next.getThreshold() - current.getThreshold()));
        RankingDTO dto = new RankingDTO();
        dto.setTotalPoints(total);
        dto.setCurrentRank(current);
        dto.setNextRank(next);
        dto.setProgressPct(Math.max(0, Math.min(100, progress)));
        return dto;
    }

    public List<RankDefinitionDTO> getRankDefs() {
        List<RankDefinitionDTO> configured = rankingProperties != null ? rankingProperties.getRanks() : null;
        if (configured != null && !configured.isEmpty()) {
            List<RankDefinitionDTO> copy = new ArrayList<>(configured);
            copy.sort(Comparator.comparingInt(RankDefinitionDTO::getLevel));
            return copy;
        }
        List<RankDefinitionDTO> list = new ArrayList<>();
        list.add(makeRank("beginner", 0, 1));
        list.add(makeRank("trainee", 1000, 2));
        list.add(makeRank("novice", 3000, 3));
        list.add(makeRank("apprentice", 6000, 4));
        list.add(makeRank("wood", 10000, 5));
        return list;
    }

    private RankDefinitionDTO makeRank(String key, int th, int level) {
        RankDefinitionDTO r = new RankDefinitionDTO();
        r.setKey(key);
        r.setThreshold(th);
        r.setLevel(level);
        return r;
    }

    public Map<String, Object> analyticsMonthly(Integer userId, Integer months) {
        int m = months == null ? 6 : Math.max(1, Math.min(24, months));
        List<String> labels = new ArrayList<>();
        List<Integer> points = new ArrayList<>();
        YearMonth now = YearMonth.now();
        for (int i = m - 1; i >= 0; i--) {
            YearMonth ym = now.minusMonths(i);
            labels.add(ym.toString());
            int sum = historyMapper.selectList(new LambdaQueryWrapper<TodoHistory>()
                            .eq(TodoHistory::getUserId, userId)
                            .apply("date_format(completed_at, '%Y-%m') = {0}", ym.toString()))
                    .stream().map(h -> Optional.ofNullable(h.getPointsApplied()).orElse(0)).reduce(0, Integer::sum);
            points.add(sum);
        }
        Map<String, Object> data = new HashMap<>();
        data.put("labels", labels);
        data.put("points", points);
        return data;
    }

    public Map<String, Object> analyticsSummary(Integer userId, String month) {
        if (month == null || month.isEmpty()) {
            throw new ToolsException(HttpStatus.BAD_REQUEST, "validation_error", "month is required");
        }
        List<TodoHistory> list = historyMapper.selectList(new LambdaQueryWrapper<TodoHistory>()
                .eq(TodoHistory::getUserId, userId)
                .apply("date_format(completed_at, '%Y-%m') = {0}", month));
        int total = list.stream().map(h -> Optional.ofNullable(h.getPointsApplied()).orElse(0)).reduce(0, Integer::sum);
        long completedCount = list.size();
        long successCount = list.stream().filter(h -> "success".equals(h.getStatus())).count();
        int successRate = completedCount == 0 ? 0 : (int) Math.round(100.0 * successCount / completedCount);
        Map<String, Object> data = new HashMap<>();
        data.put("month", month);
        data.put("totalPoints", total);
        data.put("completedCount", (int) completedCount);
        data.put("successRatePct", successRate);
        return data;
    }

    @Transactional
    public int seedDemo(Integer userId) {
        List<TodoDemoProperties.TaskDef> defs = demoProperties.getTasks();
        if (defs == null || defs.isEmpty()) {
            throw new ToolsException(HttpStatus.BAD_REQUEST, "config_missing", "No demo tasks configured");
        }
        int created = 0;
        for (TodoDemoProperties.TaskDef def : defs) {
            if (def.getTitle() == null || def.getTitle().trim().isEmpty()) {
                continue;
            }
            long exists = taskMapper.selectCount(new LambdaQueryWrapper<TodoTask>()
                    .eq(TodoTask::getUserId, userId)
                    .eq(TodoTask::getTitle, def.getTitle())
                    .isNull(TodoTask::getDeletedAt));
            if (exists > 0) {
                continue;
            }
            TodoTask task = new TodoTask();
            task.setId(String.valueOf(IdWorker.getId()));
            task.setUserId(userId);
            task.setTitle(def.getTitle());
            task.setDescription(def.getDescription());
            task.setSuccessPoints(Optional.ofNullable(def.getSuccessPoints()).orElse(10));
            task.setFailPoints(Optional.ofNullable(def.getFailPoints()).orElse(-5));
            task.setFrequency(Optional.ofNullable(def.getFrequency()).orElse("once"));
            if (def.getCustomDays() != null) {
                task.setCustomDays(def.getCustomDays());
            }
            task.setStatus("pending");
            task.setCreatedAt(LocalDateTime.now());
            task.setUpdatedAt(task.getCreatedAt());
            taskMapper.insert(task);
            created++;
        }
        return created;
    }
}
