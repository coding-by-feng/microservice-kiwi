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

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.enumeration.AiPromptModeEnum;
import me.fengorz.kiwi.common.enumeration.LanguageEnum;
import me.fengorz.kiwi.domain.ai.config.AiProviderProperties;
import me.fengorz.kiwi.domain.ai.service.AiStreamingService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

/**
 * Delegating AI Streaming Service
 * Routes streaming requests to the configured AI provider (Grok or Gemini)
 *
 * @author codingByFeng
 */
@Slf4j
@Primary
@Service("aiStreamingService")
public class DelegatingAiStreamingService implements AiStreamingService {

    private final AiProviderProperties providerProperties;
    private final AiStreamingService grokStreamingService;
    private final AiStreamingService geminiStreamingService;

    public DelegatingAiStreamingService(AiProviderProperties providerProperties,
                                         @Qualifier("grokStreamingService") AiStreamingService grokStreamingService,
                                         @Qualifier("geminiStreamingService") AiStreamingService geminiStreamingService) {
        this.providerProperties = providerProperties;
        this.grokStreamingService = grokStreamingService;
        this.geminiStreamingService = geminiStreamingService;
    }

    @PostConstruct
    public void init() {
        log.info("AI Streaming Service initialized with provider: {}", providerProperties.getProvider());
    }

    private AiStreamingService getActiveService() {
        if (providerProperties.isGemini()) {
            return geminiStreamingService;
        }
        return grokStreamingService; // Default to Grok
    }

    @Override
    public void streamCall(String prompt, AiPromptModeEnum promptMode, LanguageEnum targetLanguage,
                           LanguageEnum nativeLanguage, Consumer<String> onChunk, Consumer<Exception> onError,
                           Runnable onComplete) {
        log.debug("Routing stream call to provider: {}", providerProperties.getProvider());
        getActiveService().streamCall(prompt, promptMode, targetLanguage, nativeLanguage, onChunk, onError, onComplete);
    }
}
