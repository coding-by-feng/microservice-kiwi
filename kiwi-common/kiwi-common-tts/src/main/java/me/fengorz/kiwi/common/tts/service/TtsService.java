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

import me.fengorz.kiwi.common.sdk.exception.tts.TtsException;

/**
 * @Description TODO
 * @Author Kason Zhan
 * @Date 2022/7/5 22:33
 */
public interface TtsService {

    byte[] speechEnglish(String apiKey, String text) throws TtsException;

    byte[] speechChinese(String apiKey, String text) throws TtsException;

    String autoSelectApiKey();

    void increaseApiKeyUsedTime(String apiKey);

    Integer queryTtsApiKeyUsed(String apiKey);

    @SuppressWarnings("UnusedReturnValue")
    int useTtsApiKey(String apiKey, int time);

    void deprecateApiKeyToday(String apiKey);

    void voiceRssGlobalIncreaseCounter();

    boolean hasValidApiKey();

    void refreshAllApiKey();

}
