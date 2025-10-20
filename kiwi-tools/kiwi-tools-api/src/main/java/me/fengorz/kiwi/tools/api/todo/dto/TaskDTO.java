package me.fengorz.kiwi.tools.api.todo.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
public class TaskDTO {
    private String id;
    private String userId; // expose as string
    private String title;
    private String description;
    private Integer successPoints;
    private Integer failPoints;
    private String frequency;
    private Integer customDays;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private Map<String,Object> metadata;
}

