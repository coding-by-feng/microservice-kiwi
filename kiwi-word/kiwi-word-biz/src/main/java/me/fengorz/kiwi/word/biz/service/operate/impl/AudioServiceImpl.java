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

import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.fastdfs.service.DfsService;
import me.fengorz.kiwi.common.sdk.exception.dfs.DfsOperateException;
import me.fengorz.kiwi.common.sdk.exception.tts.TtsException;
import me.fengorz.kiwi.common.tts.service.BaiduTtsService;
import me.fengorz.kiwi.common.tts.service.TtsService;
import me.fengorz.kiwi.word.api.common.ApiCrawlerConstants;
import me.fengorz.kiwi.word.api.common.enumeration.ReviseAudioTypeEnum;
import me.fengorz.kiwi.word.biz.common.SpeakerFunction;
import me.fengorz.kiwi.word.biz.service.operate.AudioService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;

import static me.fengorz.kiwi.common.tts.TtsConstants.BEAN_NAMES.BAIDU_TTS_SERVICE_IMPL;

/**
 * @Description TODO
 * @Author zhanshifeng
 * @Date 2022/7/13 20:30
 */
@Slf4j
@Service
public class AudioServiceImpl implements AudioService {

    private final TtsService ttsService;
    private final BaiduTtsService baiduTtsService;
    private final DfsService dfsService;

    public AudioServiceImpl(TtsService ttsService,
                            @Qualifier(BAIDU_TTS_SERVICE_IMPL) BaiduTtsService baiduTtsService,
                            DfsService dfsService) {
        this.ttsService = ttsService;
        this.baiduTtsService = baiduTtsService;
        this.dfsService = dfsService;
    }

    @Override
    public String generateVoice(String text, int type) throws DfsOperateException, TtsException {
        if (ReviseAudioTypeEnum.isEnglish(type)) {
            return generateEnglishVoice(text);
        } else {
            return generateChineseVoice(text);
        }
    }

    @Override
    public String generateEnglishVoice(String englishText) throws DfsOperateException, TtsException {
        byte[] bytes;
        bytes = generateBytes(apiKey -> ttsService.speechEnglish(apiKey, englishText));
        return dfsService.uploadFile(new ByteArrayInputStream(bytes), bytes.length, ApiCrawlerConstants.EXT_MP3);
    }

    @Override
    public String generateChineseVoice(String chineseText) throws DfsOperateException, TtsException {
        byte[] bytes;
        bytes = generateBytes(apiKey -> ttsService.speechChinese(apiKey, chineseText));
        return dfsService.uploadFile(new ByteArrayInputStream(bytes), bytes.length, ApiCrawlerConstants.EXT_MP3);
    }

    @Override
    public String generateVoiceUseBaiduTts(String chineseText) throws DfsOperateException, TtsException {
        if (StringUtils.isBlank(chineseText)) {
            chineseText = CHINESE_TEXT_MISSING;
        }
        byte[] bytes = baiduTtsService.speech(chineseText);
        return dfsService.uploadFile(new ByteArrayInputStream(bytes), bytes.length, ApiCrawlerConstants.EXT_MP3);
    }

    private byte[] generateBytes(SpeakerFunction<String, byte[]> speaker) throws TtsException {
        byte[] bytes;
        String apiKey = ttsService.autoSelectApiKey();
        try {
            bytes = speaker.speech(apiKey);
        } catch (Exception e) {
            log.error("tts api key {} is invalid!", apiKey);
            throw new TtsException();
        }
        return bytes;
    }

    private static final String CHINESE_TEXT_MISSING = "中文翻译缺失";

}
