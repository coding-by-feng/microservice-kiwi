/*
 *
 *   Copyright [2019~2025] [codingByFeng]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 *
 */

package me.fengorz.kiwi.common.tts.service;

import me.fengorz.kiwi.common.sdk.exception.tts.TtsException;

/**
 * @Description TODO
 * @Author Kason Zhan
 * @Date 2022/10/1 11:22
 */
public interface BaiduTtsService {

    byte[] speech(String text) throws TtsException;

    String SPEAK_SPEED_DEFAULT_VALUE = "5";
    String SPEAK_TONE_DEFAULT_VALUE = "5";
    String SPEAK_PER_DEFAULT_VALUE_DUYAYA = "4";
    String SPEAK_PER_DEFAULT_VALUE_DUXIAOMEI = "0";
    String SPEAK_AUE_DEFAULT_VALUE = "3";
    String SPEAK_LANGUAGE_DEFAULT_VALUE = "zh";

}
