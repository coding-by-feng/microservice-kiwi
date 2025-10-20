package me.fengorz.kiwi.tools.api.todo.dto;

import lombok.Data;

@Data
public class TaskCreateRequest {
    private String title;
    private String description;
    private Integer successPoints;
    private Integer failPoints;
    private String frequency;
    private Integer customDays;
}

