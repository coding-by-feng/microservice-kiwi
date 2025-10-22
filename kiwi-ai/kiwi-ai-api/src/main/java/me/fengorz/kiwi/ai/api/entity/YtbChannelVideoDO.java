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
 * YouTube channel videos
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("ytb_channel_video")
public class YtbChannelVideoDO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Primary key
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * Channel ID
     */
    private Long channelId;

    /**
     * Video title
     */
    private String videoTitle;

    /**
     * Video link
     */
    private String videoLink;

    /**
     * Video publication datetime (UTC or server local time based on storage)
     */
    private LocalDateTime publishedAt;

    /**
     * Processing status (0: ready, 1: processing, 2: finish)
     */
    private Integer status;

    /**
     * Record creation time
     */
    private LocalDateTime createTime;

    /**
     * Whether the video is valid (1: valid, 0: invalid)
     */
    private Boolean ifValid;
}