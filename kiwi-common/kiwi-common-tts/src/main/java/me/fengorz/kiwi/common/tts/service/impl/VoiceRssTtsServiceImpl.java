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

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import com.voicerss.tts.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.sdk.annotation.cache.KiwiCacheKey;
import me.fengorz.kiwi.common.sdk.annotation.cache.KiwiCacheKeyPrefix;
import me.fengorz.kiwi.common.sdk.constant.CacheConstants;
import me.fengorz.kiwi.common.sdk.constant.GlobalConstants;
import me.fengorz.kiwi.common.sdk.exception.ResourceNotFoundException;
import me.fengorz.kiwi.common.sdk.exception.tts.TtsException;
import me.fengorz.kiwi.common.sdk.util.validate.KiwiAssertUtils;
import me.fengorz.kiwi.common.tts.TtsConstants;
import me.fengorz.kiwi.common.tts.enumeration.TtsSourceEnum;
import me.fengorz.kiwi.common.tts.model.TtsProperties;
import me.fengorz.kiwi.common.tts.service.TtsService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Service("voiceRssTtsService")
public class VoiceRssTtsServiceImpl implements TtsService {

    private final TtsProperties ttsProperties;

    @Override
    public byte[] speechEnglish(String text) throws TtsException {
        if (StringUtils.isBlank(text)) {
            return speech(Languages.Chinese_China, this.autoSelectApiKey(), ENGLISH_PARAPHRASE_MISSING);
        }
        return speech(Languages.English_UnitedStates, this.autoSelectApiKey(), text);
    }

    @Override
    public byte[] speechChinese(String text) throws TtsException {
        if (StringUtils.isBlank(text)) {
            return speech(Languages.Chinese_China, this.autoSelectApiKey(), CHINESE_PARAPHRASE_MISSING);
        }
        return speech(Languages.Chinese_China, this.autoSelectApiKey(), this.replaceEllipsis(text));
    }

    @Override
    public String autoSelectApiKey() {
        String finalApiKey = null;
        int minUsedTime = TtsConstants.API_KEY_MAX_USE_TIME;
        for (String apiKey : ttsProperties.listApiKey()) {
            int usedTime = Optional.ofNullable(queryTtsApiKeyUsed(apiKey)).orElse(0);
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
            try {
                increaseApiKeyUsedTime(finalApiKey);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                throw new RuntimeException(e);
            }
        } else {
            throw new ResourceNotFoundException(ERROR_MSG_NOT_VALID_API_KEY);
        }
        return finalApiKey;
    }

    @Async
    @Override
    public void increaseApiKeyUsedTime(String apiKey) {
        synchronized (BARRIER) {
            this.voiceRssGlobalIncreaseCounter();
            useTtsApiKey(apiKey,
                    Optional.ofNullable(queryTtsApiKeyUsed(apiKey)).orElse(0) + 1);
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
        if (!ttsProperties.listApiKey().contains(apiKey)) {
            return;
        }
        Optional.ofNullable(HttpUtil.get(StrUtil.format(ttsProperties.getUrl(), apiKey))).ifPresent(response -> {
            if (StringUtils.startsWith(response, "ERROR:")) {
                useTtsApiKey(apiKey, TtsConstants.API_KEY_MAX_USE_TIME);
            }
        });
    }

    @Override
    public void voiceRssGlobalIncreaseCounter() {
        useTtsApiKey(TtsConstants.CACHE_KEY_PREFIX_TTS.TOTAL_API_KEY,
                Optional.ofNullable(queryTtsApiKeyUsed(TtsConstants.CACHE_KEY_PREFIX_TTS.TOTAL_API_KEY))
                        .orElse(0) + 1);
    }

    @Override
    public boolean hasValidApiKey() {
        Integer totalUsedTime = Optional.ofNullable(queryTtsApiKeyUsed(TtsConstants.CACHE_KEY_PREFIX_TTS.TOTAL_API_KEY))
                .orElse(0);
        return totalUsedTime < TtsConstants.API_KEY_MAX_USE_TIME * ttsProperties.listApiKey().size();
    }

    @Override
    public void refreshAllApiKey() {
        for (String apiKey : ttsProperties.listApiKey()) {
            useTtsApiKey(apiKey, 0);
        }
        useTtsApiKey(TtsConstants.CACHE_KEY_PREFIX_TTS.TOTAL_API_KEY, 0);
    }

    private byte[] wrap(byte[] bytes) {
        return KiwiAssertUtils.assertNotEmpty(bytes, "%s TTS generated bytes must not be null.",
                TtsSourceEnum.VOICERSS);
    }

    private String replaceEllipsis(String text) {
        if (StringUtils.isBlank(text)) {
            return null;
        }
        return text.replaceAll(GlobalConstants.SYMBOL_ENGLISH_ELLIPSIS, GlobalConstants.WHAT).replaceAll(REGEX,
                GlobalConstants.WHAT);
    }

    private byte[] speech(String language, String apiKey, String text) throws TtsException {
        log.info("{} TTS is speeching, language = {}, apiKey = {}, text = {}", TtsSourceEnum.VOICERSS.getSource(),
                language, apiKey, text);
        VoiceProvider tts = new VoiceProvider(apiKey, true);
        VoiceParameters params = new VoiceParameters(text, language);
        params.setCodec(AudioCodec.MP3);
        params.setFormat(AudioFormat.Format_11KHZ.AF_11khz_16bit_stereo);
        params.setRate(DEFAULT_TTS_VOICE_RSS_ENGLISH_SPEECH_RATE);
        params.setBase64(false);
        params.setSSML(false);
        params.setVoice(DEFAULT_TTS_VOICES_ROLE);

        byte[] voice;
        try {
            voice = tts.speech(params);
        } catch (Exception e) {
            log.error(e.getMessage());
            log.error("Api key({}) has deprecated.", apiKey);
            this.deprecateApiKeyToday(apiKey);
            throw new TtsException("tts speech error.");
        }
        return wrap(voice);
    }

    private static final int DEFAULT_TTS_VOICE_RSS_ENGLISH_SPEECH_RATE = -2;
    private static final String DEFAULT_TTS_VOICES_ROLE = "Xia";
    private static final String ENGLISH_PARAPHRASE_MISSING = "英文缺失";
    private static final String CHINESE_PARAPHRASE_MISSING = "中文缺失";
    private static final String REGEX = "\\.\\.\\.";
    private static final Object BARRIER = new Object();
    private static final String ERROR_MSG_NOT_VALID_API_KEY = "There is not valid api key.";

}
