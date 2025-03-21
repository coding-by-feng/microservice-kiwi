package me.fengorz.kiwi.ai.api.vo.ytb;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

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
    private Integer id;

    /**
     * Video title
     */
    private String videoTitle;

    /**
     * video link
     */
    private String videoLink;

    private String status;

}