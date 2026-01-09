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
package me.fengorz.kiwi.domain.ai.service;

import me.fengorz.kiwi.common.enumeration.AiPromptModeEnum;
import me.fengorz.kiwi.common.enumeration.LanguageEnum;

import java.util.function.Consumer;

/**
 * AI Streaming Service Interface for real-time AI responses
 *
 * @author codingByFeng
 */
public interface AiStreamingService {

    void streamCall(String prompt, AiPromptModeEnum promptMode, LanguageEnum targetLanguage,
                    LanguageEnum nativeLanguage, Consumer<String> onChunk, Consumer<Exception> onError,
                    Runnable onComplete);
}
