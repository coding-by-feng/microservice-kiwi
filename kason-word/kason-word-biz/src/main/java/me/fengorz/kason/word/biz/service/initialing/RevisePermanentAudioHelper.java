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

package me.fengorz.kason.word.biz.service.initialing;

import cn.hutool.core.map.MapUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kason.common.dfs.DfsService;
import me.fengorz.kason.common.dfs.DfsUtils;
import me.fengorz.kason.common.sdk.exception.dfs.DfsOperateException;
import me.fengorz.kason.common.sdk.exception.tts.TtsException;
import me.fengorz.kason.common.tts.enumeration.TtsSourceEnum;
import me.fengorz.kason.word.api.common.enumeration.RevisePermanentAudioEnum;
import me.fengorz.kason.word.api.entity.WordReviewAudioDO;
import me.fengorz.kason.word.biz.service.base.ReviewAudioService;
import me.fengorz.kason.word.biz.service.operate.AudioService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @Description TODO
 * @Author Kason Zhan
 * @Date 2022/10/3 13:33
 */
@Slf4j
@Service
public class RevisePermanentAudioHelper implements InitializingBean {

    private final ReviewAudioService reviewAudioService;
    private final DfsService dfsService;
    private final AudioService audioService;
    @Getter
    private Map<String, RevisePermanentAudioEnum> permanentAudioEnumMap;
    @Getter
    private Set<RevisePermanentAudioEnum> permanentAudioEnums;
    @Getter
    private Map<RevisePermanentAudioEnum, WordReviewAudioDO> cacheStoreWithEnumKey;
    @Getter
    private Map<String, WordReviewAudioDO> cacheStoreWithStringKey;
    private final ApplicationContext applicationContext;

    public RevisePermanentAudioHelper(ReviewAudioService reviewAudioService,
                                      @Qualifier("fastDfsService") DfsService dfsService,
                                      AudioService audioService,
                                      ApplicationContext applicationContext) {
        this.reviewAudioService = reviewAudioService;
        this.dfsService = dfsService;
        this.audioService = audioService;
        this.applicationContext = applicationContext;
    }

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
            if (audio == null) {
                log.warn("Could not find Audio for key: {}, value: {}", k, v);
            }
            log.info("Permanent Audio key: {}, value: {}, audio: {}", k, v, audio);

            try {
                byte[] bytes = this.dfsService.downloadFile(audio.getGroupName(), audio.getFilePath());
                log.info("Required wordReviewAudio bytes download success, characterCode={}, bytes length={}", k, bytes.length);
            } catch (DfsOperateException e) {
                log.error("downloadReviewAudio exception, characterCode={}!", k, e);
                try {
                    String uploadResult = audioService.generateChineseVoice(v.getText());
                    audio.setGroupName(DfsUtils.getGroupName(uploadResult));
                    audio.setFilePath(DfsUtils.getUploadVoiceFilePath(uploadResult));
                    reviewAudioService.updateById(audio);
                    log.info("Updated audio, characterCode={}, audio: {}", k, audio);
                } catch (DfsOperateException | TtsException ex) {
                    log.warn("Generate Chinese voice exception, characterCode={}, audio={}!", k, audio, ex);
                    log.warn("Critical error occurred, shutting down application", e);
                    // Exit with a non-zero status code (indicating abnormal termination)
//                    int exitCode = SpringApplication.exit(applicationContext, () -> 1);
//                    System.exit(exitCode);
                }
            }

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
