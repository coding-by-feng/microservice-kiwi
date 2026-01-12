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
package me.fengorz.kiwi.domain.ai.service.impl;

import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.enumeration.AiPromptModeEnum;
import me.fengorz.kiwi.common.enumeration.LanguageEnum;
import me.fengorz.kiwi.domain.ai.config.AiProviderProperties;
import me.fengorz.kiwi.domain.ai.service.AiChatService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.List;

/**
 * Delegating AI Chat Service
 * Routes requests to the configured AI provider (Grok or Gemini)
 *
 * @author codingByFeng
 */
@Slf4j
@Primary
@Service("aiChatService")
public class DelegatingAiChatService implements AiChatService {

    private final AiProviderProperties providerProperties;
    private final AiChatService grokAiService;
    private final AiChatService geminiAiService;

    public DelegatingAiChatService(AiProviderProperties providerProperties,
                                    @Qualifier("grokAiService") AiChatService grokAiService,
                                    @Qualifier("geminiAiService") AiChatService geminiAiService) {
        this.providerProperties = providerProperties;
        this.grokAiService = grokAiService;
        this.geminiAiService = geminiAiService;
    }

    @PostConstruct
    public void init() {
        log.info("AI Chat Service initialized with provider: {}", providerProperties.getProvider());
    }

    private AiChatService getActiveService() {
        if (providerProperties.isGemini()) {
            return geminiAiService;
        }
        return grokAiService; // Default to Grok
    }

    @Override
    public String call(String prompt, AiPromptModeEnum promptMode, LanguageEnum language) {
        log.debug("Routing call to provider: {}", providerProperties.getProvider());
        return getActiveService().call(prompt, promptMode, language);
    }

    @Override
    public String call(String prompt, AiPromptModeEnum promptMode, LanguageEnum targetLanguage, LanguageEnum nativeLanguage) {
        log.debug("Routing call to provider: {}", providerProperties.getProvider());
        return getActiveService().call(prompt, promptMode, targetLanguage, nativeLanguage);
    }

    @Override
    public String batchCall(List<String> prompt, AiPromptModeEnum promptMode, LanguageEnum language) {
        log.debug("Routing batch call to provider: {}", providerProperties.getProvider());
        return getActiveService().batchCall(prompt, promptMode, language);
    }

    @Override
    public String batchCallForYtbAndCache(String ytbUrl, List<String> prompt, AiPromptModeEnum promptMode, LanguageEnum language) {
        log.debug("Routing YTB batch call to provider: {}", providerProperties.getProvider());
        return getActiveService().batchCallForYtbAndCache(ytbUrl, prompt, promptMode, language);
    }

    @Override
    public void cleanBatchCallForYtbAndCache(String ytbUrl, AiPromptModeEnum promptMode, LanguageEnum language) {
        getActiveService().cleanBatchCallForYtbAndCache(ytbUrl, promptMode, language);
    }

    @Override
    public String callForYtbAndCache(String ytbUrl, String prompt, AiPromptModeEnum promptMode, LanguageEnum language) {
        log.debug("Routing YTB call to provider: {}", providerProperties.getProvider());
        return getActiveService().callForYtbAndCache(ytbUrl, prompt, promptMode, language);
    }

    @Override
    public void cleanCallForYtbAndCache(String ytbUrl, AiPromptModeEnum promptMode, LanguageEnum language) {
        getActiveService().cleanCallForYtbAndCache(ytbUrl, promptMode, language);
    }
}
