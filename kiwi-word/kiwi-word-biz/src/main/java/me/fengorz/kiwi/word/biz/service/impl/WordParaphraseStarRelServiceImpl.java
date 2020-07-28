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
package me.fengorz.kiwi.word.biz.service.impl;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import lombok.RequiredArgsConstructor;
import me.fengorz.kiwi.word.api.entity.WordParaphraseDO;
import me.fengorz.kiwi.word.api.entity.WordParaphraseStarRelDO;
import me.fengorz.kiwi.word.biz.mapper.WordParaphraseMapper;
import me.fengorz.kiwi.word.biz.mapper.WordParaphraseStarRelMapper;
import me.fengorz.kiwi.word.biz.service.IWordParaphraseStarRelService;

/**
 * @author zhanshifeng
 * @date 2020-01-03 14:44:37
 */
@Service
@RequiredArgsConstructor
public class WordParaphraseStarRelServiceImpl extends ServiceImpl<WordParaphraseStarRelMapper, WordParaphraseStarRelDO>
    implements IWordParaphraseStarRelService {

    private final WordParaphraseStarRelMapper wordParaphraseStarRelMapper;
    private final WordParaphraseMapper wordParaphraseMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replaceFetchResult(Integer oldRelId, Integer newRelId) {
        if (oldRelId == null || newRelId == null) {
            return;
        }

        int update = wordParaphraseStarRelMapper.update(new WordParaphraseStarRelDO().setParaphraseId(newRelId),
            Wrappers.<WordParaphraseStarRelDO>lambdaUpdate().eq(WordParaphraseStarRelDO::getParaphraseId, oldRelId));

        // 更新失败的话，可能是因为单词删除的逻辑出现异常，下面做补偿处理
        if (update < 1) {
            Optional.of(wordParaphraseMapper.selectById(newRelId)).ifPresent(paraphrase -> {
                LambdaQueryWrapper<WordParaphraseDO> wrapper = Wrappers.<WordParaphraseDO>lambdaQuery()
                    .eq(WordParaphraseDO::getParaphraseEnglish, paraphrase.getParaphraseEnglish())
                    .eq(WordParaphraseDO::getMeaningChinese, paraphrase.getMeaningChinese())
                    .eq(WordParaphraseDO::getIsHavePhrase, paraphrase.getIsHavePhrase());
                List<Integer> allStockId = wordParaphraseMapper.selectList(wrapper).stream()
                    .map(WordParaphraseDO::getParaphraseId).collect(Collectors.toList());
                wordParaphraseStarRelMapper.update(new WordParaphraseStarRelDO().setParaphraseId(newRelId), Wrappers
                    .<WordParaphraseStarRelDO>lambdaUpdate().in(WordParaphraseStarRelDO::getParaphraseId, allStockId));
            });
        }
    }
}
