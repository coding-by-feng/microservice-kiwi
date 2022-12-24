/*
 *
 * Copyright [2019~2025] [zhanshifeng]
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

package me.fengorz.kiwi.word.biz.config;

import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.bdf.cache.redis.config.CacheConfig;
import me.fengorz.kiwi.bdf.core.config.CoreConfig;
import me.fengorz.kiwi.bdf.core.config.LogAspectConfig;
import me.fengorz.kiwi.common.es.config.ESConfig;
import me.fengorz.kiwi.common.fastdfs.config.DfsConfig;
import me.fengorz.kiwi.common.sdk.config.UtilsBeanConfiguration;
import me.fengorz.kiwi.common.tts.config.TtsConfig;
import me.fengorz.kiwi.common.tts.enumeration.TtsSourceEnum;
import me.fengorz.kiwi.word.api.common.enumeration.ReviseAudioTypeEnum;
import me.fengorz.kiwi.word.api.model.ParaphraseTtsGenerationPayload;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author zhanshifeng
 * @Date 2019/10/30 3:45 PM
 */
@Slf4j
@Configuration
@Import({CoreConfig.class, UtilsBeanConfiguration.class, LogAspectConfig.class, CacheConfig.class, DfsConfig.class,
        ESConfig.class, TtsConfig.class})
public class WordBizConfig {

    public WordBizConfig() {
        log.info("WordBizConfig...");
    }

    @Bean
    public ParaphraseTtsGenerationPayload paraphraseTtsGenerationPayload() {
        List<ImmutablePair<ReviseAudioTypeEnum, TtsSourceEnum>> pairs = new ArrayList<>();
        pairs.add(ImmutablePair.of(ReviseAudioTypeEnum.WORD_SPELLING, TtsSourceEnum.BAIDU));
        pairs.add(ImmutablePair.of(ReviseAudioTypeEnum.CHARACTER_CH, TtsSourceEnum.BAIDU));
        pairs.add(ImmutablePair.of(ReviseAudioTypeEnum.PARAPHRASE_CH, TtsSourceEnum.BAIDU));
        pairs.add(ImmutablePair.of(ReviseAudioTypeEnum.PARAPHRASE_EN, TtsSourceEnum.VOICERSS));
        pairs.add(ImmutablePair.of(ReviseAudioTypeEnum.EXAMPLE_CH, TtsSourceEnum.BAIDU));
        pairs.add(ImmutablePair.of(ReviseAudioTypeEnum.EXAMPLE_EN, TtsSourceEnum.VOICERSS));
        pairs.add(ImmutablePair.of(ReviseAudioTypeEnum.PHRASE_PRONUNCIATION, TtsSourceEnum.VOICERSS));

        List<ImmutablePair<ReviseAudioTypeEnum, Boolean>> isReplacePayload = new ArrayList<>();
        isReplacePayload.add(ImmutablePair.of(ReviseAudioTypeEnum.WORD_SPELLING, false));
        isReplacePayload.add(ImmutablePair.of(ReviseAudioTypeEnum.CHARACTER_CH, false));
        isReplacePayload.add(ImmutablePair.of(ReviseAudioTypeEnum.PARAPHRASE_CH, false));
        isReplacePayload.add(ImmutablePair.of(ReviseAudioTypeEnum.PARAPHRASE_EN, false));
        isReplacePayload.add(ImmutablePair.of(ReviseAudioTypeEnum.EXAMPLE_CH, false));
        isReplacePayload.add(ImmutablePair.of(ReviseAudioTypeEnum.EXAMPLE_EN, false));
        isReplacePayload.add(ImmutablePair.of(ReviseAudioTypeEnum.COMBO, true));
        isReplacePayload.add(ImmutablePair.of(ReviseAudioTypeEnum.PHRASE_PRONUNCIATION, false));

        List<ImmutablePair<ReviseAudioTypeEnum, Boolean>> enablePayload = new ArrayList<>();
        enablePayload.add(ImmutablePair.of(ReviseAudioTypeEnum.WORD_SPELLING, true));
        enablePayload.add(ImmutablePair.of(ReviseAudioTypeEnum.CHARACTER_CH, true));
        enablePayload.add(ImmutablePair.of(ReviseAudioTypeEnum.PARAPHRASE_CH, true));
        enablePayload.add(ImmutablePair.of(ReviseAudioTypeEnum.PARAPHRASE_EN, true));
        enablePayload.add(ImmutablePair.of(ReviseAudioTypeEnum.EXAMPLE_CH, true));
        enablePayload.add(ImmutablePair.of(ReviseAudioTypeEnum.EXAMPLE_EN, true));
        enablePayload.add(ImmutablePair.of(ReviseAudioTypeEnum.COMBO, false));
        enablePayload.add(ImmutablePair.of(ReviseAudioTypeEnum.PHRASE_PRONUNCIATION, true));

        List<ImmutablePair<ReviseAudioTypeEnum, Integer>> counters = new ArrayList<>();
        counters.add(ImmutablePair.of(ReviseAudioTypeEnum.PRONUNCIATION, 2));
        counters.add(ImmutablePair.of(ReviseAudioTypeEnum.WORD_SPELLING, 3));
        counters.add(ImmutablePair.of(ReviseAudioTypeEnum.PRONUNCIATION, 2));
        counters.add(ImmutablePair.of(ReviseAudioTypeEnum.WORD_SPELLING, 3));
        counters.add(ImmutablePair.of(ReviseAudioTypeEnum.CHARACTER_CH, 1));
        counters.add(ImmutablePair.of(ReviseAudioTypeEnum.PARAPHRASE_CH, 2));
        counters.add(ImmutablePair.of(ReviseAudioTypeEnum.PARAPHRASE_EN, 2));
        counters.add(ImmutablePair.of(ReviseAudioTypeEnum.PARAPHRASE_CH, 1));
        counters.add(ImmutablePair.of(ReviseAudioTypeEnum.PARAPHRASE_EN, 1));
        counters.add(ImmutablePair.of(ReviseAudioTypeEnum.EXAMPLE_CH, 2));
        counters.add(ImmutablePair.of(ReviseAudioTypeEnum.EXAMPLE_EN, 2));
        counters.add(ImmutablePair.of(ReviseAudioTypeEnum.EXAMPLE_CH, 1));
        counters.add(ImmutablePair.of(ReviseAudioTypeEnum.EXAMPLE_EN, 1));

        return ParaphraseTtsGenerationPayload.builder().pairs(ListUtils.unmodifiableList(pairs))
                .isReplacePayload(ListUtils.unmodifiableList(isReplacePayload))
                .counters(ListUtils.unmodifiableList(counters)).enablePayload(ListUtils.unmodifiableList(enablePayload))
                .build();
    }

}
