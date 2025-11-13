package me.fengorz.kason.common.sdk.enumeration;

import lombok.Getter;
import me.fengorz.kason.common.sdk.util.enumeration.EnumToMapConverter;

import java.util.Map;

@Getter
public enum LanguageEnum {

    EN("EN", "English"),
    ZH_CN("ZH_CN", "Simplified Chinese"),
    ZH_HK("ZH_HK", "Traditional HK Chinese"),
    ZH_TW("ZH_TW", "Traditional TW Chinese"),
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
    PL("PL", "Polish"),
    SV("SV", "Swedish"),
    FI("FI", "Finnish"),
    NO("NO", "Norwegian"),
    DA("DA", "Danish"),
    CS("CS", "Czech"),
    EL("EL", "Greek"),
    HE("HE", "Hebrew"),
    LT("LT", "Lithuanian"),
    LV("LV", "Latvian"),
    SK("SK", "Slovak"),
    UK("UK", "Ukrainian"),
    AR("AR", "Arabic"),
    BG("BG", "Bulgarian"),
    HR("HR", "Croatian"),
    CY("CY", "Welsh"),
    EE("EE", "Estonian"),
    FIU("FIU", "Finno-Ugric"),
    GA("GA", "Irish"),
    IS("IS", "Icelandic"),
    MK("MK", "Macedonian"),
    MT("MT", "Maltese"),
    NB("NB", "Norwegian Bokmål"),
    NN("NN", "Norwegian Nynorsk"),
    RO("RO", "Romanian"),
    SE("SE", "Swedish"),
    UZ("UZ", "Uzbek"),
    VE("VE", "Venda"),
    XH("XH", "Xhosa"),
    ZU("ZU", "Zulu"),
    AL("AL", "Albanian"),
    AM("AM", "Armenian"),
    AZ("AZ", "Azerbaijani"),
    BS("BS", "Bosnian"),
    CA("CA", "Catalan"),
    NONE("NONE", "NONE");

    private final String code;
    private final String name;

    LanguageEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public static final Map<String, LanguageEnum> LANGUAGE_MAP = EnumToMapConverter.enumToMap(LanguageEnum.class, "code");

}
