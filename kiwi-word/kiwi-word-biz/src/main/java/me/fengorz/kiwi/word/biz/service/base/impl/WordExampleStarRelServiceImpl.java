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

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import me.fengorz.kiwi.word.api.entity.ExampleStarRelDO;
import me.fengorz.kiwi.word.biz.mapper.ExampleStarRelMapper;
import me.fengorz.kiwi.word.biz.service.base.IWordExampleStarRelService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 释义例句本与例句关系表
 *
 * @author zhanshifeng
 * @date 2020-01-03 14:48:48
 */
@Service
@RequiredArgsConstructor
public class WordExampleStarRelServiceImpl extends ServiceImpl<ExampleStarRelMapper, ExampleStarRelDO>
    implements IWordExampleStarRelService {

    private final ExampleStarRelMapper exampleStarRelMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replaceFetchResult(Integer oldRelId, Integer newRelId) {
        if (oldRelId == null || newRelId == null) {
            return;
        }

        exampleStarRelMapper.update(new ExampleStarRelDO().setExampleId(newRelId),
            Wrappers.<ExampleStarRelDO>lambdaUpdate().eq(ExampleStarRelDO::getExampleId, oldRelId));
    }
}
