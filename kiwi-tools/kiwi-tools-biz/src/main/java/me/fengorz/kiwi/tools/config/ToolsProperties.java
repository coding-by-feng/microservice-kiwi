package me.fengorz.kiwi.tools.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "tools")
public class ToolsProperties {
    /**
     * Allowed CORS origins
     */
    private List<String> corsAllowedOrigins = Arrays.asList("http://localhost:5173");

    /**
     * Static upload directory on local disk
     */
    private String uploadDir = "uploads";

    /**
     * Public base URL to build absolute URLs for photo_url; if blank will derive from request
     */
    private String publicBaseUrl = "";

    /** Rate limit per IP per minute for write operations */
    private int writeRateLimitPerMin = 60;

    /** Rate limit per IP per minute for upload operations */
    private int uploadRateLimitPerMin = 30;

    /** Idempotency key TTL minutes for POST /api/projects */
    private int idempotencyTtlMinutes = 60;

    /** Allowed status values (codes) */
    private List<String> allowedStatuses = Arrays.asList("not_started", "in_progress", "completed");
}
