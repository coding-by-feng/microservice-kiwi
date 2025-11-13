package me.fengorz.kason.ai.api.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI call history entity
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("ai_call_history")
public class AiCallHistoryDO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Primary key
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * User ID
     */
    private Long userId;

    /**
     * AI URL (endpoint used for the request)
     */
    private String aiUrl;

    /**
     * The text prompt sent to AI
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
    private LocalDateTime timestamp;

    /**
     * Soft delete flag (0: not deleted, 1: deleted)
     */
    private Boolean isDelete;

    /**
     * Archive flag (0: not archived, 1: archived)
     */
    private Boolean isArchive;

    /**
     * Favorite flag (0: not favorite, 1: favorite)
     */
    private Boolean isFavorite;

    /**
     * Record creation time
     */
    private LocalDateTime createTime;

    /**
     * Record update time
     */
    private LocalDateTime updateTime;
}