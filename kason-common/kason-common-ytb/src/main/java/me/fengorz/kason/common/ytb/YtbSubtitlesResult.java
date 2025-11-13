package me.fengorz.kason.common.ytb;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class YtbSubtitlesResult implements Serializable {

    private static final long serialVersionUID = 4200784339674285787L;

    private String videoUrl;
    private SubtitleTypeEnum type;
    private String scrollingSubtitles;
    private Object pendingToBeTranslatedOrRetouchedSubtitles;
    private String langCode; // e.g. en, en-US, zh-CN, zh-Hans

}
