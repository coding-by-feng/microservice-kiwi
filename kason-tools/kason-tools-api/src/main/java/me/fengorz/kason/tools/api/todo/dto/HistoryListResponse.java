package me.fengorz.kason.tools.api.todo.dto;

import lombok.Data;

@Data
public class HistoryListResponse {
    private java.util.List<HistoryRecordDTO> data;
    private Object meta;
}

