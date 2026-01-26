package me.fengorz.kiwi.domain.notes.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration properties for predefined image generation styles.
 * Styles are configured in application.yml under kiwi.notes.image-styles
 */
@Data
@Component
@ConfigurationProperties(prefix = "kiwi.notes")
public class NotesImageStyleProperties {

    /**
     * List of predefined image generation styles available for notes.
     */
    private List<ImageStyle> imageStyles = new ArrayList<>();

    @Data
    public static class ImageStyle {
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
         * The full prompt text to be appended to the image generation request.
         * This is the detailed style instruction sent to the AI model.
         */
        private String prompt;

        /**
         * Optional preview image URL for the style
         */
        private String previewUrl;

        /**
         * Whether this style is enabled and available for selection
         */
        private boolean enabled = true;

        /**
         * Display order for sorting in the UI
         */
        private int sortOrder = 0;
    }
}
