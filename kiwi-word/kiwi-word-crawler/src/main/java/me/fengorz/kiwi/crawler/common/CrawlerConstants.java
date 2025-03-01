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

package me.fengorz.kiwi.crawler.common;

import lombok.experimental.UtilityClass;

/**
 * @Description TODO
 * @Author Kason Zhan
 * @Date 2022/8/28 01:08
 */
@UtilityClass
public class CrawlerConstants {

    public interface COMPONENT_BEAN_ID {
        String GENERATE_REVIEW_RECORD_DAILY_SCHEDULER = "generateReviewRecordDailyScheduler";
        String REFRESH_ALL_API_KEY_DAILY_SCHEDULER = "refreshAllApiKeyDailyScheduler";
        String GENERATE_VOICE_NON_COLLECTED_SCHEDULER = "generateVoiceNonCollectedScheduler";
        String GENERATE_VOICE_ONLY_COLLECTED_SCHEDULER = "generateVoiceOnlyCollectedScheduler";
        String RE_GEN_INCORRECT_AUDIO_BY_VOICERSS_SCHEDULER = "reGenIncorrectAudioByVoicerssScheduler";
        String CACHE_WORD_SCHEDULER = "cacheWordScheduler";
    }

    public interface ENABLE_SCHEDULER_KEY {
        String VOICE_GENERATE_ONLY_COLLECTED = "voice-generate-only-collected";
        String VOICE_GENERATE_NON_COLLECTED = "voice-generate-non-collected";
        String CACHE_WORD = "cache-word";
        String REFRESH_ALL_API_KEY = "refresh-all-api-key";
        String REGEN_INCORRECT_AUDIO_BY_VOICERSS = "regen-incorrect-audio-by-voicerss";
    }

}
