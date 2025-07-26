package me.fengorz.kiwi.ai.api.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Enum for filtering AI call history records
 * 
 * @author Kason Zhan
 * @date 26/07/2025
 */
@Getter
@AllArgsConstructor
public enum HistoryFilterEnum {
    
    /**
     * Show only normal (non-archived) items
     */
    NORMAL("normal", "Show only normal items"),
    
    /**
     * Show only archived items
     */
    ARCHIVED("archived", "Show only archived items"),
    
    /**
     * Show all items (both normal and archived)
     */
    ALL("all", "Show all items");
    
    private final String code;
    private final String description;
    
    /**
     * Convert string to enum
     */
    public static HistoryFilterEnum fromCode(String code) {
        if (code == null) {
            return NORMAL; // Default to normal items
        }
        
        for (HistoryFilterEnum filter : values()) {
            if (filter.getCode().equalsIgnoreCase(code)) {
                return filter;
            }
        }
        
        return NORMAL; // Default to normal items if invalid code
    }
}
