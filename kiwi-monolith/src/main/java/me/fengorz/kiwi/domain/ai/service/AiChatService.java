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
package me.fengorz.kiwi.domain.ai.service;

import me.fengorz.kiwi.common.enumeration.AiPromptModeEnum;
import me.fengorz.kiwi.common.enumeration.LanguageEnum;

import java.util.List;

/**
 * AI Chat Service Interface
 *
 * @author codingByFeng
 */
public interface AiChatService {

    /**
     * Call AI with string-based prompt mode (for controller compatibility)
     */
    default String call(String prompt, String promptMode, String language) {
        // Convert underscore format (DIRECTLY_TRANSLATION) to hyphen format (directly-translation)
        String modeStr = promptMode.toLowerCase().replace("_", "-");
        AiPromptModeEnum mode = AiPromptModeEnum.fromMode(modeStr);
        LanguageEnum lang = LanguageEnum.fromCode(language);
        return call(prompt, mode, lang);
    }

    String call(String prompt, AiPromptModeEnum promptMode, LanguageEnum language);

    String call(String prompt, AiPromptModeEnum promptMode, LanguageEnum targetLanguage, LanguageEnum nativeLanguage);

    String batchCall(List<String> prompt, AiPromptModeEnum promptMode, LanguageEnum language);

    String batchCallForYtbAndCache(String ytbUrl, List<String> prompt, AiPromptModeEnum promptMode, LanguageEnum language);

    void cleanBatchCallForYtbAndCache(String ytbUrl, AiPromptModeEnum promptMode, LanguageEnum language);

    String callForYtbAndCache(String ytbUrl, String prompt, AiPromptModeEnum promptMode, LanguageEnum language);

    void cleanCallForYtbAndCache(String ytbUrl, AiPromptModeEnum promptMode, LanguageEnum language);
}
