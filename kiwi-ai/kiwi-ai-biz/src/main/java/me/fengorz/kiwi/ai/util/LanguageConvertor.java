package me.fengorz.kiwi.ai.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.sdk.enumeration.LanguageEnum;
import me.fengorz.kiwi.common.sdk.exception.BadRequestException;

import java.util.Optional;

/**
 * @Author Kason Zhan
 * @Date 06/03/2025
 */
@Slf4j
@UtilityClass
public class LanguageConvertor {

    public static LanguageEnum convertLanguageToEnum(String language) {
        return Optional.ofNullable(LanguageEnum.LANGUAGE_MAP.get(language)).orElseThrow(() -> {
            log.error("Unsupported language: {}", language);
            return new BadRequestException("Unsupported language: " + language);
        });
    }

}
