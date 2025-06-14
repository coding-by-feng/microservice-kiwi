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
        log.info("Initializing GoogleTtsService...");
        try {
            // Initialize the client
            this.textToSpeechClient = TextToSpeechClient.create();
            log.info("GoogleTtsService initialized successfully");
        } catch (Exception e) {
            log.error("Failed to initialize GoogleTtsService: {}", e.getMessage(), e);
            throw e;
        }
    }

    private byte[] synthesizeText(String text, String languageCode) throws Exception {
        log.debug("Starting text synthesis - languageCode: {}, textLength: {}",
                languageCode, text != null ? text.length() : 0);

        if (text == null || text.trim().isEmpty()) {
            log.warn("Empty or null text provided for synthesis");
            throw new IllegalArgumentException("Text cannot be null or empty");
        }

        try {
            // Set the text input to be synthesized
            SynthesisInput input = SynthesisInput.newBuilder().setText(text).build();
            log.debug("Created synthesis input for text: '{}'",
                    text.length() > 50 ? text.substring(0, 50) + "..." : text);

            // Build the voice request
            VoiceSelectionParams voice = VoiceSelectionParams.newBuilder()
                    .setLanguageCode(languageCode) // e.g., "en-US"
                    .setSsmlGender(SsmlVoiceGender.FEMALE)
                    .build();
            log.debug("Voice selection configured - languageCode: {}, gender: FEMALE", languageCode);

            // Select the audio configuration
            AudioConfig audioConfig = AudioConfig.newBuilder()
                    .setAudioEncoding(AudioEncoding.LINEAR16) // WAV format
                    .build();
            log.debug("Audio configuration set to LINEAR16 encoding");

            // Perform the text-to-speech request
            log.info("Sending TTS request to Google Cloud - languageCode: {}, textLength: {}",
                    languageCode, text.length());
            SynthesizeSpeechResponse response = textToSpeechClient.synthesizeSpeech(input, voice, audioConfig);

            // Get the audio content as a byte array
            ByteString audioContents = response.getAudioContent();
            byte[] audioBytes = audioContents.toByteArray();

            log.info("TTS synthesis completed successfully - languageCode: {}, audioSize: {} bytes",
                    languageCode, audioBytes.length);
            log.debug("Audio content generated with {} bytes", audioBytes.length);

            return audioBytes;

        } catch (Exception e) {
            log.error("Text synthesis failed - languageCode: {}, error: {}",
                    languageCode, e.getMessage(), e);
            throw e;
        }
    }

    // Clean up resources
    public void shutdown() {
        log.info("Shutting down GoogleTtsService...");
        try {
            if (textToSpeechClient != null) {
                textToSpeechClient.close();
                log.info("GoogleTtsService shutdown completed successfully");
            } else {
                log.warn("TextToSpeechClient was null during shutdown");
            }
        } catch (Exception e) {
            log.error("Error during GoogleTtsService shutdown: {}", e.getMessage(), e);
        }
    }

    @Override
    public byte[] speechEnglish(String text) throws TtsException {
        log.info("Processing English TTS request - textLength: {}",
                text != null ? text.length() : 0);
        return speech(text, Locale.US);
    }

    private byte[] speech(String text, Locale locale) throws TtsException {
        String languageTag = locale.toLanguageTag();
        log.info("Processing TTS request - locale: {}, textLength: {}",
                languageTag, text != null ? text.length() : 0);

        if (text != null && log.isDebugEnabled()) {
            log.debug("TTS request content preview: '{}'",
                    text.length() > 100 ? text.substring(0, 100) + "..." : text);
        }

        try {
            byte[] result = this.synthesizeText(text, languageTag);
            log.info("TTS request completed successfully - locale: {}, resultSize: {} bytes",
                    languageTag, result.length);
            return result;
        } catch (Exception e) {
            log.error("TTS request failed - locale: {}, text: '{}', error: {}",
                    languageTag,
                    text != null && text.length() > 50 ? text.substring(0, 50) + "..." : text,
                    e.getMessage(), e);
            throw new TtsException(String.format("Call speech method of GoogleTtsService failed. locale=%s, text=%s",
                    languageTag, text));
        }
    }

    @Override
    public byte[] speechChinese(String text) throws TtsException {
        log.info("Processing Chinese TTS request - textLength: {}",
                text != null ? text.length() : 0);
        return speech(text, Locale.CHINA);
    }

    @Override
    public boolean hasValidApiKey() {
        log.debug("Checking API key validity");
        // Note: This always returns true. Consider implementing actual validation.
        log.debug("API key validation result: true");
        return true;
    }
}