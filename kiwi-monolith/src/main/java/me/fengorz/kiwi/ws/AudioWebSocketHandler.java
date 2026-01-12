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
package me.fengorz.kiwi.ws;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.domain.ai.service.AiAudioService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;

/**
 * Audio WebSocket Handler for Speech-to-Text
 *
 * @author codingByFeng
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AudioWebSocketHandler extends TextWebSocketHandler {

    private static final String LOG_PREFIX = "[AUDIO-WS]";

    private final AiAudioService aiAudioService;

    @Override
    public void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        log.debug("{} Received binary audio message - SessionId: {}, Size: {} bytes",
                LOG_PREFIX, session.getId(), message.getPayloadLength());

        byte[] audioBytes = message.getPayload().array();
        try {
            String transcript = aiAudioService.speechToText(audioBytes);
            session.sendMessage(new TextMessage(transcript));
            log.debug("{} Sent transcript - SessionId: {}, Length: {}",
                    LOG_PREFIX, session.getId(), transcript.length());
        } catch (IOException e) {
            log.error("{} Failed to send transcript: {}", LOG_PREFIX, e.getMessage());
        } catch (Exception e) {
            log.error("{} Failed to transcribe audio: {}", LOG_PREFIX, e.getMessage());
            try {
                session.sendMessage(new TextMessage("Error: " + e.getMessage()));
            } catch (IOException ex) {
                log.error("{} Failed to send error message: {}", LOG_PREFIX, ex.getMessage());
            }
        }
    }
}
