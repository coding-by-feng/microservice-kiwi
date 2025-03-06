package me.fengorz.kiwi.ai;

import lombok.experimental.UtilityClass;
import me.fengorz.kiwi.common.sdk.enumeration.LanguageEnum;
import me.fengorz.kiwi.common.sdk.exception.BadRequestException;

import java.util.Optional;

/**
 * @Author Kason Zhan
 * @Date 06/03/2025
 */
@UtilityClass
public class LanguageConvertor {

    public static LanguageEnum convertLanguageToEnum(String language) {
        return Optional.ofNullable(LanguageEnum.LANGUAGE_MAP.get(language)).orElseThrow(() -> new BadRequestException("Unsupported language: " + language));
    }

}
