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

import java.util.HashMap;

import org.springframework.stereotype.Service;

import com.baidu.aip.speech.AipSpeech;
import com.baidu.aip.speech.TtsResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.sdk.exception.tts.TtsException;
import me.fengorz.kiwi.common.tts.TtsConstants;
import me.fengorz.kiwi.common.tts.model.BaiduTtsProperties;
import me.fengorz.kiwi.common.tts.service.BaiduTtsService;

/**
 * @Description TODO
 * @Author zhanshifeng
 * @Date 2022/10/1 11:20
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BaiduTtsServiceImpl implements BaiduTtsService {

    private final BaiduTtsProperties config;
    private static final String SPEAK_SPEED_DEFAULT_VALUE = "5";
    private static final String SPEAK_TONE_DEFAULT_VALUE = "5";
    private static final String SPEAK_PER_DEFAULT_VALUE = "4";
    private static final String SPEAK_AUE_DEFAULT_VALUE = "3";
    private static final String SPEAK_LANGUAGE_DEFAULT_VALUE = "zh";

    @Override
    public byte[] speech(String text) throws TtsException {
        // 初始化一个AipSpeech
        AipSpeech client = new AipSpeech(config.getAppId(), config.getApiKey(), config.getSecretKey());

        // 可选：设置网络连接参数
        client.setConnectionTimeoutInMillis(2000);
        client.setSocketTimeoutInMillis(60000);

        HashMap<String, Object> options = new HashMap<>(3);
        options.put(TtsConstants.BAIDU_TTS_OPTIONS.SPEAK_SPEED, SPEAK_SPEED_DEFAULT_VALUE);
        options.put(TtsConstants.BAIDU_TTS_OPTIONS.SPEAK_TONE, SPEAK_TONE_DEFAULT_VALUE);
        options.put(TtsConstants.BAIDU_TTS_OPTIONS.SPEAK_PER, SPEAK_PER_DEFAULT_VALUE);
        options.put(TtsConstants.BAIDU_TTS_OPTIONS.SPEAK_AUE, SPEAK_AUE_DEFAULT_VALUE);

        try {
            TtsResponse res = client.synthesis(text, SPEAK_LANGUAGE_DEFAULT_VALUE, 1, options);
            return res.getData();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new TtsException("Baidu TTS speech failed, text=%s", text);
        }
    }

}
