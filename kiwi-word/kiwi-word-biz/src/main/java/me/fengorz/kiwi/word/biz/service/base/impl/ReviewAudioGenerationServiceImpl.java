/*
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
 */
package me.fengorz.kiwi.word.biz.service.base.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.db.service.SeqService;
import me.fengorz.kiwi.common.sdk.constant.GlobalConstants;
import me.fengorz.kiwi.word.api.common.enumeration.ReviseAudioTypeEnum;
import me.fengorz.kiwi.word.api.entity.WordReviewAudioGenerationDO;
import me.fengorz.kiwi.word.biz.mapper.ReviewAudioGenerationMapper;
import me.fengorz.kiwi.word.biz.service.base.ReviewAudioGenerationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author zhanShiFeng
 * @date 2020-09-16 16:56:42
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewAudioGenerationServiceImpl extends
    ServiceImpl<ReviewAudioGenerationMapper, WordReviewAudioGenerationDO> implements ReviewAudioGenerationService {

    private final ReviewAudioGenerationMapper reviewAudioGenerationMapper;
    private final SeqService seqService;

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public void markGenerateFinish(Integer sourceId, Integer audioId, ReviseAudioTypeEnum type) {
        mark(sourceId, audioId, type, GlobalConstants.FLAG_YES);
        log.info("Method markGenerateFinish invoked success, sourceId={}, audioId={}, type={}", sourceId, audioId,
            type.name());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markGenerateNotFinish(Integer sourceId, Integer audioId, ReviseAudioTypeEnum type) {
        mark(sourceId, audioId, type, GlobalConstants.FLAG_NO);
        log.info("Method markGenerateNotFinish invoked success, sourceId={}, audioId={}, type={}", sourceId, audioId,
            type.name());
    }

    private void mark(Integer sourceId, Integer audioId, ReviseAudioTypeEnum type, Integer isFinish) {
        WordReviewAudioGenerationDO wordReviewAudioGenerationDO = reviewAudioGenerationMapper.selectOne(
            Wrappers.<WordReviewAudioGenerationDO>lambdaQuery().eq(WordReviewAudioGenerationDO::getSourceId, sourceId)
                .eq(WordReviewAudioGenerationDO::getType, type.getType()));
        if (wordReviewAudioGenerationDO == null) {
            reviewAudioGenerationMapper
                .insert(new WordReviewAudioGenerationDO().setId(seqService.genCommonIntSequence()).setSourceId(sourceId)
                    .setAudioId(audioId).setIsFinish(isFinish).setType(type.getType()));
        } else {
            if (GlobalConstants.FLAG_YES != wordReviewAudioGenerationDO.getIsFinish()) {
                wordReviewAudioGenerationDO.setIsFinish(isFinish);
            }
            wordReviewAudioGenerationDO.setAudioId(audioId);
            reviewAudioGenerationMapper.updateById(wordReviewAudioGenerationDO);
        }
    }

}
