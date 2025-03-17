package me.fengorz.kiwi.ai;

import me.fengorz.kiwi.common.sdk.enumeration.AiPromptModeEnum;
import me.fengorz.kiwi.common.sdk.enumeration.LanguageEnum;

import java.util.List;

/**
 * @Author Kason Zhan
 * @Date 06/03/2025 06/03/2025
 */
public interface AiChatService {

    String call(String prompt, AiPromptModeEnum promptMode, LanguageEnum language);

    String batchCall(List<String> prompt, AiPromptModeEnum promptMode, LanguageEnum language);

}
