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
package me.fengorz.kiwi.domain.tools.dto.focus;

import me.fengorz.kiwi.domain.tools.entity.FocusSession;
import me.fengorz.kiwi.domain.tools.entity.FocusStats;
import me.fengorz.kiwi.domain.tools.entity.PlantedTree;

import java.util.List;
import java.util.stream.Collectors;

/**
 * DTO mapper for focus-related entities
 *
 * @author codingByFeng
 */
public class FocusDtoMapper {

    public static FocusSessionDTO toSessionDTO(FocusSession session) {
        if (session == null) {
            return null;
        }
        FocusSessionDTO dto = new FocusSessionDTO();
        dto.setId(session.getId());
        dto.setUserId(session.getUserId() != null ? String.valueOf(session.getUserId()) : null);
        dto.setDuration(session.getDuration());
        dto.setTreeType(session.getTreeType());
        dto.setPotentialPoints(session.getPotentialPoints());
        dto.setStartTime(session.getStartTime());
        dto.setEndTime(session.getEndTime());
        dto.setStatus(session.getStatus());
        dto.setCompletedAt(session.getCompletedAt());
        dto.setCancelledAt(session.getCancelledAt());
        dto.setFailedAt(session.getFailedAt());
        dto.setFailReason(session.getFailReason());
        dto.setPoints(session.getPoints());
        dto.setPenalty(session.getPenalty());
        dto.setCreatedAt(session.getCreatedAt());
        dto.setUpdatedAt(session.getUpdatedAt());
        return dto;
    }

    public static PlantedTreeDTO toTreeDTO(PlantedTree tree) {
        if (tree == null) {
            return null;
        }
        PlantedTreeDTO dto = new PlantedTreeDTO();
        dto.setId(tree.getId());
        dto.setType(tree.getTreeType());
        dto.setColor(tree.getColor());
        dto.setPlantedAt(tree.getPlantedAt());
        dto.setDuration(tree.getDuration());
        dto.setPoints(tree.getPoints());
        dto.setPosition(tree.getPosition());
        return dto;
    }

    public static List<PlantedTreeDTO> toTreeDTOList(List<PlantedTree> trees) {
        if (trees == null) {
            return List.of();
        }
        return trees.stream()
                .map(FocusDtoMapper::toTreeDTO)
                .collect(Collectors.toList());
    }

    public static FocusStatsDTO toStatsDTO(FocusStats stats, List<PlantedTree> trees) {
        FocusStatsDTO dto = new FocusStatsDTO();
        if (stats != null) {
            dto.setTodayTrees(stats.getTodayTrees() != null ? stats.getTodayTrees() : 0);
            dto.setTodayMinutes(stats.getTodayMinutes() != null ? stats.getTodayMinutes() : 0);
            dto.setCurrentStreak(stats.getCurrentStreak() != null ? stats.getCurrentStreak() : 0);
            dto.setTotalTrees(stats.getTotalTrees() != null ? stats.getTotalTrees() : 0);
            dto.setTotalMinutes(stats.getTotalMinutes() != null ? stats.getTotalMinutes() : 0);
            dto.setTotalPoints(stats.getTotalPoints() != null ? stats.getTotalPoints() : 0);
            dto.setLastSessionDate(stats.getLastSessionDate() != null ? stats.getLastSessionDate().toString() : null);
        } else {
            dto.setTodayTrees(0);
            dto.setTodayMinutes(0);
            dto.setCurrentStreak(0);
            dto.setTotalTrees(0);
            dto.setTotalMinutes(0);
            dto.setTotalPoints(0);
        }
        dto.setPlantedTrees(toTreeDTOList(trees));
        return dto;
    }
}
