/*
 *
 * Copyright [2019~2025] [codingByFeng]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package me.fengorz.kiwi.common.tts.service.impl;

import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.tts.model.OpenAiTtsProperties;
import okhttp3.OkHttpClient;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * OpenAI TTS Service Test
 * Tests English and Chinese text-to-speech generation
 *
 * @Author Kason Zhan
 * @Date 2026/1/11
 */
@Slf4j
public class OpenAiTtsServiceTest {

    private OpenAiTtsService openAiTtsService;
    private static final String OUTPUT_DIR = "target/tts-output";

    @BeforeEach
    void setUp() {
        // Get API key from environment variable
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("OPENAI_API_KEY environment variable is not set. Tests will be skipped.");
            return;
        }

        // Configure properties
        OpenAiTtsProperties properties = new OpenAiTtsProperties();
        properties.setApiKey(apiKey);
        properties.setModel("tts-1");  // Use standard model for testing (cheaper)
        properties.setEnglishVoice("alloy");
        properties.setChineseVoice("nova");
        properties.setResponseFormat("mp3");
        properties.setSpeed(1.0);

        // Create HTTP client with timeout
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        openAiTtsService = new OpenAiTtsService(properties, httpClient);

        // Create output directory
        new File(OUTPUT_DIR).mkdirs();
    }

    @Test
    void testSpeechEnglish() throws Exception {
        if (openAiTtsService == null) {
            log.warn("Skipping test: OPENAI_API_KEY not configured");
            return;
        }

        String englishText = "Hello! This is a test of the OpenAI text-to-speech API. " +
                "The quick brown fox jumps over the lazy dog.";

        log.info("Generating English speech for: {}", englishText);

        byte[] audioBytes = openAiTtsService.speechEnglish(englishText);

        assertNotNull(audioBytes, "Audio bytes should not be null");
        assertTrue(audioBytes.length > 0, "Audio bytes should not be empty");

        // Save to file for manual verification
        File outputFile = new File(OUTPUT_DIR, "test_english.mp3");
        FileUtils.writeByteArrayToFile(outputFile, audioBytes);

        log.info("English audio saved to: {} ({} bytes)", outputFile.getAbsolutePath(), audioBytes.length);
    }

    @Test
    void testSpeechChinese() throws Exception {
        if (openAiTtsService == null) {
            log.warn("Skipping test: OPENAI_API_KEY not configured");
            return;
        }

        String chineseText = "你好！这是一个OpenAI文字转语音API的测试。今天天气真好，适合出去散步。";

        log.info("Generating Chinese speech for: {}", chineseText);

        byte[] audioBytes = openAiTtsService.speechChinese(chineseText);

        assertNotNull(audioBytes, "Audio bytes should not be null");
        assertTrue(audioBytes.length > 0, "Audio bytes should not be empty");

        // Save to file for manual verification
        File outputFile = new File(OUTPUT_DIR, "test_chinese.mp3");
        FileUtils.writeByteArrayToFile(outputFile, audioBytes);

        log.info("Chinese audio saved to: {} ({} bytes)", outputFile.getAbsolutePath(), audioBytes.length);
    }

    @Test
    void testSpeechBothLanguages() throws Exception {
        if (openAiTtsService == null) {
            log.warn("Skipping test: OPENAI_API_KEY not configured");
            return;
        }

        // Test English
        String englishText = "Learning a new language opens doors to new opportunities.";
        byte[] englishAudio = openAiTtsService.speechEnglish(englishText);
        assertNotNull(englishAudio);
        assertTrue(englishAudio.length > 0);

        File englishFile = new File(OUTPUT_DIR, "test_both_english.mp3");
        FileUtils.writeByteArrayToFile(englishFile, englishAudio);
        log.info("English: {} bytes -> {}", englishAudio.length, englishFile.getName());

        // Test Chinese
        String chineseText = "学习一门新语言可以打开通往新机遇的大门。";
        byte[] chineseAudio = openAiTtsService.speechChinese(chineseText);
        assertNotNull(chineseAudio);
        assertTrue(chineseAudio.length > 0);

        File chineseFile = new File(OUTPUT_DIR, "test_both_chinese.mp3");
        FileUtils.writeByteArrayToFile(chineseFile, chineseAudio);
        log.info("Chinese: {} bytes -> {}", chineseAudio.length, chineseFile.getName());

        log.info("Both language tests passed! Audio files saved to: {}", OUTPUT_DIR);
    }
}
