package me.fengorz.kiwi.common.ytb;

import lombok.Getter;

@Getter
public enum SubtitleTypeEnum {

    SMALL_AUTO_GENERATED_VTT_RETURN_STRING("auto_generated_vtt_return_string"),
    LARGE_AUTO_GENERATED_VTT_RETURN_LIST("auto_generated_vtt_return_list"),
    SMALL_PROFESSIONAL_SRT_RETURN_STRING("small_professional_srt_return_string"),
    LARGE_PROFESSIONAL_SRT_RETURN_LIST("large_professional_srt_return_list"),
    ;

    private final String value;

    SubtitleTypeEnum(String value) {
        this.value = value;
    }
}
