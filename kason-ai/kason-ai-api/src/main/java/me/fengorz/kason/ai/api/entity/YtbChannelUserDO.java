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
 * User-channel subscription relation
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("ytb_channel_user")
public class YtbChannelUserDO implements Serializable {

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
     * Channel ID
     */
    private Long channelId;

    /**
     * Subscription time
     */
    private LocalDateTime createTime;

    /**
     * Whether the subscription is valid (1: valid, 0: invalid)
     */
    private Boolean ifValid;
}