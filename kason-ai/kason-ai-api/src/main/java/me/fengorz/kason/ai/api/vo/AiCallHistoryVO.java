package me.fengorz.kason.ai.api.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI call history VO for API response
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AiCallHistoryVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Record ID
     */
    private Long id;

    /**
     * AI URL (endpoint used for the request)
     */
    private String aiUrl;

    /**
     * The text prompt sent to AI
     */
    private String prompt;

    /**
     * AI prompt mode
     */
    private String promptMode;

    /**
     * Target language
     */
    private String targetLanguage;

    /**
     * Native language
     */
    private String nativeLanguage;

    /**
     * Request timestamp
     */
    private LocalDateTime timestamp;

    /**
     * Record creation time
     */
    private LocalDateTime createTime;

    /**
     * Whether this history item is marked favorite
     */
    private Boolean isFavorite;
}