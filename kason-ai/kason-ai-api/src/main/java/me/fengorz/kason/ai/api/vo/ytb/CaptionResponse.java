package me.fengorz.kason.ai.api.vo.ytb;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaptionResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String id;
    private String language;
    private String name;
    private String trackKind;
    private Boolean isAutoSynced;
    private Boolean isCC;
    private Boolean isDraft;
}