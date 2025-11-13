package me.fengorz.kason.ai.api.vo;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
public class SpeechToTextVO implements Serializable {

    private static final long serialVersionUID = 7911088270740445658L;

    private String transcribedText;
    private String language;

}