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

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import com.voicerss.tts.*;

import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.sdk.constant.GlobalConstants;
import me.fengorz.kiwi.common.sdk.exception.tts.TtsException;
import me.fengorz.kiwi.common.tts.service.TtsService;

/**
 * @Description TODO
 * @Author zhanshifeng
 * @Date 2022/7/5 22:35
 */
@Slf4j
@Service
public class TtsServiceImpl implements TtsService {

    private static final int DEFAULT_TTS_VOICE_RSS_ENGLISH_SPEECH_RATE = -2;
    private static final String ENGLISH_PARAPHRASE_MISSING = "英文缺失";
    private static final String CHINESE_PARAPHRASE_MISSING = "中文缺失";
    private static final String REGEX = "\\.\\.\\.";

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

    private String replaceEllipsis(String text) {
        if (StringUtils.isBlank(text)) {
            return null;
        }
        return text.replaceAll(GlobalConstants.SYMBOL_ENGLISH_ELLIPSIS, GlobalConstants.WHAT).replaceAll(REGEX,
            GlobalConstants.WHAT);
    }

    private byte[] speech(String language, String apiKey, String text) throws TtsException {
        log.info("TTS is speeching, language = {}, apiKey = {}, text = {}", language, apiKey, text);
        VoiceProvider tts = new VoiceProvider(apiKey);
        VoiceParameters params = new VoiceParameters(text, language);
        params.setCodec(AudioCodec.WAV);
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

}