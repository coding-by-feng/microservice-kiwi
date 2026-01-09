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
package me.fengorz.kiwi.domain.ai.service.impl;

import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.domain.ai.service.AiAudioService;
import org.springframework.stereotype.Service;

/**
 * AI Audio Service Implementation (Stub)
 * TODO: Implement actual TTS/STT functionality
 *
 * @author codingByFeng
 */
@Slf4j
@Service("aiAudioService")
public class AiAudioServiceImpl implements AiAudioService {

    @Override
    public byte[] textToSpeech(String text) {
        log.warn("textToSpeech not implemented yet");
        return new byte[0];
    }

    @Override
    public byte[] textToSpeech(String text, String voice) {
        log.warn("textToSpeech with voice not implemented yet");
        return new byte[0];
    }

    @Override
    public String speechToText(byte[] audio) {
        log.warn("speechToText not implemented yet");
        return "Speech-to-Text not implemented";
    }

    @Override
    public String speechToText(byte[] audio, String language) {
        log.warn("speechToText with language not implemented yet");
        return "Speech-to-Text not implemented";
    }
}
