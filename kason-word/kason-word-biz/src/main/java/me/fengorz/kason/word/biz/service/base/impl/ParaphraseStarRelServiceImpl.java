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
package me.fengorz.kason.word.biz.service.base.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kason.common.sdk.util.lang.collection.KasonCollectionUtils;
import me.fengorz.kason.word.api.common.enumeration.ReviseAudioTypeEnum;
import me.fengorz.kason.word.api.entity.ParaphraseDO;
import me.fengorz.kason.word.api.entity.ParaphraseStarRelDO;
import me.fengorz.kason.word.biz.mapper.ParaphraseMapper;
import me.fengorz.kason.word.biz.mapper.ParaphraseStarRelMapper;
import me.fengorz.kason.word.biz.service.base.ParaphraseStarRelService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author zhanshifeng
 * @date 2020-01-03 14:44:37
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ParaphraseStarRelServiceImpl extends ServiceImpl<ParaphraseStarRelMapper, ParaphraseStarRelDO>
        implements ParaphraseStarRelService {

    private final ParaphraseStarRelMapper mapper;
    private final ParaphraseMapper paraphraseMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replaceFetchResult(Integer oldRelId, Integer newRelId) {
        if (oldRelId == null || newRelId == null) {
            return;
        }

        int update = mapper.update(new ParaphraseStarRelDO().setParaphraseId(newRelId),
                Wrappers.<ParaphraseStarRelDO>lambdaUpdate().eq(ParaphraseStarRelDO::getParaphraseId, oldRelId));

        // 更新失败的话，可能是因为单词删除的逻辑出现异常，下面做补偿处理
        if (update < 1) {
            ParaphraseDO paraphrase = Optional.of(paraphraseMapper.selectById(newRelId)).get();
            LambdaQueryWrapper<ParaphraseDO> wrapper = Wrappers.<ParaphraseDO>lambdaQuery()
                    .eq(ParaphraseDO::getParaphraseEnglish, paraphrase.getParaphraseEnglish())
                    .eq(ParaphraseDO::getMeaningChinese, paraphrase.getMeaningChinese())
                    .eq(ParaphraseDO::getIsHavePhrase, paraphrase.getIsHavePhrase());
            List<Integer> allStockId = paraphraseMapper.selectList(wrapper).stream().map(ParaphraseDO::getParaphraseId)
                    .collect(Collectors.toList());
            update = mapper.update(new ParaphraseStarRelDO().setParaphraseId(newRelId),
                    Wrappers.<ParaphraseStarRelDO>lambdaUpdate().in(ParaphraseStarRelDO::getParaphraseId, allStockId));

            if (update < 1) {
                List<Integer> list = paraphraseMapper.selectList(wrapper.orderByDesc(ParaphraseDO::getParaphraseId))
                        .stream().map(ParaphraseDO::getParaphraseId).filter(id -> !id.equals(newRelId))
                        .collect(Collectors.toList());
                if (KasonCollectionUtils.isEmpty(list)) {
                    return;
                }
                mapper.update(new ParaphraseStarRelDO().setParaphraseId(newRelId),
                        Wrappers.<ParaphraseStarRelDO>lambdaUpdate().in(ParaphraseStarRelDO::getParaphraseId, list));
            }
        }
    }

    @Override
    public List<Integer> listNotGeneratedVoice() {
        List<Integer> result = mapper.listNotGeneratedVoice();
        log.info("Method listNotGeneratedVoice is invoking, result size={}", result.size());
        result.forEach(id -> log.info(String.valueOf(id)));
        return result;
    }

    @Override
    public List<Integer> listNotAllGeneratedVoice() {
        List<Integer> result = mapper.listNotAllGeneratedVoice(ReviseAudioTypeEnum.COMBO.getType());
        log.info("Method listNotAllGeneratedVoice is invoking, result size={}", result.size());
        result.forEach(id -> log.info(String.valueOf(id)));
        return result;
    }

    @Override
    public List<Integer> listNotGeneratedPronunciationVoiceForPhrase() {
        List<Integer> result = mapper.listNotGeneratedPronunciationVoiceForPhrase(ReviseAudioTypeEnum.PHRASE_PRONUNCIATION.getType());
        log.info("Method listNotGeneratedPronunciationVoiceForPhrase is invoking, result size={}", result.size());
        result.forEach(id -> log.info(String.valueOf(id)));
        return result;
    }

}
