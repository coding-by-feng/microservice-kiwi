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
 * Video subtitles
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("ytb_video_subtitles")
public class YtbVideoSubtitlesDO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Primary key
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * Video ID
     */
    private Long videoId;

    /**
     * Subtitle type (1: professional, 2: auto-generated)
     */
    private Integer type;

    /**
     * Processing status (0: ready, 1: processing, 2: finish)
     */
    private Integer status;

    /**
     * Subtitle text content
     */
    private String subtitlesText;

    /**
     * Record creation time
     */
    private LocalDateTime createTime;

    /**
     * Whether the subtitle is valid (1: valid, 0: invalid)
     */
    private Boolean ifValid;
}