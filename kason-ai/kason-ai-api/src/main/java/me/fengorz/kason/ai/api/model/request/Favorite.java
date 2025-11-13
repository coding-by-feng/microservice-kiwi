package me.fengorz.kason.ai.api.model.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload to set favorite status on a resource (e.g., AI call history item)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Favorite {
    /**
     * Whether the item is marked as favorite
     */
    private Boolean favorite;
}

