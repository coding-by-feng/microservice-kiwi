package me.fengorz.kason.tools.api.todo.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class HistoryRecordDTO {
    private String id;
    private String userId;
    private String taskId;
    private String title;
    private String description;
    private Integer successPoints;
    private Integer failPoints;
    private String status;
    private Integer pointsApplied;
    private LocalDateTime completedAt;
}

