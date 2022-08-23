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

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.voicerss.tts.*;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.sdk.annotation.cache.KiwiCacheKey;
import me.fengorz.kiwi.common.sdk.annotation.cache.KiwiCacheKeyPrefix;
import me.fengorz.kiwi.common.sdk.constant.CacheConstants;
import me.fengorz.kiwi.common.sdk.constant.GlobalConstants;
import me.fengorz.kiwi.common.sdk.exception.ResourceNotFoundException;
import me.fengorz.kiwi.common.sdk.exception.tts.TtsException;
import me.fengorz.kiwi.common.tts.TtsConstants;
import me.fengorz.kiwi.common.tts.model.TtsConfig;
import me.fengorz.kiwi.common.tts.service.TtsService;

/**
 * @Description TODO
 * @Author zhanshifeng
 * @Date 2022/7/5 22:35
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TtsServiceImpl implements TtsService {

    private final TtsConfig ttsConfig;

    @Override
    public byte[] speechEnglish(String apiKey, String text) throws TtsException {
        if (StringUtils.isBlank(text)) {
            return speech(Languages.Chinese_China, apiKey, ENGLISH_PARAPHRASE_MISSING);
        }
        return speech(Languages.English_UnitedStates, apiKey, text);
    }

    @Override
    public byte[] speechChinese(String apiKey, String text) throws TtsException {
        if (StringUtils.isBlank(text)) {
            return speech(Languages.Chinese_China, apiKey, CHINESE_PARAPHRASE_MISSING);
        }
        return speech(Languages.Chinese_China, apiKey, this.replaceEllipsis(text));
    }

    @Override
    public String autoSelectApiKey() {
        String finalApiKey = null;
        int minUsedTime = TtsConstants.API_KEY_MAX_USE_TIME;
        for (String apiKey : ttsConfig.listApiKey()) {
            int usedTime = queryTtsApiKeyUsed(apiKey);
            if (usedTime >= TtsConstants.API_KEY_MAX_USE_TIME) {
                continue;
            }
            if (usedTime < minUsedTime) {
                minUsedTime = usedTime;
                finalApiKey = apiKey;
            }
        }
        log.info("autoSelectApiKey finalApiKey is {}, minUsedTime is {}", finalApiKey, minUsedTime);

        if (StringUtils.isNotBlank(finalApiKey)) {
            increaseApiKeyUsedTime(finalApiKey);
        } else {
            throw new ResourceNotFoundException();
        }
        return finalApiKey;
    }

    @Async
    @Override
    public void increaseApiKeyUsedTime(String apiKey) {
        synchronized (BARRIER) {
            this.voiceRssGlobalIncreaseCounter();
            useTtsApiKey(apiKey,
                Optional.ofNullable(queryTtsApiKeyUsed(apiKey)).orElseThrow(ResourceNotFoundException::new) + 1);
        }
    }

    @KiwiCacheKeyPrefix(TtsConstants.CACHE_KEY_PREFIX_TTS.TTS_VOICE_RSS_API_KEY_USED_TIME)
    @Cacheable(cacheNames = TtsConstants.CACHE_NAMES, keyGenerator = CacheConstants.CACHE_KEY_GENERATOR_BEAN,
        unless = "#result == null")
    @Override
    public Integer queryTtsApiKeyUsed(@KiwiCacheKey String apiKey) {
        return null;
    }

    @SuppressWarnings("UnusedReturnValue")
    @KiwiCacheKeyPrefix(TtsConstants.CACHE_KEY_PREFIX_TTS.TTS_VOICE_RSS_API_KEY_USED_TIME)
    @CachePut(cacheNames = TtsConstants.CACHE_NAMES, keyGenerator = CacheConstants.CACHE_KEY_GENERATOR_BEAN,
        unless = "#result == null")
    @Override
    public int useTtsApiKey(@KiwiCacheKey String apiKey, int time) {
        return time;
    }

    @Async
    @Override
    public void deprecateApiKeyToday(String apiKey) {
        if (!ttsConfig.listApiKey().contains(apiKey)) {
            return;
        }
        Optional.ofNullable(HttpUtil.get(StrUtil.format(ttsConfig.getUrl(), apiKey))).ifPresent(response -> {
            if (StringUtils.startsWith(response, "ERROR:")) {
                useTtsApiKey(apiKey, TtsConstants.API_KEY_MAX_USE_TIME);
            }
        });
    }

    @Override
    public void voiceRssGlobalIncreaseCounter() {
        useTtsApiKey(TtsConstants.CACHE_KEY_PREFIX_TTS.TOTAL_API_KEY,
            Optional.ofNullable(queryTtsApiKeyUsed(TtsConstants.CACHE_KEY_PREFIX_TTS.TOTAL_API_KEY))
                .orElseThrow(() -> new ResourceNotFoundException("apiKey is null")) + 1);
    }

    @Override
    public boolean hasValidApiKey() {
        Integer totalUsedTime = queryTtsApiKeyUsed(TtsConstants.CACHE_KEY_PREFIX_TTS.TOTAL_API_KEY);
        return totalUsedTime != null
            && totalUsedTime < TtsConstants.API_KEY_MAX_USE_TIME * ttsConfig.listApiKey().size();
    }

    private String replaceEllipsis(String text) {
        if (StringUtils.isBlank(text)) {
            return null;
        }
        return text.replaceAll(GlobalConstants.SYMBOL_ENGLISH_ELLIPSIS, GlobalConstants.WHAT).replaceAll(REGEX,
            GlobalConstants.WHAT);
    }

    private byte[] speech(String language, String apiKey, String text) throws TtsException {
        log.info("TTS is speeching, language = {}, apiKey = {}, text = {}", language, apiKey, text);
        VoiceProvider tts = new VoiceProvider(apiKey, true);
        VoiceParameters params = new VoiceParameters(text, language);
        params.setCodec(AudioCodec.MP3);
        params.setFormat(AudioFormat.Format_44KHZ.AF_44khz_16bit_stereo);
        params.setRate(DEFAULT_TTS_VOICE_RSS_ENGLISH_SPEECH_RATE);
        params.setBase64(false);
        params.setSSML(false);

        byte[] voice;
        try {
            voice = tts.speech(params);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new TtsException("tts speech error.");
        }
        return voice;
    }

    private static final int DEFAULT_TTS_VOICE_RSS_ENGLISH_SPEECH_RATE = -2;
    private static final String ENGLISH_PARAPHRASE_MISSING = "英文缺失";
    private static final String CHINESE_PARAPHRASE_MISSING = "中文缺失";
    private static final String REGEX = "\\.\\.\\.";
    private static final Object BARRIER = new Object();

}
