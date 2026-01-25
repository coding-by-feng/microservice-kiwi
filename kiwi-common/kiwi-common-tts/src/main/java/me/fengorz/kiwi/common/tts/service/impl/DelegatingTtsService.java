/*
 *
 * Copyright [2019~2025] [codingByFeng]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 *
 */

package me.fengorz.kiwi.common.tts.service.impl;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.sdk.exception.tts.TtsException;
import me.fengorz.kiwi.common.tts.model.TtsProviderProperties;
import me.fengorz.kiwi.common.tts.service.TtsBaseService;
import me.fengorz.kiwi.common.tts.service.TtsService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * Delegating TTS Service
 * Routes requests to the configured TTS provider based on YAML configuration
 *
 * Configuration example in application.yml:
 * tts:
 *   provider: openai  # Options: voicerss, baidu, gcp, deepgram, openai
 *
 * @Author Kason Zhan
 * @Date 2026/1/11
 */
@Slf4j
@Primary
@Service("delegatingTtsService")
public class DelegatingTtsService implements TtsService {

    private final TtsProviderProperties providerProperties;
    private final TtsBaseService voiceRssTtsService;
    private final TtsBaseService baiduTtsService;
    private final TtsBaseService googleTtsService;
    private final TtsBaseService deepgramTtsService;
    private final TtsBaseService openAiTtsService;

    public DelegatingTtsService(
            TtsProviderProperties providerProperties,
            @Qualifier("voiceRssTtsService") TtsBaseService voiceRssTtsService,
            @Qualifier("baiduTtsServiceImpl") TtsBaseService baiduTtsService,
            @Qualifier("googleTtsService") TtsBaseService googleTtsService,
            @Qualifier("deepgramAura2TtsService") TtsBaseService deepgramTtsService,
            @Qualifier("openAiTtsService") TtsBaseService openAiTtsService) {
        this.providerProperties = providerProperties;
        this.voiceRssTtsService = voiceRssTtsService;
        this.baiduTtsService = baiduTtsService;
        this.googleTtsService = googleTtsService;
        this.deepgramTtsService = deepgramTtsService;
        this.openAiTtsService = openAiTtsService;
    }

    @PostConstruct
    public void init() {
        log.info("TTS Service initialized with provider: {}", providerProperties.getProvider());
    }

    private TtsBaseService getActiveService() {
        if (providerProperties.isOpenAi()) {
            return openAiTtsService;
        }
        if (providerProperties.isDeepgram()) {
            return deepgramTtsService;
        }
        if (providerProperties.isGcp()) {
            return googleTtsService;
        }
        if (providerProperties.isBaidu()) {
            return baiduTtsService;
        }
        if (providerProperties.isVoiceRss()) {
            return voiceRssTtsService;
        }
        // Default to OpenAI
        log.warn("Unknown TTS provider '{}', falling back to OpenAI", providerProperties.getProvider());
        return openAiTtsService;
    }

    @Override
    public byte[] speechEnglish(String text) throws TtsException {
        log.debug("Routing speechEnglish to provider: {}", providerProperties.getProvider());
        return getActiveService().speechEnglish(text);
    }

    @Override
    public byte[] speechChinese(String text) throws TtsException {
        log.debug("Routing speechChinese to provider: {}", providerProperties.getProvider());
        return getActiveService().speechChinese(text);
    }
}
