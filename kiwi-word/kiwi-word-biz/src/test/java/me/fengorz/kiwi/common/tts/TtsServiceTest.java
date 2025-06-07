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

package me.fengorz.kiwi.common.tts;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import com.baidu.aip.util.Util;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.sdk.constant.EnvConstants;
import me.fengorz.kiwi.common.tts.model.TtsProperties;
import me.fengorz.kiwi.common.tts.service.BaiduTtsService;
import me.fengorz.kiwi.common.tts.service.TtsService;
import me.fengorz.kiwi.word.biz.WordBizApplication;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;

import static me.fengorz.kiwi.common.tts.TtsConstants.BEAN_NAMES.BAIDU_TTS_SERVICE_IMPL;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
@ActiveProfiles({EnvConstants.TEST})
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = WordBizApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class TtsServiceTest {

    @Autowired
    private TtsProperties ttsProperties;

    @Qualifier("chatTtsService")
    private TtsService ttsService;

    @Autowired
    @Qualifier(BAIDU_TTS_SERVICE_IMPL)
    private BaiduTtsService baiduTtsService;

    @Autowired
    private Environment env;

    @Test
    @Disabled
    void autoSelectApiKey() {
        String apiKey = ttsService.autoSelectApiKey();
        assertTrue(ttsProperties.listApiKey().contains(apiKey));
        log.info("autoSelectApiKey >>>> {}", apiKey);
    }

    @Test
    @Disabled
    void increaseApiKeyUsedTime() {
        Assertions.assertDoesNotThrow(() -> ttsService.increaseApiKeyUsedTime("d2f5e9a0fbf14318a475d808c0dddc62"));
    }

    @Test
    @Disabled
    void useTtsApiKey() {
        Assertions.assertDoesNotThrow(() -> {
            ttsService.useTtsApiKey(ttsProperties.getApiKey10(), 350);
            ttsService.useTtsApiKey(ttsProperties.getApiKey11(), 350);
            ttsService.useTtsApiKey(ttsProperties.getApiKey12(), 350);
            ttsService.useTtsApiKey(ttsProperties.getApiKey13(), 350);
            ttsService.useTtsApiKey(ttsProperties.getApiKey14(), 350);
            ttsService.useTtsApiKey(ttsProperties.getApiKey15(), 0);
            ttsService.useTtsApiKey(ttsProperties.getApiKey16(), 0);
            ttsService.useTtsApiKey(ttsProperties.getApiKey17(), 0);
            ttsService.useTtsApiKey(ttsProperties.getApiKey18(), 0);
            ttsService.useTtsApiKey(ttsProperties.getApiKey19(), 0);
            ttsService.useTtsApiKey(ttsProperties.getApiKey20(), 0);
        });
    }

    @Test
    @Disabled
    void queryTtsApiKeyUsed() {
        Assertions.assertEquals(0, ttsService.queryTtsApiKeyUsed(ttsProperties.getApiKey10()));
    }


    @Test
    @Disabled
    void deprecateApiKeyToday() {
        Assertions.assertDoesNotThrow(() -> ttsService.deprecateApiKeyToday(ttsProperties.getApiKey1()));
        Assertions.assertEquals(TtsConstants.API_KEY_MAX_USE_TIME,
                ttsService.queryTtsApiKeyUsed(ttsProperties.getApiKey1()));
    }

    @Test
    @Disabled
    void queryApiKeyUsed() {
        log.info("queryTtsApiKeyUsed [{}] used times is {}", ttsProperties.getApiKey10(),
                ttsService.queryTtsApiKeyUsed(ttsProperties.getApiKey10()));
        log.info("queryTtsApiKeyUsed [{}] used times is {}", ttsProperties.getApiKey11(),
                ttsService.queryTtsApiKeyUsed(ttsProperties.getApiKey11()));
        log.info("queryTtsApiKeyUsed [{}] used times is {}", ttsProperties.getApiKey12(),
                ttsService.queryTtsApiKeyUsed(ttsProperties.getApiKey12()));
        log.info("queryTtsApiKeyUsed [{}] used times is {}", ttsProperties.getApiKey13(),
                ttsService.queryTtsApiKeyUsed(ttsProperties.getApiKey13()));
        log.info("queryTtsApiKeyUsed [{}] used times is {}", ttsProperties.getApiKey14(),
                ttsService.queryTtsApiKeyUsed(ttsProperties.getApiKey14()));
    }

    @Test
    @Disabled
    void refreshAllApiKey() {
        Assertions.assertDoesNotThrow(() -> ttsService.refreshAllApiKey());
    }

    @Test
    void queryAllTtsApiKeyUsed() {
        log.info("queryTtsApiKeyUsed [total] used times is {}",
                ttsService.queryTtsApiKeyUsed(TtsConstants.CACHE_KEY_PREFIX_TTS.TOTAL_API_KEY));
        for (String apiKey : ttsProperties.listApiKey()) {
            log.info("queryTtsApiKeyUsed [{}] used times is {}", apiKey, ttsService.queryTtsApiKeyUsed(apiKey));
        }
    }

    @Test
    @Disabled
    void assertTtsApiKeyNotNull() {
        for (String apiKey : ttsProperties.listApiKey()) {
            assertNotNull(ttsService.queryTtsApiKeyUsed(apiKey));
        }
    }

    @Test
    @Disabled
    void testVoiceRssUrl() {
        log.info("testVoiceRssUrl response is {}",
                HttpUtil.get(StrUtil.format(ttsProperties.getUrl(), "58d4baef52414088998cbbda9751c812")));
    }

    @SneakyThrows
    @Test
    @Disabled
    public void testBaiduTts() {
        byte[] data = baiduTtsService.speech(
                " A。R。E。F。E。R。L。a way of discovering, by questions or practical activities, what someone knows, or what someone or something can do or is like");
        assertNotNull(data);
        try {
            Util.writeBytesToFileSystem(data, "/Users/zhanshifeng/Documents/temp/baidu_tts_test.mp3");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SneakyThrows
    @Test
    public void testVoiceRssTts() {
        byte[] data = ttsService.speechEnglish("test voice rss");
        assertNotNull(data);
        try {
            Util.writeBytesToFileSystem(data, "/Users/zhanshifeng/Documents/temp/voice_rss_tts_test.mp3");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}