package me.fengorz.kiwi.common.ytb;

public enum SubtitleTypeEnum {

    SMALL_AUTO_GENERATED_RETURN_STRING("auto_generated_return_string"),
    LARGE_AUTO_GENERATED_RETURN_LIST("auto_generated_vtt_return_list"),
    SMALL_PROFESSIONAL_RETURN_STRING("small_professional_return_string"),
    LARGE_PROFESSIONAL_RETURN_LIST("large_professional_return_list"),
    ;

    private final String value;

    SubtitleTypeEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
