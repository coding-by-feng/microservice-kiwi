/*
 * Copyright [2019~2025] [codingByFeng]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.fengorz.kiwi.common.enumeration;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * Language Enumeration
 *
 * @author codingByFeng
 */
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

    public static final Map<String, LanguageEnum> LANGUAGE_MAP;

    static {
        LANGUAGE_MAP = new HashMap<>();
        for (LanguageEnum lang : values()) {
            LANGUAGE_MAP.put(lang.getCode(), lang);
        }
    }

    public static LanguageEnum fromCode(String code) {
        return LANGUAGE_MAP.get(code);
    }
}
