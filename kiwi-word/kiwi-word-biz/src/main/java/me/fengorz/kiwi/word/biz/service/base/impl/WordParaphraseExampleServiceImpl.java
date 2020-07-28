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

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import lombok.RequiredArgsConstructor;
import me.fengorz.kiwi.word.api.dto.mapper.in.SelectEntityIsCollectDTO;
import me.fengorz.kiwi.word.api.entity.WordParaphraseExampleDO;
import me.fengorz.kiwi.word.api.vo.WordParaphraseExampleVO;
import me.fengorz.kiwi.word.biz.mapper.WordParaphraseExampleMapper;
import me.fengorz.kiwi.word.biz.service.base.IWordParaphraseExampleService;

/**
 * 单词例句表
 *
 * @author zhanshifeng
 * @date 2019-10-31 20:40:38
 */
@Service()
@RequiredArgsConstructor
public class WordParaphraseExampleServiceImpl extends ServiceImpl<WordParaphraseExampleMapper, WordParaphraseExampleDO>
    implements IWordParaphraseExampleService {

    private final WordParaphraseExampleMapper wordParaphraseExampleMapper;

    @Override
    public Integer countById(Integer id) {
        return this.count(new QueryWrapper<>(new WordParaphraseExampleDO().setExampleId(id)));
    }

    @Override
    public List<WordParaphraseExampleVO> selectExampleAndIsCollect(Integer owner, Integer paraphraseId) {
        return this.wordParaphraseExampleMapper
            .selectExampleAndIsCollect(new SelectEntityIsCollectDTO().setOwner(owner).setEntityId(paraphraseId));
    }
}
