package me.fengorz.kason.ai.service;

import me.fengorz.kason.common.sdk.enumeration.AiPromptModeEnum;
import me.fengorz.kason.common.sdk.enumeration.LanguageEnum;

import java.util.function.Consumer;

/**
 * AI Streaming Service interface for real-time AI responses
 * @Author Kason Zhan
 * @Date 06/03/2025
 */
public interface AiStreamingService {

    /**
     * Stream AI response with target and native languages
     *
     * @param prompt The input prompt
     * @param promptMode The prompt mode
     * @param targetLanguage The target language
     * @param nativeLanguage The native language
     * @param onChunk Callback for each text chunk received
     * @param onError Callback for error handling
     * @param onComplete Callback when streaming is complete
     */
    void streamCall(String prompt, AiPromptModeEnum promptMode, LanguageEnum targetLanguage, 
                   LanguageEnum nativeLanguage, Consumer<String> onChunk, Consumer<Exception> onError, 
                   Runnable onComplete);
}