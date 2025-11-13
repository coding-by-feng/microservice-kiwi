package me.fengorz.kason.tools.config;

import me.fengorz.kason.tools.api.todo.dto.RankDefinitionDTO;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Binds todo.ranking.* properties to provide rank definitions from YAML.
 */
@Component
@ConfigurationProperties(prefix = "todo.ranking")
public class TodoRankingProperties {

    /**
     * List of rank definitions in ascending level order.
     */
    private List<RankDefinitionDTO> ranks = new ArrayList<>();

    public List<RankDefinitionDTO> getRanks() {
        return ranks;
    }

    public void setRanks(List<RankDefinitionDTO> ranks) {
        this.ranks = ranks;
    }
}

