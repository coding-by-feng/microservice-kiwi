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
 * @Author Kason Zhan
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

    public interface BEAN_NAMES {
        String BAIDU_TTS_WEB_API_SERVICE_IMPL = "baiduTtsWebApiServiceImpl";
        String BAIDU_TTS_SERVICE_IMPL = "baiduTtsServiceImpl";
    }

    public interface BAIDU_TTS_OPTIONS {
        String ACCESS_TOKEN = "access_token";
        /**
         * 固定值zh。语言选择,目前只有中英文混合模式，填写固定值zh
         */
        String SPEAK_LANGUAGE = "lan";
        /**
         * 语速，取值0-15，默认为5中语速
         */
        String SPEAK_SPEED = "spd";
        /**
         * 音调，取值0-15，默认为5中语调
         */
        String SPEAK_TONE = "pit";
        /**
         * 音量，取值0-15，默认为5中音量（取值为0时为音量最小值，并非为无声）
         */
        String SPEAK_VOLUME = "vol";
        /**
         * 基础音库：度小宇=1，度小美=0，度逍遥（基础）=3，度丫丫=4
         * <p>
         * 精品音库：度逍遥（精品）=5003，度小鹿=5118，度博文=106，度小童=110，度小萌=111，度米朵=103，度小娇=5
         */
        String SPEAK_PER = "per";
        /**
         * 3为mp3格式(默认)； 4为pcm-16k；5为pcm-8k；6为wav（内容同pcm-16k）;
         * <p>
         * 注意aue=4或者6是语音识别要求的格式，但是音频内容不是语音识别要求的自然人发音，所以识别效果会受影响。
         */
        String SPEAK_AUE = "aue";
    }

}
