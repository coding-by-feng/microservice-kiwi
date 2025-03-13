/*
 *
 * Copyright [2019~2025] [codingByFeng]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 *
 */

package me.fengorz.kiwi.common.tts.service;

import me.fengorz.kiwi.common.sdk.exception.ServiceException;

/**
 * @Description TODO
 * @Author Kason Zhan
 * @Date 2022/7/5 22:33
 */
public interface TtsService extends TtsBaseService {

    default String autoSelectApiKey(){
        throw new ServiceException("Method autoSelectApiKey is not supported.");
    }


    default void increaseApiKeyUsedTime(String apiKey){
        throw new ServiceException("Method increaseApiKeyUsedTime is not supported.");
    }

    default Integer queryTtsApiKeyUsed(String apiKey){
        throw new ServiceException("Method queryTtsApiKeyUsed is not supported.");
    }

    @SuppressWarnings("UnusedReturnValue")
    default int useTtsApiKey(String apiKey, int time){
        throw new ServiceException("Method useTtsApiKey is not supported.");
    }

    default void deprecateApiKeyToday(String apiKey){
        throw new ServiceException("Method deprecateApiKeyToday is not supported.");
    }

    default void voiceRssGlobalIncreaseCounter(){
        throw new ServiceException("Method voiceRssGlobalIncreaseCounter is not supported.");
    }

    default boolean hasValidApiKey(){
        throw new ServiceException("Method hasValidApiKey is not supported.");
    }

    default void refreshAllApiKey(){
        throw new ServiceException("Method refreshAllApiKey is not supported.");
    }

}
