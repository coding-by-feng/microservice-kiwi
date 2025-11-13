package me.fengorz.kason.tools.api.todo.dto;

import lombok.Data;

import java.util.Map;

@Data
public class CompleteTaskResponse {
    // keys: task, history, ranking
    private Map<String, Object> data;
}
