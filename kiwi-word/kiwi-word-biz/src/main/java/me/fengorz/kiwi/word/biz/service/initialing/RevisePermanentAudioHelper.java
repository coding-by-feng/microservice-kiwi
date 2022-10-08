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

package me.fengorz.kiwi.word.biz.service.initialing;

import cn.hutool.core.map.MapUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.tts.enumeration.TtsSourceEnum;
import me.fengorz.kiwi.word.api.common.enumeration.RevisePermanentAudioEnum;
import me.fengorz.kiwi.word.api.entity.WordReviewAudioDO;
import me.fengorz.kiwi.word.biz.service.base.ReviewAudioService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @Description TODO
 * @Author zhanshifeng
 * @Date 2022/10/3 13:33
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RevisePermanentAudioHelper implements InitializingBean {

    private final ReviewAudioService reviewAudioService;
    @Getter
    private Map<String, RevisePermanentAudioEnum> permanentAudioEnumMap;
    @Getter
    private Set<RevisePermanentAudioEnum> permanentAudioEnums;
    @Getter
    private Map<RevisePermanentAudioEnum, WordReviewAudioDO> cacheStoreWithEnumKey;
    @Getter
    private Map<String, WordReviewAudioDO> cacheStoreWithStringKey;

    @Override
    public void afterPropertiesSet() throws Exception {
        this.permanentAudioEnumMap = MapUtil.<String, RevisePermanentAudioEnum>builder()
                .put("", RevisePermanentAudioEnum.WORD_CHARACTER_EMPTY)
                .put("adjective", RevisePermanentAudioEnum.WORD_CHARACTER_ADJECTIVE)
                .put("adj", RevisePermanentAudioEnum.WORD_CHARACTER_ADJ)
                .put("noun", RevisePermanentAudioEnum.WORD_CHARACTER_NOUN)
                .put("verb", RevisePermanentAudioEnum.WORD_CHARACTER_VERB)
                .put("adverb", RevisePermanentAudioEnum.WORD_CHARACTER_ADVERB)
                .put("conjunction", RevisePermanentAudioEnum.WORD_CHARACTER_CONJUNCTION)
                .put("plural", RevisePermanentAudioEnum.WORD_CHARACTER_PLURAL)
                .put("preposition", RevisePermanentAudioEnum.WORD_CHARACTER_PREPOSITION)
                .put("phrase", RevisePermanentAudioEnum.WORD_CHARACTER_PHRASE)
                .put("phrasal verb verb", RevisePermanentAudioEnum.WORD_CHARACTER_PHRASAL_VERB)
                .put("suffix", RevisePermanentAudioEnum.WORD_CHARACTER_SUFFIX)
                .put("exclamation", RevisePermanentAudioEnum.WORD_CHARACTER_EXCLAMATION)
                .put("prefix", RevisePermanentAudioEnum.WORD_CHARACTER_PREFIX)
                .put("determiner", RevisePermanentAudioEnum.WORD_CHARACTER_DETERMINER)
                .put("predeterminer", RevisePermanentAudioEnum.WORD_CHARACTER_PREDETERMINER)
                .put("pronoun", RevisePermanentAudioEnum.WORD_CHARACTER_PRONOUN)
                .put("auxiliary", RevisePermanentAudioEnum.WORD_CHARACTER_AUXILIARY)
                .build();

        this.permanentAudioEnums = new HashSet<>();
        this.cacheStoreWithEnumKey = new HashMap<>();
        this.cacheStoreWithStringKey = new HashMap<>();
        this.permanentAudioEnumMap.forEach((k, v) -> {
            this.permanentAudioEnums.add(v);
            WordReviewAudioDO audio = reviewAudioService.selectOne(v.getSourceId(), v.getType());
            this.cacheStoreWithEnumKey.put(v, audio);
            this.cacheStoreWithStringKey.put(k, audio);
        });
    }

    public TtsSourceEnum queryTtsSource(RevisePermanentAudioEnum audioEnum) {
        return TtsSourceEnum.BAIDU;
    }

    public boolean isExists(String characterCode) {
        return this.cacheStoreWithStringKey.containsKey(characterCode);
    }

}
