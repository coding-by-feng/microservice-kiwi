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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.domain.tools.dto.PageMeta;
import me.fengorz.kiwi.domain.tools.dto.focus.*;
import me.fengorz.kiwi.domain.tools.entity.FocusSession;
import me.fengorz.kiwi.domain.tools.entity.FocusStats;
import me.fengorz.kiwi.domain.tools.entity.PlantedTree;
import me.fengorz.kiwi.domain.tools.exception.ToolsException;
import me.fengorz.kiwi.domain.tools.mapper.FocusSessionMapper;
import me.fengorz.kiwi.domain.tools.mapper.FocusStatsMapper;
import me.fengorz.kiwi.domain.tools.mapper.PlantedTreeMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Focus Timer Service
 *
 * @author codingByFeng
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FocusService {

    private final FocusSessionMapper sessionMapper;
    private final FocusStatsMapper statsMapper;
    private final PlantedTreeMapper treeMapper;

    private static final Set<String> VALID_TREE_TYPES = Set.of(
            "oak", "pine", "cherry", "maple", "willow", "sakura"
    );

    private static final Map<String, String> TREE_COLORS = Map.of(
            "oak", "#4CAF50",
            "pine", "#1B5E20",
            "cherry", "#E91E63",
            "maple", "#FF5722",
            "willow", "#8BC34A",
            "sakura", "#F48FB1"
    );

    private static final int POINTS_PER_30_MIN = 100;
    private static final int DEFAULT_PENALTY = 100;
    private static final int GRID_SIZE = 48;

    // ==================== GET STATS ====================

    @Transactional(readOnly = true)
    public FocusStatsDTO getStats(Integer userId) {
        FocusStats stats = getOrCreateStats(userId);
        resetDailyStatsIfNeeded(stats);

        List<PlantedTree> trees = getPlantedTrees(userId, GRID_SIZE, 0);
        return FocusDtoMapper.toStatsDTO(stats, trees);
    }

    // ==================== CREATE SESSION ====================

    @Transactional
    public Map<String, Object> createSession(Integer userId, CreateSessionRequest request) {
        validateCreateRequest(request);
        checkNoActiveSession(userId);

        LocalDateTime now = LocalDateTime.now();
        int potentialPoints = request.getPotentialPoints() != null
                ? request.getPotentialPoints()
                : calculatePoints(request.getDuration());

        FocusSession session = new FocusSession();
        session.setUserId(userId);
        session.setDuration(request.getDuration());
        session.setTreeType(request.getTreeType().toLowerCase());
        session.setPotentialPoints(potentialPoints);
        session.setStartTime(now);
        session.setStatus("active");
        session.setPoints(0);
        session.setPenalty(0);
        session.setCreatedAt(now);
        session.setUpdatedAt(now);
        sessionMapper.insert(session);

        Map<String, Object> result = new HashMap<>();
        result.put("session", FocusDtoMapper.toSessionDTO(session));
        return result;
    }

    // ==================== COMPLETE SESSION ====================

    @Transactional
    public Map<String, Object> completeSession(Integer userId, String sessionId, CompleteSessionRequest request) {
        FocusSession session = getSessionByIdAndUser(sessionId, userId);
        validateActiveSession(session);

        LocalDateTime now = LocalDateTime.now();

        int pointsEarned = request != null && request.getPoints() != null
                ? request.getPoints()
                : calculatePoints(session.getDuration());

        session.setStatus("completed");
        session.setEndTime(now);
        session.setCompletedAt(now);
        session.setPoints(pointsEarned);
        session.setUpdatedAt(now);
        sessionMapper.updateById(session);

        PlantedTree tree = plantTree(userId, session, pointsEarned);

        FocusStats stats = getOrCreateStats(userId);
        resetDailyStatsIfNeeded(stats);

        stats.setTodayTrees(stats.getTodayTrees() + 1);
        stats.setTodayMinutes(stats.getTodayMinutes() + session.getDuration());
        stats.setTotalTrees(stats.getTotalTrees() + 1);
        stats.setTotalMinutes(stats.getTotalMinutes() + session.getDuration());
        stats.setTotalPoints(stats.getTotalPoints() + pointsEarned);
        updateStreak(stats, now.toLocalDate());
        stats.setUpdatedAt(now);
        statsMapper.updateById(stats);

        List<PlantedTree> trees = getPlantedTrees(userId, GRID_SIZE, 0);

        Map<String, Object> result = new HashMap<>();
        result.put("session", FocusDtoMapper.toSessionDTO(session));
        result.put("stats", FocusDtoMapper.toStatsDTO(stats, trees));
        result.put("plantedTree", FocusDtoMapper.toTreeDTO(tree));
        return result;
    }

    // ==================== FAIL SESSION ====================

    @Transactional
    public Map<String, Object> failSession(Integer userId, String sessionId, FailSessionRequest request) {
        FocusSession session = getSessionByIdAndUser(sessionId, userId);
        validateActiveSession(session);

        LocalDateTime now = LocalDateTime.now();

        String reason = request != null && request.getReason() != null
                ? request.getReason()
                : "left_page";
        int penalty = request != null && request.getPenalty() != null
                ? request.getPenalty()
                : DEFAULT_PENALTY;

        session.setStatus("failed");
        session.setEndTime(now);
        session.setFailedAt(now);
        session.setFailReason(reason);
        session.setPenalty(penalty);
        session.setPoints(0);
        session.setUpdatedAt(now);
        sessionMapper.updateById(session);

        FocusStats stats = getOrCreateStats(userId);
        resetDailyStatsIfNeeded(stats);

        int newTotalPoints = Math.max(0, stats.getTotalPoints() - penalty);
        stats.setTotalPoints(newTotalPoints);
        stats.setUpdatedAt(now);
        statsMapper.updateById(stats);

        List<PlantedTree> trees = getPlantedTrees(userId, GRID_SIZE, 0);

        Map<String, Object> result = new HashMap<>();
        result.put("session", FocusDtoMapper.toSessionDTO(session));
        result.put("stats", FocusDtoMapper.toStatsDTO(stats, trees));
        return result;
    }

    // ==================== CANCEL SESSION ====================

    @Transactional
    public Map<String, Object> cancelSession(Integer userId, String sessionId) {
        FocusSession session = getSessionByIdAndUser(sessionId, userId);
        validateActiveSession(session);

        LocalDateTime now = LocalDateTime.now();

        session.setStatus("cancelled");
        session.setEndTime(now);
        session.setCancelledAt(now);
        session.setFailReason("give_up");
        session.setPenalty(DEFAULT_PENALTY);
        session.setPoints(0);
        session.setUpdatedAt(now);
        sessionMapper.updateById(session);

        FocusStats stats = getOrCreateStats(userId);
        resetDailyStatsIfNeeded(stats);

        int newTotalPoints = Math.max(0, stats.getTotalPoints() - DEFAULT_PENALTY);
        stats.setTotalPoints(newTotalPoints);
        stats.setUpdatedAt(now);
        statsMapper.updateById(stats);

        List<PlantedTree> trees = getPlantedTrees(userId, GRID_SIZE, 0);

        Map<String, Object> result = new HashMap<>();
        result.put("session", FocusDtoMapper.toSessionDTO(session));
        result.put("stats", FocusDtoMapper.toStatsDTO(stats, trees));
        return result;
    }

    // ==================== LIST SESSIONS ====================

    @Transactional(readOnly = true)
    public Map<String, Object> listSessions(Integer userId, Integer page, Integer pageSize,
                                            String status, String startDate, String endDate, String treeType) {
        int p = page == null || page < 1 ? 1 : page;
        int sz = pageSize == null || pageSize < 1 ? 20 : Math.min(pageSize, 100);

        LambdaQueryWrapper<FocusSession> qw = new LambdaQueryWrapper<>();
        qw.eq(FocusSession::getUserId, userId);

        if (status != null && !status.isEmpty()) {
            qw.eq(FocusSession::getStatus, status);
        }
        if (startDate != null && !startDate.isEmpty()) {
            qw.ge(FocusSession::getStartTime, LocalDate.parse(startDate).atStartOfDay());
        }
        if (endDate != null && !endDate.isEmpty()) {
            qw.le(FocusSession::getStartTime, LocalDate.parse(endDate).plusDays(1).atStartOfDay());
        }
        if (treeType != null && !treeType.isEmpty()) {
            qw.eq(FocusSession::getTreeType, treeType.toLowerCase());
        }

        qw.orderByDesc(FocusSession::getStartTime);

        Page<FocusSession> mp = new Page<>(p, sz);
        sessionMapper.selectPage(mp, qw);

        List<FocusSessionDTO> sessionDTOs = new ArrayList<>();
        for (FocusSession session : mp.getRecords()) {
            sessionDTOs.add(FocusDtoMapper.toSessionDTO(session));
        }

        PageMeta meta = new PageMeta();
        meta.setPage(p);
        meta.setPageSize(sz);
        meta.setTotal(mp.getTotal());

        Map<String, Object> result = new HashMap<>();
        result.put("data", sessionDTOs);
        result.put("meta", meta);
        return result;
    }

    // ==================== GET FOREST ====================

    @Transactional(readOnly = true)
    public Map<String, Object> getForest(Integer userId, Integer limit, Integer offset) {
        int lim = limit == null || limit < 1 ? GRID_SIZE : Math.min(limit, 100);
        int off = offset == null || offset < 0 ? 0 : offset;

        List<PlantedTree> trees = getPlantedTrees(userId, lim, off);
        List<PlantedTreeDTO> treeDTOs = FocusDtoMapper.toTreeDTOList(trees);

        LambdaQueryWrapper<PlantedTree> countQw = new LambdaQueryWrapper<>();
        countQw.eq(PlantedTree::getUserId, userId);
        long totalTrees = treeMapper.selectCount(countQw);

        Map<String, Object> meta = new HashMap<>();
        meta.put("totalTrees", totalTrees);
        meta.put("gridSize", GRID_SIZE);

        Map<String, Object> result = new HashMap<>();
        result.put("trees", treeDTOs);
        result.put("meta", meta);
        return result;
    }

    // ==================== HELPER METHODS ====================

    private void validateCreateRequest(CreateSessionRequest request) {
        if (request.getDuration() == null || request.getDuration() < 1 || request.getDuration() > 180) {
            throw new ToolsException(HttpStatus.BAD_REQUEST, "validation_error",
                    "Duration must be between 1 and 180 minutes");
        }
        if (request.getTreeType() == null || !VALID_TREE_TYPES.contains(request.getTreeType().toLowerCase())) {
            throw new ToolsException(HttpStatus.BAD_REQUEST, "validation_error",
                    "Invalid tree type. Must be one of: oak, pine, cherry, maple, willow, sakura");
        }
    }

    private void checkNoActiveSession(Integer userId) {
        LambdaQueryWrapper<FocusSession> qw = new LambdaQueryWrapper<>();
        qw.eq(FocusSession::getUserId, userId)
          .eq(FocusSession::getStatus, "active");
        if (sessionMapper.selectCount(qw) > 0) {
            throw new ToolsException(HttpStatus.BAD_REQUEST, "active_session_exists",
                    "An active focus session already exists");
        }
    }

    private FocusSession getSessionByIdAndUser(String sessionId, Integer userId) {
        FocusSession session = sessionMapper.selectById(sessionId);
        if (session == null || !Objects.equals(session.getUserId(), userId)) {
            throw new ToolsException(HttpStatus.NOT_FOUND, "not_found", "Focus session not found");
        }
        return session;
    }

    private void validateActiveSession(FocusSession session) {
        if (!"active".equals(session.getStatus())) {
            if ("completed".equals(session.getStatus())) {
                throw new ToolsException(HttpStatus.BAD_REQUEST, "already_completed",
                        "Session has already been completed");
            }
            throw new ToolsException(HttpStatus.BAD_REQUEST, "invalid_status",
                    "Session is not active");
        }
    }

    private FocusStats getOrCreateStats(Integer userId) {
        LambdaQueryWrapper<FocusStats> qw = new LambdaQueryWrapper<>();
        qw.eq(FocusStats::getUserId, userId);
        FocusStats stats = statsMapper.selectOne(qw);

        if (stats == null) {
            LocalDateTime now = LocalDateTime.now();
            stats = new FocusStats();
            stats.setUserId(userId);
            stats.setTodayTrees(0);
            stats.setTodayMinutes(0);
            stats.setCurrentStreak(0);
            stats.setTotalTrees(0);
            stats.setTotalMinutes(0);
            stats.setTotalPoints(0);
            stats.setStatsDate(LocalDate.now());
            stats.setCreatedAt(now);
            stats.setUpdatedAt(now);
            statsMapper.insert(stats);
        }
        return stats;
    }

    private void resetDailyStatsIfNeeded(FocusStats stats) {
        LocalDate today = LocalDate.now();
        if (stats.getStatsDate() == null || !stats.getStatsDate().equals(today)) {
            stats.setTodayTrees(0);
            stats.setTodayMinutes(0);
            stats.setStatsDate(today);
            stats.setUpdatedAt(LocalDateTime.now());
            statsMapper.updateById(stats);
        }
    }

    private int calculatePoints(int durationMinutes) {
        return (int) Math.round((double) durationMinutes / 30 * POINTS_PER_30_MIN);
    }

    private void updateStreak(FocusStats stats, LocalDate focusDate) {
        LocalDate lastFocus = stats.getLastSessionDate();

        if (lastFocus == null) {
            stats.setCurrentStreak(1);
        } else if (lastFocus.equals(focusDate)) {
            // Already focused today, no change
        } else if (lastFocus.plusDays(1).equals(focusDate)) {
            stats.setCurrentStreak(stats.getCurrentStreak() + 1);
        } else {
            stats.setCurrentStreak(1);
        }

        stats.setLastSessionDate(focusDate);
    }

    private PlantedTree plantTree(Integer userId, FocusSession session, int points) {
        int nextPosition = findNextGridPosition(userId);
        String color = TREE_COLORS.getOrDefault(session.getTreeType(), "#4CAF50");

        LocalDateTime now = LocalDateTime.now();
        PlantedTree tree = new PlantedTree();
        tree.setUserId(userId);
        tree.setSessionId(session.getId());
        tree.setTreeType(session.getTreeType());
        tree.setColor(color);
        tree.setPlantedAt(now);
        tree.setDuration(session.getDuration());
        tree.setPoints(points);
        tree.setPosition(nextPosition);
        tree.setCreatedAt(now);
        treeMapper.insert(tree);
        return tree;
    }

    private int findNextGridPosition(Integer userId) {
        LambdaQueryWrapper<PlantedTree> qw = new LambdaQueryWrapper<>();
        qw.eq(PlantedTree::getUserId, userId)
          .select(PlantedTree::getPosition);
        List<PlantedTree> trees = treeMapper.selectList(qw);

        Set<Integer> usedPositions = new HashSet<>();
        for (PlantedTree t : trees) {
            if (t.getPosition() != null) {
                usedPositions.add(t.getPosition());
            }
        }

        for (int i = 0; i < GRID_SIZE; i++) {
            if (!usedPositions.contains(i)) {
                return i;
            }
        }

        return trees.size() % GRID_SIZE;
    }

    private List<PlantedTree> getPlantedTrees(Integer userId, int limit, int offset) {
        LambdaQueryWrapper<PlantedTree> qw = new LambdaQueryWrapper<>();
        qw.eq(PlantedTree::getUserId, userId)
          .orderByDesc(PlantedTree::getPlantedAt)
          .last("LIMIT " + limit + " OFFSET " + offset);
        return treeMapper.selectList(qw);
    }
}
