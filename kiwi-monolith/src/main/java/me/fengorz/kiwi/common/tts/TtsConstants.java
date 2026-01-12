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
package me.fengorz.kiwi.common.tts;

/**
 * TTS Constants
 *
 * @author codingByFeng
 */
public final class TtsConstants {

    private TtsConstants() {}

    public static final String TTS_CACHE_NAMES = "kiwi-tts";

    public static final class CACHE_KEY_PREFIX {
        public static final String CLASS = "tts";
        public static final String SPEECH = "speech";

        private CACHE_KEY_PREFIX() {}
    }
}
