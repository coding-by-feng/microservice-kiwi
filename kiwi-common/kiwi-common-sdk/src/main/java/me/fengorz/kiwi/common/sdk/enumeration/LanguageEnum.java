package me.fengorz.kiwi.common.sdk.enumeration;

import lombok.Getter;
import me.fengorz.kiwi.common.sdk.util.enumeration.EnumToMapConverter;

import java.util.Map;

@Getter
public enum LanguageEnum {

    EN("EN", "English"),
    ZH_CN("ZH_CN", "Simplified Chinese"),
    ZH_HK("ZH_HK", "Traditional Chinese"),
    ZH_TW("ZH_TW", "Traditional Chinese"),
    JA("JA", "Japanese"),
    KO("KO", "Korean"),
    ES("ES", "Spanish"),
    FR("FR", "French"),
    DE("DE", "German"),
    IT("IT", "Italian"),
    PT("PT", "Portuguese"),
    RU("RU", "Russian"),
    TH("TH", "Thai"),
    VI("VI", "Vietnamese"),
    ;

    private final String code;
    private final String name;

    LanguageEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public static Map<String, LanguageEnum> LANGUAGE_MAP = EnumToMapConverter.enumToMap(LanguageEnum.class, "code");

}
