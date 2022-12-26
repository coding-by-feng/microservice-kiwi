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

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.bdf.core.service.SeqService;
import me.fengorz.kiwi.common.fastdfs.service.DfsService;
import me.fengorz.kiwi.common.sdk.constant.GlobalConstants;
import me.fengorz.kiwi.common.sdk.exception.dfs.DfsOperateDeleteException;
import me.fengorz.kiwi.common.tts.enumeration.TtsSourceEnum;
import me.fengorz.kiwi.word.api.common.enumeration.ReviseAudioTypeEnum;
import me.fengorz.kiwi.word.api.entity.WordReviewAudioDO;
import me.fengorz.kiwi.word.api.vo.ParaphraseExampleVO;
import me.fengorz.kiwi.word.biz.mapper.ReviewAudioMapper;
import me.fengorz.kiwi.word.biz.service.base.ParaphraseExampleService;
import me.fengorz.kiwi.word.biz.service.base.ReviewAudioService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
    private final ParaphraseExampleService paraphraseExampleService;
    private final SeqService seqService;
    private final DfsService dfsService;

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
        List<Integer> sourceIds = new ArrayList<>();
        sourceIds.add(sourceId);
        ListUtils.emptyIfNull(paraphraseExampleService.listExamples(sourceId))
                .stream().map(ParaphraseExampleVO::getExampleId).collect(Collectors.toCollection(() -> sourceIds));

        LambdaQueryWrapper<WordReviewAudioDO> condition =
                Wrappers.<WordReviewAudioDO>lambdaQuery().in(WordReviewAudioDO::getSourceId, sourceIds);
        List<WordReviewAudioDO> list = mapper.selectList(condition);
        log.info("Method removeWordReviewAudio list size is: {}", list.size());
        if (CollectionUtils.isEmpty(list)) {
            return;
        }
        for (WordReviewAudioDO wordReviewAudioDO : list) {
            try {
                dfsService.deleteFile(wordReviewAudioDO.getGroupName(), wordReviewAudioDO.getFilePath());
            } catch (DfsOperateDeleteException e) {
                log.error("Method deleteFile invoked failed, audioId = {}", wordReviewAudioDO.getId());
            }
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
        log.info("Method selectOne invoke, sourceId={}, type={}", sourceId, type);
        return mapper.selectOne(Wrappers.<WordReviewAudioDO>lambdaQuery().eq(WordReviewAudioDO::getSourceId, sourceId)
                .eq(WordReviewAudioDO::getType, type));
    }

    @Override
    public List<WordReviewAudioDO> list(Integer sourceId, Integer type) {
        return mapper.selectList(Wrappers.<WordReviewAudioDO>lambdaQuery().eq(WordReviewAudioDO::getSourceId, sourceId)
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

    @Override
    public List<WordReviewAudioDO> listIncorrectAudioByVoicerss(ReviseAudioTypeEnum type) {
        LambdaQueryWrapper<WordReviewAudioDO> condition =
                Wrappers.<WordReviewAudioDO>lambdaQuery().eq(WordReviewAudioDO::getSourceUrl, TtsSourceEnum.VOICERSS.getSource())
                        .eq(WordReviewAudioDO::getType, type.getType())
                        .eq(WordReviewAudioDO::getIsDel, GlobalConstants.FLAG_DEL_NO);
        IPage<WordReviewAudioDO> page = new Page<>(0, 10);
        List<WordReviewAudioDO> records = mapper.selectPage(page, condition).getRecords();
        log.info("listIncorrectAudioByVoicerss[{}] invoke success, size={}", type.name(), records.size());
        return records;
    }
}
