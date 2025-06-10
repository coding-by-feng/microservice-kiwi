package me.fengorz.kiwi.ai.api.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * WebSocket streaming request model for AI operations
 * 
 * @Author Kason Zhan
 * @Date 06/03/2025
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiStreamingRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Unique identifier for the request
     */
    private String requestId;

    /**
     * The text prompt to be processed by AI
     */
    private String prompt;

    /**
     * AI prompt mode (e.g., DIRECTLY_TRANSLATION, GRAMMAR_EXPLANATION)
     */
    private String promptMode;

    /**
     * Target language for translation or analysis
     */
    private String targetLanguage;

    /**
     * Native language (optional, for dual-language operations)
     */
    private String nativeLanguage;

    /**
     * Request timestamp
     */
    private Long timestamp;

    /**
     * Additional metadata (optional)
     */
    private String metadata;
}