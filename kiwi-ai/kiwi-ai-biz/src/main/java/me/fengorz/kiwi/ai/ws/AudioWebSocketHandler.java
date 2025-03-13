package me.fengorz.kiwi.ai.ws;

import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.ai.AiAudioService;
import me.fengorz.kiwi.common.sdk.exception.ai.OpenaiException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;

@Slf4j
@Component
public class AudioWebSocketHandler extends TextWebSocketHandler {

    private final AiAudioService aiAudioService;

    public AudioWebSocketHandler(@Qualifier("openaiAudioService") final AiAudioService aiAudioService) {
        this.aiAudioService = aiAudioService;
    }

    @Override
    public void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        byte[] audioBytes = message.getPayload().array();
        String transcript = transcribeAudio(audioBytes);
        try {
            session.sendMessage(new TextMessage(transcript));
        } catch (IOException e) {
            log.error("Failed to send audio transcript to client.", e);
            throw new OpenaiException("Failed to send audio transcript to client");
        }
    }

    private String transcribeAudio(byte[] audioBytes) {
        return aiAudioService.transcribeAudio(audioBytes);
    }
}