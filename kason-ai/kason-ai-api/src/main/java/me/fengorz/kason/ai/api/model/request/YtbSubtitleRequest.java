package me.fengorz.kason.ai.api.model.request;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class YtbSubtitleRequest {
    
    private String videoUrl;
    private String language;
    private String requestType; // "scrolling" or "translated"
    private Long timestamp;
    
    // Optional fields for future enhancements
    private String sessionId;
    private String userId;
    private Boolean enableCache;
    
    public YtbSubtitleRequest() {
        this.timestamp = System.currentTimeMillis();
        this.enableCache = true;
    }
}