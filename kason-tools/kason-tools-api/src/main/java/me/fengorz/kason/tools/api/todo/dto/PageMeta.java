package me.fengorz.kason.tools.api.todo.dto;

import lombok.Data;

@Data
public class PageMeta {
    private Integer page;
    private Integer pageSize;
    private Long total;
}

