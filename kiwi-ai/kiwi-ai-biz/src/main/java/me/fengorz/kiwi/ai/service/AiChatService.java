package me.fengorz.kiwi.ai.service;

import me.fengorz.kiwi.common.sdk.enumeration.AiPromptModeEnum;
import me.fengorz.kiwi.common.sdk.enumeration.LanguageEnum;

import java.util.List;

/**
 * @Author Kason Zhan
 * @Date 06/03/2025 06/03/2025
 */
public interface AiChatService {

    String call(String prompt, AiPromptModeEnum promptMode, LanguageEnum language);

    String call(String prompt, AiPromptModeEnum promptMode, LanguageEnum targetLanguage, LanguageEnum nativeLanguage);

    String batchCall(List<String> prompt, AiPromptModeEnum promptMode, LanguageEnum language);

    String batchCallForYtbAndCache(String ytbUrl, List<String> prompt, AiPromptModeEnum promptMode, LanguageEnum language);

    void cleanBatchCallForYtbAndCache(String ytbUrl, AiPromptModeEnum promptMode, LanguageEnum language);

    String callForYtbAndCache(String ytbUrl, String prompt, AiPromptModeEnum promptMode, LanguageEnum language);

    void cleanCallForYtbAndCache(String ytbUrl, AiPromptModeEnum promptMode, LanguageEnum language);

}
