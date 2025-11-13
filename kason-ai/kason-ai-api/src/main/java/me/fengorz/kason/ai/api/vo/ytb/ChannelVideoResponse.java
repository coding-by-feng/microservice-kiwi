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
public class ChannelVideoResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String videoId;
    private String title;
    private String description;
    private String publishedAt;
    private Long position;
    private Map<String, String> thumbnails;
}