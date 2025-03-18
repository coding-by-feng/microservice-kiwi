package me.fengorz.kiwi.ai.api.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class YtbSubtitlesVO implements Serializable {

    private static final long serialVersionUID = 8961036140529222053L;

    private String translatedOrRetouchedSubtitles;
    private String scrollingSubtitles;
    private String type;

}
