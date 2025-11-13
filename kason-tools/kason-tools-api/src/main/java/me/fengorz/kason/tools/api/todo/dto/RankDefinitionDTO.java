package me.fengorz.kason.tools.api.todo.dto;

import lombok.Data;

@Data
public class RankDefinitionDTO {
    private String key;
    private Integer threshold;
    private Integer level;
}

