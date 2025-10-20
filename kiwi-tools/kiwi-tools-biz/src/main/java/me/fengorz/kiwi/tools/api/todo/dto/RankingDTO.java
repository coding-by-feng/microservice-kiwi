package me.fengorz.kiwi.tools.api.todo.dto;

import lombok.Data;

@Data
public class RankingDTO {
    private Integer totalPoints;
    private RankDefinitionDTO currentRank;
    private RankDefinitionDTO nextRank;
    private Double progressPct;
}

