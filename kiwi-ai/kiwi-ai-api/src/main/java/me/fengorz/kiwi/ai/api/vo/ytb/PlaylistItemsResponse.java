package me.fengorz.kiwi.ai.api.vo.ytb;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaylistItemsResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private List<ChannelVideoResponse> items;
    private String nextPageToken;
    private Long totalResults;
}