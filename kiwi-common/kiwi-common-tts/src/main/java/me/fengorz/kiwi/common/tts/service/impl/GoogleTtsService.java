package me.fengorz.kiwi.common.tts.service.impl;

import com.google.cloud.texttospeech.v1.*;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.sdk.exception.tts.TtsException;
import me.fengorz.kiwi.common.tts.service.TtsService;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Slf4j
@Service("googleTtsService")
public class GoogleTtsService implements TtsService {

    private final TextToSpeechClient textToSpeechClient;

    public GoogleTtsService() throws Exception {
        // Initialize the client
        this.textToSpeechClient = TextToSpeechClient.create();
    }

    private byte[] synthesizeText(String text, String languageCode) throws Exception {
        // Set the text input to be synthesized
        SynthesisInput input = SynthesisInput.newBuilder().setText(text).build();

        // Build the voice request
        VoiceSelectionParams voice = VoiceSelectionParams.newBuilder()
                .setLanguageCode(languageCode) // e.g., "en-US"
                .setSsmlGender(SsmlVoiceGender.FEMALE)
                .build();

        // Select the audio configuration
        AudioConfig audioConfig = AudioConfig.newBuilder()
                .setAudioEncoding(AudioEncoding.LINEAR16) // WAV format
                .build();

        // Perform the text-to-speech request
        SynthesizeSpeechResponse response = textToSpeechClient.synthesizeSpeech(input, voice, audioConfig);

        // Get the audio content as a byte array
        ByteString audioContents = response.getAudioContent();
        return audioContents.toByteArray();
    }

    // Clean up resources
    public void shutdown() {
        textToSpeechClient.close();
    }

    @Override
    public byte[] speechEnglish(String text) throws TtsException {
        return speech(text, Locale.US);
    }

    private byte[] speech(String text, Locale locale) throws TtsException {
        String languageTag = locale.toLanguageTag();
        try {
            return this.synthesizeText(text, languageTag);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new TtsException(String.format("Call speech method of GoogleTtsService failed. locale=%s, text=%s", languageTag, text));
        }
    }

    @Override
    public byte[] speechChinese(String text) throws TtsException {
        return speech(text, Locale.CHINA);
    }

    @Override
    public boolean hasValidApiKey() {
        return true;
    }

}