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
 * YouTube channel information
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("ytb_channel")
public class YtbChannelDO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Primary key
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * YouTube channel link
     */
    private String channelLink;

    /**
     * YouTube channel name
     */
    private String channelName;

    /**
     * Processing status (0: ready, 1: processing, 2: finish)
     */
    private Integer status;

    /**
     * Record creation time
     */
    private LocalDateTime createTime;

    /**
     * Whether the channel is valid (1: valid, 0: invalid)
     */
    private Boolean ifValid;
}