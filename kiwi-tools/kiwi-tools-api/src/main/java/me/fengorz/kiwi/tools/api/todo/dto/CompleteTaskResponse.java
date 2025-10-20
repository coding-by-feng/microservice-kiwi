package me.fengorz.kiwi.tools.api.todo.dto;

import lombok.Data;

import java.util.Map;

@Data
public class CompleteTaskResponse {
    // keys: task, history, ranking
    private Map<String, Object> data;
}
