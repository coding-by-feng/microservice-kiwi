package me.fengorz.kason.tools.api.todo.dto;

import lombok.Data;

@Data
public class TrashListResponse {
    private java.util.List<TrashItemDTO> data;
    private PageMeta meta;
}

