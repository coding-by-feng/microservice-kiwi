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

package me.fengorz.kiwi.crawler.component.scheduler;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.sdk.annotation.ScheduledAwake;
import me.fengorz.kiwi.crawler.common.CrawlerConstants;
import me.fengorz.kiwi.crawler.component.scheduler.base.Scheduler;
import me.fengorz.kiwi.word.api.feign.DictFetchApi;
import org.springframework.stereotype.Component;

@Slf4j
@Component(CrawlerConstants.COMPONENT_BEAN_ID.RE_GEN_INCORRECT_AUDIO_BY_VOICERSS_SCHEDULER)
@AllArgsConstructor
public class ReGenIncorrectAudioByVoicerssScheduler implements Scheduler {

    private final DictFetchApi dictFetchApi;

    @Override
    @ScheduledAwake(key = CrawlerConstants.ENABLE_SCHEDULER_KEY.REGEN_INCORRECT_AUDIO_BY_VOICERSS)
    public void schedule() {
        log.info("Voice reGenIncorrectAudioByVoicerss is starting.");
        dictFetchApi.reGenIncorrectAudioByVoicerss();
        log.info("Voice reGenIncorrectAudioByVoicerss has ended.");
    }

}
