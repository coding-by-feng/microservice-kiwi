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

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.bdf.core.service.SeqService;
import me.fengorz.kiwi.word.api.entity.WordReviewAudioDO;
import me.fengorz.kiwi.word.biz.mapper.ReviewAudioMapper;
import me.fengorz.kiwi.word.biz.service.base.ReviewAudioService;

/**
 * @author zhanShiFeng
 * @date 2020-09-16 16:56:42
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewAudioServiceImpl extends ServiceImpl<ReviewAudioMapper, WordReviewAudioDO>
    implements ReviewAudioService {

    private final ReviewAudioMapper mapper;
    private final SeqService seqService;

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public void cleanAndInsert(WordReviewAudioDO entity) {
        LambdaQueryWrapper<WordReviewAudioDO> condition = Wrappers.<WordReviewAudioDO>lambdaQuery()
            .eq(WordReviewAudioDO::getSourceId, entity.getSourceId()).eq(WordReviewAudioDO::getType, entity.getType());
        if (mapper.selectCount(condition) > 0) {
            this.cleanBySourceIdAndType(entity.getSourceId(), entity.getType());
        }
        mapper.insert(entity);
        log.info("wordReviewAudioDO insert success!, wordReviewAudioDO={}", entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public void cleanBySourceId(Integer sourceId) {
        LambdaQueryWrapper<WordReviewAudioDO> condition =
            Wrappers.<WordReviewAudioDO>lambdaQuery().eq(WordReviewAudioDO::getSourceId, sourceId);
        Integer count = mapper.selectCount(condition);
        log.info("Method removeWordReviewAudio count is: {}", count);
        if (count < 1) {
            return;
        }
        mapper.delete(condition);
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public void cleanById(Integer id) {
        mapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WordReviewAudioDO selectOne(Integer sourceId, Integer type) {
        return mapper.selectOne(Wrappers.<WordReviewAudioDO>lambdaQuery().eq(WordReviewAudioDO::getSourceId, sourceId)
            .eq(WordReviewAudioDO::getType, type));
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public void cleanBySourceIdAndType(Integer sourceId, Integer type) {
        LambdaQueryWrapper<WordReviewAudioDO> condition =
            Wrappers.<WordReviewAudioDO>lambdaQuery().eq(WordReviewAudioDO::getSourceId, sourceId);
        mapper.delete(condition);
        log.info("cleanBySourceIdAndType invoke success, sourceId={}, type={}", sourceId, type);
    }
}
