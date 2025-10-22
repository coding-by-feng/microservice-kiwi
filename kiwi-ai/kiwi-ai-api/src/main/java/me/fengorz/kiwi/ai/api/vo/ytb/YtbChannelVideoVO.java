package me.fengorz.kiwi.ai.api.vo.ytb;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * YouTube video VO for API response
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class YtbChannelVideoVO implements Serializable {

    private static final long serialVersionUID = 2327558766516753470L;

    /**
     * Video ID
     */
    private Long id;

    /**
     * Video title
     */
    private String videoTitle;

    /**
     * video link
     */
    private String videoLink;

    /**
     * Video publication datetime
     */
    private LocalDateTime publishedAt;

    private Integer status;

    private Boolean favorited;

    private Long favoriteCount;

}