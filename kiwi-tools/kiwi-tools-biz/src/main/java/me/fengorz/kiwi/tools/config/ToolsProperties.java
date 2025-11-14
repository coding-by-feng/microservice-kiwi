package me.fengorz.kiwi.tools.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;

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
    private List<String> allowedStatuses = Arrays.asList(
        "glass_ordered",
        "doors_windows_produced",
        "doors_windows_delivered",
        "doors_windows_installed",
        "final_payment_received"
    );

    /**
     * Maximum allowed single photo upload size. Defaults to 10MB.
     * Can be overridden via YAML: tools.photo-max-size: 5MB
     */
    private DataSize photoMaxSize = DataSize.ofMegabytes(10);

    /**
     * Maximum allowed single video upload size. Defaults to 30MB.
     * Can be overridden via YAML: tools.video-max-size: 30MB
     */
    private DataSize videoMaxSize = DataSize.ofMegabytes(30);
}
