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

package me.fengorz.kiwi.common.tts;

import lombok.experimental.UtilityClass;

/**
 * @Description TODO
 * @Author zhanshifeng
 * @Date 2022/8/20 18:35
 */
@UtilityClass
public class TtsConstants {

    public static final int API_KEY_MAX_USE_TIME = 350;

    public static final String CACHE_NAMES = "tts";

    public interface CACHE_KEY_PREFIX_TTS {
        String TOTAL_API_KEY = "total_api_key";
        String TTS_VOICE_RSS_API_KEY_USED_TIME = "TTS_VOICE_RSS_API_KEY";
    }

}
