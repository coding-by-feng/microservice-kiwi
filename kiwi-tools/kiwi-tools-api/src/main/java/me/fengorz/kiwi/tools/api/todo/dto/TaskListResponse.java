package me.fengorz.kiwi.tools.api.todo.dto;

import lombok.Data;

@Data
public class TaskListResponse {
    private java.util.List<TaskDTO> data;
    private PageMeta meta;
}

