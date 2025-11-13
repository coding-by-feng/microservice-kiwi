package me.fengorz.kason.ai.api.vo.ytb;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChannelDetailsResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String channelId;
    private String title;
    private String description;
    private String customUrl;
    private String publishedAt;
    private Long subscriberCount;
    private Long videoCount;
    private Long viewCount;
    private String uploadsPlaylistId;
    private Map<String, String> thumbnails;
}