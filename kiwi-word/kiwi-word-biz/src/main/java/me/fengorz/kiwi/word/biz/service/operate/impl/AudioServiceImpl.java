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

package me.fengorz.kiwi.word.biz.service.operate.impl;

import static me.fengorz.kiwi.word.api.common.WordConstants.API_KEY_MAX_USE_TIME;

import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.fastdfs.service.DfsService;
import me.fengorz.kiwi.common.sdk.exception.ResourceNotFoundException;
import me.fengorz.kiwi.common.sdk.exception.dfs.DfsOperateException;
import me.fengorz.kiwi.common.sdk.exception.tts.TtsException;
import me.fengorz.kiwi.common.tts.service.TtsService;
import me.fengorz.kiwi.word.api.common.WordCrawlerConstants;
import me.fengorz.kiwi.word.api.common.enumeration.ReviewAudioTypeEnum;
import me.fengorz.kiwi.word.biz.common.SpeakerFunction;
import me.fengorz.kiwi.word.biz.mapper.ReviewAudioMapper;
import me.fengorz.kiwi.word.biz.model.TtsConfig;
import me.fengorz.kiwi.word.biz.service.operate.AudioService;

/**
 * @Description TODO
 * @Author zhanshifeng
 * @Date 2022/7/13 20:30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AudioServiceImpl implements AudioService {

    private final TtsService ttsService;
    private final TtsConfig ttsConfig;
    private final DfsService dfsService;
    private final ReviewAudioMapper reviewAudioMapper;
    private static final Map<Integer, Integer> API_KEY_USE_TIME_MAP = new ConcurrentHashMap<>();
    static {
        API_KEY_USE_TIME_MAP.put(1, 300);
        API_KEY_USE_TIME_MAP.put(2, 0);
        API_KEY_USE_TIME_MAP.put(3, 0);
        API_KEY_USE_TIME_MAP.put(4, 0);
    }

    @Override

    public String generateVoice(String text, int type) throws DfsOperateException, TtsException {
        if (ReviewAudioTypeEnum.isEnglish(type)) {
            return generateEnglishVoice(text);
        } else {
            return generateChineseVoice(text);
        }
    }

    @Override
    public String generateEnglishVoice(String englishText) throws DfsOperateException, TtsException {
        byte[] bytes;
        bytes = generateBytes(apiKey -> ttsService.speechEnglish(apiKey, englishText));
        return dfsService.uploadFile(new ByteArrayInputStream(bytes), bytes.length, WordCrawlerConstants.EXT_MP3);
    }

    @Override
    public String generateChineseVoice(String chineseText) throws DfsOperateException, TtsException {
        byte[] bytes;
        bytes = generateBytes(apiKey -> ttsService.speechChinese(apiKey, chineseText));
        return dfsService.uploadFile(new ByteArrayInputStream(bytes), bytes.length, WordCrawlerConstants.EXT_MP3);
    }

    private byte[] generateBytes(SpeakerFunction<String, byte[]> speaker) throws TtsException {
        byte[] bytes;
        String apiKey = chooseApiKey();
        try {
            bytes = speaker.speech(apiKey);
        } catch (Exception e) {
            log.error("tts api key {} is invalid!", apiKey);
            throw new TtsException();
        }
        return bytes;
    }

    private String chooseApiKey() {
        if (API_KEY_USE_TIME_MAP.get(1) < API_KEY_MAX_USE_TIME) {
            API_KEY_USE_TIME_MAP.computeIfPresent(1, (key, time) -> time++);
            return ttsConfig.getApiKey1();
        } else if (API_KEY_USE_TIME_MAP.get(2) < API_KEY_MAX_USE_TIME) {
            API_KEY_USE_TIME_MAP.computeIfPresent(2, (key, time) -> time++);
            return ttsConfig.getApiKey2();
        } else if (API_KEY_USE_TIME_MAP.get(3) < API_KEY_MAX_USE_TIME) {
            API_KEY_USE_TIME_MAP.computeIfPresent(3, (key, time) -> time++);
            return ttsConfig.getApiKey3();
        } else if (API_KEY_USE_TIME_MAP.get(4) < API_KEY_MAX_USE_TIME) {
            API_KEY_USE_TIME_MAP.computeIfPresent(4, (key, time) -> time++);
            return ttsConfig.getApiKey4();
        }
        throw new ResourceNotFoundException();
    }

}
