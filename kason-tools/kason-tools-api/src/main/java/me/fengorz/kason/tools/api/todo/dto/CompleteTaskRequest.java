package me.fengorz.kason.tools.api.todo.dto;

import lombok.Data;

@Data
public class CompleteTaskRequest {
    private String status; // success|fail
}

