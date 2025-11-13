package me.fengorz.kason.tools.api.todo.dto;

import lombok.Data;

@Data
public class DeleteOkWithRankingMetaResponse {
    private java.util.Map<String, Boolean> data;
    private java.util.Map<String, Object> meta;
}

