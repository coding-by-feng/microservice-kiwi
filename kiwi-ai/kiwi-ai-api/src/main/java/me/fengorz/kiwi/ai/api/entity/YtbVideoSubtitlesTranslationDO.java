package me.fengorz.kiwi.ai.api.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Subtitle translations
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("ytb_video_subtitles_translation")
public class YtbVideoSubtitlesTranslationDO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Primary key
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * Subtitle ID
     */
    private Long subtitlesId;

    /**
     * Language international standard code
     */
    private String lang;

    /**
     * Translation content
     */
    private String translation;

    /**
     * Translation type (1: not proofread, 2: proofread)
     */
    private Integer type;

    /**
     * Processing status (0: ready, 1: processing, 2: finish)
     */
    private Integer status;

    /**
     * Whether generated from AI (1: yes, 0: no)
     */
    private Boolean fromAi;

    /**
     * Record creation time
     */
    private LocalDateTime createTime;

    /**
     * Whether the translation is valid (1: valid, 0: invalid)
     */
    private Boolean ifValid;
}