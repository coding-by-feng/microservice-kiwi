package me.fengorz.kason.tools.api.todo.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TrashItemDTO {
    private String id;
    private String title;
    private String description;
    private Integer successPoints;
    private Integer failPoints;
    private String frequency;
    private Integer customDays;
    private String status;
    private LocalDateTime originalDate;
    private LocalDateTime deletedDate;
}

