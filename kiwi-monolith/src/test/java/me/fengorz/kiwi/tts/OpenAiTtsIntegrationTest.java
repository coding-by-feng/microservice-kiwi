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

package me.fengorz.kiwi.tts;

import okhttp3.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * OpenAI TTS Integration Test
 * Standalone test that directly calls OpenAI TTS API
 *
 * @Author Kason Zhan
 * @Date 2026/1/11
 */
public class OpenAiTtsIntegrationTest {

    private OkHttpClient httpClient;
    private String apiKey;
    private static final String BASE_URL = "https://api.openai.com/v1/audio/speech";
    private static final String OUTPUT_DIR = "target/tts-output";
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json");

    @BeforeEach
    void setUp() {
        apiKey = System.getenv("OPENAI_API_KEY");

        httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        new File(OUTPUT_DIR).mkdirs();
    }

    @Test
    void testSpeechEnglish() throws Exception {
        if (apiKey == null || apiKey.isBlank()) {
            System.out.println("OPENAI_API_KEY not set. Skipping test.");
            return;
        }

        String englishText = "Hello! This is a test of the OpenAI text-to-speech API. The quick brown fox jumps over the lazy dog.";

        System.out.println("Generating English speech for: " + englishText);

        byte[] audioBytes = synthesize(englishText, "alloy");

        assertNotNull(audioBytes, "Audio bytes should not be null");
        assertTrue(audioBytes.length > 0, "Audio bytes should not be empty");

        File outputFile = new File(OUTPUT_DIR, "openai_test_english.mp3");
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            fos.write(audioBytes);
        }

        System.out.println("English audio saved to: " + outputFile.getAbsolutePath() + " (" + audioBytes.length + " bytes)");
    }

    @Test
    void testSpeechChinese() throws Exception {
        if (apiKey == null || apiKey.isBlank()) {
            System.out.println("OPENAI_API_KEY not set. Skipping test.");
            return;
        }

        String chineseText = "你好！这是一个OpenAI文字转语音API的测试。今天天气真好，适合出去散步。";

        System.out.println("Generating Chinese speech for: " + chineseText);

        byte[] audioBytes = synthesize(chineseText, "nova");

        assertNotNull(audioBytes, "Audio bytes should not be null");
        assertTrue(audioBytes.length > 0, "Audio bytes should not be empty");

        File outputFile = new File(OUTPUT_DIR, "openai_test_chinese.mp3");
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            fos.write(audioBytes);
        }

        System.out.println("Chinese audio saved to: " + outputFile.getAbsolutePath() + " (" + audioBytes.length + " bytes)");
    }

    @Test
    void testSpeechBothLanguages() throws Exception {
        if (apiKey == null || apiKey.isBlank()) {
            System.out.println("OPENAI_API_KEY not set. Skipping test.");
            return;
        }

        // Test English
        String englishText = "Learning a new language opens doors to new opportunities.";
        byte[] englishAudio = synthesize(englishText, "alloy");
        assertNotNull(englishAudio);
        assertTrue(englishAudio.length > 0);

        File englishFile = new File(OUTPUT_DIR, "openai_both_english.mp3");
        try (FileOutputStream fos = new FileOutputStream(englishFile)) {
            fos.write(englishAudio);
        }
        System.out.println("English: " + englishAudio.length + " bytes -> " + englishFile.getName());

        // Test Chinese
        String chineseText = "学习一门新语言可以打开通往新机遇的大门。";
        byte[] chineseAudio = synthesize(chineseText, "nova");
        assertNotNull(chineseAudio);
        assertTrue(chineseAudio.length > 0);

        File chineseFile = new File(OUTPUT_DIR, "openai_both_chinese.mp3");
        try (FileOutputStream fos = new FileOutputStream(chineseFile)) {
            fos.write(chineseAudio);
        }
        System.out.println("Chinese: " + chineseAudio.length + " bytes -> " + chineseFile.getName());

        System.out.println("Both language tests passed! Audio files saved to: " + OUTPUT_DIR);
    }

    private byte[] synthesize(String text, String voice) throws IOException {
        // Build JSON payload manually to avoid JSONException
        String jsonPayload = String.format(
                "{\"model\":\"tts-1\",\"input\":\"%s\",\"voice\":\"%s\",\"response_format\":\"mp3\"}",
                escapeJson(text), voice
        );

        RequestBody body = RequestBody.create(JSON_MEDIA_TYPE, jsonPayload);
        Request request = new Request.Builder()
                .url(BASE_URL)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "No error body";
                throw new IOException("OpenAI TTS request failed with status " + response.code() + ": " + errorBody);
            }

            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                throw new IOException("Response body is empty");
            }
            return responseBody.bytes();
        }
    }

    private String escapeJson(String text) {
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
