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
package me.fengorz.kiwi.word.biz.service.base.impl;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import lombok.RequiredArgsConstructor;
import me.fengorz.kiwi.common.sdk.util.lang.collection.KiwiCollectionUtils;
import me.fengorz.kiwi.word.api.common.enumeration.ReviewAudioTypeEnum;
import me.fengorz.kiwi.word.api.entity.ParaphraseDO;
import me.fengorz.kiwi.word.api.entity.ParaphraseStarRelDO;
import me.fengorz.kiwi.word.biz.mapper.ParaphraseMapper;
import me.fengorz.kiwi.word.biz.mapper.ParaphraseStarRelMapper;
import me.fengorz.kiwi.word.biz.service.base.ParaphraseStarRelService;

/**
 * @author zhanshifeng
 * @date 2020-01-03 14:44:37
 */
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
                if (KiwiCollectionUtils.isEmpty(list)) {
                    return;
                }
                mapper.update(new ParaphraseStarRelDO().setParaphraseId(newRelId),
                    Wrappers.<ParaphraseStarRelDO>lambdaUpdate().in(ParaphraseStarRelDO::getParaphraseId, list));
            }
        }
    }

    @Override
    public List<Integer> listNotGeneratedVoice() {
        return mapper.listNotGeneratedVoice();
    }

    @Override
    public List<Integer> listNotAllGeneratedVoice() {
        return mapper.listNotAllGeneratedVoice(ReviewAudioTypeEnum.COMBO.getType());
    }

}
