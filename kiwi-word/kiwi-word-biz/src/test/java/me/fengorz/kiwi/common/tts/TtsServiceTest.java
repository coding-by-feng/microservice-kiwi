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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

import static me.fengorz.kiwi.common.tts.TtsConstants.BEAN_NAMES.BAIDU_TTS_SERVICE_IMPL;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@ActiveProfiles({EnvConstants.TEST})
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = WordBizApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class TtsServiceTest {

    @Autowired
    private TtsProperties ttsProperties;

    @Qualifier("googleTtsService")
    private TtsService voiceRssTtsService;

    @Qualifier("voiceRssTtsService")
    private TtsService googleTtsService;

    @Autowired
    @Qualifier(BAIDU_TTS_SERVICE_IMPL)
    private BaiduTtsService baiduTtsService;

    @Autowired
    private Environment env;

    @Test
    @Disabled
    void autoSelectApiKey() {
        String apiKey = voiceRssTtsService.autoSelectApiKey();
        assertTrue(ttsProperties.listApiKey().contains(apiKey));
        log.info("autoSelectApiKey >>>> {}", apiKey);
    }

    @Test
    @Disabled
    void increaseApiKeyUsedTime() {
        Assertions.assertDoesNotThrow(() -> voiceRssTtsService.increaseApiKeyUsedTime("d2f5e9a0fbf14318a475d808c0dddc62"));
    }

    @Test
    @Disabled
    void useTtsApiKey() {
        Assertions.assertDoesNotThrow(() -> {
            voiceRssTtsService.useTtsApiKey(ttsProperties.getApiKey10(), 350);
            voiceRssTtsService.useTtsApiKey(ttsProperties.getApiKey11(), 350);
            voiceRssTtsService.useTtsApiKey(ttsProperties.getApiKey12(), 350);
            voiceRssTtsService.useTtsApiKey(ttsProperties.getApiKey13(), 350);
            voiceRssTtsService.useTtsApiKey(ttsProperties.getApiKey14(), 350);
            voiceRssTtsService.useTtsApiKey(ttsProperties.getApiKey15(), 0);
            voiceRssTtsService.useTtsApiKey(ttsProperties.getApiKey16(), 0);
            voiceRssTtsService.useTtsApiKey(ttsProperties.getApiKey17(), 0);
            voiceRssTtsService.useTtsApiKey(ttsProperties.getApiKey18(), 0);
            voiceRssTtsService.useTtsApiKey(ttsProperties.getApiKey19(), 0);
            voiceRssTtsService.useTtsApiKey(ttsProperties.getApiKey20(), 0);
        });
    }

    @Test
    @Disabled
    void queryTtsApiKeyUsed() {
        Assertions.assertEquals(0, voiceRssTtsService.queryTtsApiKeyUsed(ttsProperties.getApiKey10()));
    }


    @Test
    @Disabled
    void deprecateApiKeyToday() {
        Assertions.assertDoesNotThrow(() -> voiceRssTtsService.deprecateApiKeyToday(ttsProperties.getApiKey1()));
        Assertions.assertEquals(TtsConstants.API_KEY_MAX_USE_TIME,
                voiceRssTtsService.queryTtsApiKeyUsed(ttsProperties.getApiKey1()));
    }

    @Test
    @Disabled
    void queryApiKeyUsed() {
        log.info("queryTtsApiKeyUsed [{}] used times is {}", ttsProperties.getApiKey10(),
                voiceRssTtsService.queryTtsApiKeyUsed(ttsProperties.getApiKey10()));
        log.info("queryTtsApiKeyUsed [{}] used times is {}", ttsProperties.getApiKey11(),
                voiceRssTtsService.queryTtsApiKeyUsed(ttsProperties.getApiKey11()));
        log.info("queryTtsApiKeyUsed [{}] used times is {}", ttsProperties.getApiKey12(),
                voiceRssTtsService.queryTtsApiKeyUsed(ttsProperties.getApiKey12()));
        log.info("queryTtsApiKeyUsed [{}] used times is {}", ttsProperties.getApiKey13(),
                voiceRssTtsService.queryTtsApiKeyUsed(ttsProperties.getApiKey13()));
        log.info("queryTtsApiKeyUsed [{}] used times is {}", ttsProperties.getApiKey14(),
                voiceRssTtsService.queryTtsApiKeyUsed(ttsProperties.getApiKey14()));
    }

    @Test
    @Disabled
    void refreshAllApiKey() {
        Assertions.assertDoesNotThrow(() -> voiceRssTtsService.refreshAllApiKey());
    }

    @Test
    void queryAllTtsApiKeyUsed() {
        log.info("queryTtsApiKeyUsed [total] used times is {}",
                voiceRssTtsService.queryTtsApiKeyUsed(TtsConstants.CACHE_KEY_PREFIX_TTS.TOTAL_API_KEY));
        for (String apiKey : ttsProperties.listApiKey()) {
            log.info("queryTtsApiKeyUsed [{}] used times is {}", apiKey, voiceRssTtsService.queryTtsApiKeyUsed(apiKey));
        }
    }

    @Test
    @Disabled
    void assertTtsApiKeyNotNull() {
        for (String apiKey : ttsProperties.listApiKey()) {
            assertNotNull(voiceRssTtsService.queryTtsApiKeyUsed(apiKey));
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
        byte[] data = voiceRssTtsService.speechEnglish("test voice rss");
        assertNotNull(data);
        try {
            Util.writeBytesToFileSystem(data, "/Users/zhanshifeng/Documents/temp/voice_rss_tts_test.mp3");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    @SneakyThrows
    void testSynthesizeTextAndSaveAudio() throws Exception {
        // generate a long English sentence that over 20 vocabulary for test
        String text = "A man in a red shirt and a black hat, standing on a grassy hill, looking at a group of people and a small dog, while walking with a girl in a blue shirt and a yellow hat, walking with a man in a green shirt and a white hat.";
        String languageCode = Locale.US.toLanguageTag();
        String voiceName = "en-US-Wavenet-D";
        String outputFileName = "test_output.wav";
        Path outputPath = Paths.get("src/test/resources", outputFileName);

        // Act
        byte[] audioBytes = googleTtsService.speechEnglish(text, voiceName);

        // Assert
        assertNotNull(audioBytes, "Audio bytes should not be null");
        assertTrue(audioBytes.length > 0, "Audio bytes should not be empty");

        // Save the audio to src/test/resources
        try (FileOutputStream fos = new FileOutputStream(outputPath.toFile())) {
            fos.write(audioBytes);
        }

        // Verify the file exists and has content
        File outputFile = outputPath.toFile();
        assertTrue(outputFile.exists(), "Output file should exist");
        assertTrue(outputFile.length() > 0, "Output file should not be empty");

        // Optional: Read the file back and verify its content matches
        byte[] savedAudioBytes = Files.readAllBytes(outputPath);
        assertArrayEquals(audioBytes, savedAudioBytes, "Saved audio should match the generated audio");
    }
}