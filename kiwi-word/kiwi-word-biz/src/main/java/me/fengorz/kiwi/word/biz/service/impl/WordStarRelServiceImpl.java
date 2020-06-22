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

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import lombok.RequiredArgsConstructor;
import me.fengorz.kiwi.common.sdk.util.lang.collection.KiwiCollectionUtils;
import me.fengorz.kiwi.word.api.entity.WordStarRelDO;
import me.fengorz.kiwi.word.biz.mapper.WordStarRelMapper;
import me.fengorz.kiwi.word.biz.service.IWordStarRelService;

/**
 * 单词本与单词的关联表
 *
 * @author zhanshifeng
 * @date 2020-01-03 14:39:28
 */
@Service
@RequiredArgsConstructor
public class WordStarRelServiceImpl extends ServiceImpl<WordStarRelMapper, WordStarRelDO>
    implements IWordStarRelService {

    private final WordStarRelMapper wordStarRelMapper;

    @Override
    public List<Integer> findAllWordId(Integer listId) {
        List<WordStarRelDO> list =
            wordStarRelMapper.selectList(new LambdaQueryWrapper<WordStarRelDO>().eq(WordStarRelDO::getListId, listId));
        if (KiwiCollectionUtils.isNotEmpty(list)) {
            List<Integer> result = new ArrayList<>();
            for (WordStarRelDO relDO : list) {
                result.add(relDO.getWordId());
            }
            return result;
        }
        return null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replaceFetchResult(Integer oldRelId, Integer newRelId) {
        if (oldRelId == null || newRelId == null) {
            return;
        }
        wordStarRelMapper.update(new WordStarRelDO().setWordId(newRelId),
            Wrappers.<WordStarRelDO>lambdaUpdate().eq(WordStarRelDO::getWordId, oldRelId));
    }

}
