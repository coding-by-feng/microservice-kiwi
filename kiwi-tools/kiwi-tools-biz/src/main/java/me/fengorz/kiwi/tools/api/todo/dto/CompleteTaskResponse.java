package me.fengorz.kiwi.tools.api.todo.dto;

import lombok.Data;

@Data
public class CompleteTaskResponse {
    private java.util.Map<String, Object> data; // keys: task, history, ranking
}

