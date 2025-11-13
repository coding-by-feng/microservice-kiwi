package me.fengorz.kason.common.sdk.util.enumeration;

import me.fengorz.kason.common.sdk.enumeration.LanguageEnum;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EnumToMapConverterTest {

    @Test
    void testEnumToMap_Success() {
        // Convert LanguageEnum to Map
        Map<String, LanguageEnum> languageMap = EnumToMapConverter.enumToMap(LanguageEnum.class, "code");

        // Verify the Map
        assertNotNull(languageMap, "Map should not be null");
        assertEquals(LanguageEnum.values().length, languageMap.size(), "Map should contain all 14 LanguageEnum entries");

        // Verify specific entries
        assertEquals(LanguageEnum.EN, languageMap.get("EN"), "EN code should map to LanguageEnum.EN");
        assertEquals("English", languageMap.get("EN").getName(), "EN name should be English");

        assertEquals(LanguageEnum.ZH_CN, languageMap.get("ZH_CN"), "ZH_CN code should map to LanguageEnum.ZH_CN");
        assertEquals("Simplified Chinese", languageMap.get("ZH_CN").getName(), "ZH_CN name should be Simplified Chinese");

        assertEquals(LanguageEnum.JA, languageMap.get("JA"), "JA code should map to LanguageEnum.JA");
        assertEquals("Japanese", languageMap.get("JA").getName(), "JA name should be Japanese");

        assertEquals(LanguageEnum.RU, languageMap.get("RU"), "RU code should map to LanguageEnum.RU");
        assertEquals("Russian", languageMap.get("RU").getName(), "RU name should be Russian");
    }

    @Test
    void testEnumToMap_InvalidFieldName() {
        // Test with a non-existent field name
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            EnumToMapConverter.enumToMap(LanguageEnum.class, "invalidField");
        });

        String expectedMessage = "Field 'invalidField' not found in " + LanguageEnum.class.getName();
        assertEquals(expectedMessage, exception.getMessage(), "Exception message should match");
    }

}