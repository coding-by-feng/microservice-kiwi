package me.fengorz.kiwi.domain.notes.vo;

import lombok.Builder;
import lombok.Data;

/**
 * Value object representing an image generation style for API responses.
 */
@Data
@Builder
public class ImageStyleVO {

    /**
     * Unique identifier for the style (e.g., "ghibli", "cyberpunk")
     */
    private String id;

    /**
     * Display name for the style (e.g., "Studio Ghibli")
     */
    private String name;

    /**
     * Brief description of the style for UI display
     */
    private String description;

    /**
     * Optional preview image URL for the style
     */
    private String previewUrl;

    /**
     * Display order for sorting in the UI
     */
    private int sortOrder;
}
