package me.fengorz.kiwi.ai.api.enums;

/**
 * Enum for filtering AI call history records
 *
 * @author Kason Zhan
 * @date 26/07/2025
 */
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
    ALL("all", "Show all items"),

    /**
     * Show only favorite items
     */
    FAVORITE("favorite", "Show only favorite items");

    private final String code;
    private final String description;

    HistoryFilterEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

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
