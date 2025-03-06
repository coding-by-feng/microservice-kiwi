package me.fengorz.kiwi.ai;

import me.fengorz.kiwi.common.sdk.enumeration.AiPromptModeEnum;
import me.fengorz.kiwi.common.sdk.enumeration.LanguageEnum;

/**
 * @Author Kason Zhan
 * @Date 06/03/2025 06/03/2025
 */
public interface AiService {

    String translate(String prompt, AiPromptModeEnum promptMode, LanguageEnum language);

}
