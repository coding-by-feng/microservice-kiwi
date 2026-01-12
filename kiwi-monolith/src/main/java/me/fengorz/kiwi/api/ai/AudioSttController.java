/*
 * Copyright [2019~2025] [codingByFeng]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.fengorz.kiwi.api.ai;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.R;
import me.fengorz.kiwi.domain.ai.service.AiAudioService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Audio Speech-to-Text Controller
 * Replaces AudioWebSocketHandler with REST endpoint
 * Note: SSE is not suitable for binary audio uploads, so this uses regular REST
 *
 * @author codingByFeng
 */
@Slf4j
@RestController
@RequestMapping("/api/ai/audio")
@RequiredArgsConstructor
@Tag(name = "Audio STT", description = "Audio speech-to-text operations")
public class AudioSttController {

    private static final String LOG_PREFIX = "[AUDIO-STT]";

    private final AiAudioService aiAudioService;

    /**
     * Convert audio file to text
     *
     * @param file     the audio file (supports mp3, wav, webm, m4a, etc.)
     * @param language optional language hint for better accuracy
     * @return transcribed text
     */
    @PostMapping(value = "/speech-to-text", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Convert audio to text", description = "Upload audio file and get transcribed text")
    public R<String> speechToText(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "language", required = false) String language) {

        log.info("{} Speech-to-text request - filename: {}, size: {} bytes, language: {}",
                LOG_PREFIX, file.getOriginalFilename(), file.getSize(), language);

        if (file.isEmpty()) {
            return R.failed("Audio file is required");
        }

        try {
            byte[] audioBytes = file.getBytes();
            String transcript;

            if (language != null && !language.isEmpty()) {
                transcript = aiAudioService.speechToText(audioBytes, language);
            } else {
                transcript = aiAudioService.speechToText(audioBytes);
            }

            log.info("{} Transcription completed - length: {} chars", LOG_PREFIX, transcript.length());
            return R.ok(transcript);

        } catch (IOException e) {
            log.error("{} Failed to read audio file: {}", LOG_PREFIX, e.getMessage());
            return R.failed("Failed to read audio file: " + e.getMessage());
        } catch (Exception e) {
            log.error("{} Failed to transcribe audio: {}", LOG_PREFIX, e.getMessage());
            return R.failed("Failed to transcribe audio: " + e.getMessage());
        }
    }

    /**
     * Convert raw audio bytes to text
     * Useful for direct audio streaming from clients
     *
     * @param audioBytes raw audio bytes in request body
     * @param language   optional language hint
     * @return transcribed text
     */
    @PostMapping(value = "/speech-to-text/raw", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @Operation(summary = "Convert raw audio bytes to text", description = "Send raw audio bytes and get transcribed text")
    public R<String> speechToTextRaw(
            @RequestBody byte[] audioBytes,
            @RequestParam(value = "language", required = false) String language) {

        log.info("{} Raw speech-to-text request - size: {} bytes, language: {}",
                LOG_PREFIX, audioBytes.length, language);

        if (audioBytes == null || audioBytes.length == 0) {
            return R.failed("Audio data is required");
        }

        try {
            String transcript;

            if (language != null && !language.isEmpty()) {
                transcript = aiAudioService.speechToText(audioBytes, language);
            } else {
                transcript = aiAudioService.speechToText(audioBytes);
            }

            log.info("{} Transcription completed - length: {} chars", LOG_PREFIX, transcript.length());
            return R.ok(transcript);

        } catch (Exception e) {
            log.error("{} Failed to transcribe audio: {}", LOG_PREFIX, e.getMessage());
            return R.failed("Failed to transcribe audio: " + e.getMessage());
        }
    }

    /**
     * Text-to-speech: Convert text to audio
     *
     * @param text  the text to convert
     * @param voice optional voice selection
     * @return audio bytes (MP3 format)
     */
    @PostMapping(value = "/text-to-speech", produces = "audio/mpeg")
    @Operation(summary = "Convert text to speech", description = "Convert text to audio and return MP3 bytes")
    public byte[] textToSpeech(
            @RequestParam("text") String text,
            @RequestParam(value = "voice", required = false) String voice) {

        log.info("{} Text-to-speech request - text length: {}, voice: {}", LOG_PREFIX, text.length(), voice);

        if (text == null || text.isEmpty()) {
            throw new IllegalArgumentException("Text is required");
        }

        try {
            byte[] audioBytes;

            if (voice != null && !voice.isEmpty()) {
                audioBytes = aiAudioService.textToSpeech(text, voice);
            } else {
                audioBytes = aiAudioService.textToSpeech(text);
            }

            log.info("{} TTS completed - audio size: {} bytes", LOG_PREFIX, audioBytes.length);
            return audioBytes;

        } catch (Exception e) {
            log.error("{} Failed to generate speech: {}", LOG_PREFIX, e.getMessage());
            throw new RuntimeException("Failed to generate speech: " + e.getMessage());
        }
    }
}
