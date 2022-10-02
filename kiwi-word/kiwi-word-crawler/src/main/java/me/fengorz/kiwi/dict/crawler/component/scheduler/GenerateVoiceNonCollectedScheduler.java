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

package me.fengorz.kiwi.dict.crawler.component.scheduler;

import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.sdk.annotation.ScheduledAwake;
import me.fengorz.kiwi.dict.crawler.common.CrawlerConstants;
import me.fengorz.kiwi.dict.crawler.component.scheduler.base.Scheduler;
import me.fengorz.kiwi.word.api.common.enumeration.ReviewAudioGenerationEnum;
import me.fengorz.kiwi.word.api.feign.DictFetchApi;

/**
 * @Description TODO
 * @Author zhanshifeng
 * @Date 2022/7/4 23:03
 */
@Slf4j
@Component(CrawlerConstants.COMPONENT_BEAN_ID.GENERATE_VOICE_NON_COLLECTED_SCHEDULER)
@AllArgsConstructor
public class GenerateVoiceNonCollectedScheduler implements Scheduler {

    private final DictFetchApi dictFetchApi;

    @Override
    @ScheduledAwake(key = CrawlerConstants.ENABLE_SCHEDULER_KEY.VOICE_GENERATE_NON_COLLECTED)
    public void schedule() {
        log.info("Voice generation is starting.");
        dictFetchApi.generateTtsVoice(ReviewAudioGenerationEnum.NON_COLLECTED.getType());
        log.info("Voice generation has ended.");
    }

}
