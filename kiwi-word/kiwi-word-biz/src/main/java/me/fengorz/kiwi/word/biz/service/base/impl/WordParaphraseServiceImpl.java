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

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import me.fengorz.kiwi.common.api.constant.CommonConstants;
import me.fengorz.kiwi.word.api.dto.mapper.in.SelectEntityIsCollectDTO;
import me.fengorz.kiwi.word.api.entity.WordParaphraseDO;
import me.fengorz.kiwi.word.api.vo.detail.WordParaphraseVO;
import me.fengorz.kiwi.word.biz.mapper.WordParaphraseMapper;
import me.fengorz.kiwi.word.biz.service.base.IWordParaphraseService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 单词释义表
 *
 * @author zhanshifeng
 * @date 2019-10-31 20:39:48
 */
@Service()
@RequiredArgsConstructor
public class WordParaphraseServiceImpl extends ServiceImpl<WordParaphraseMapper, WordParaphraseDO>
    implements IWordParaphraseService {

    private final WordParaphraseMapper wordParaphraseMapper;

    @Override
    public Integer countById(Integer id) {
        return this
            .count(new QueryWrapper<>(new WordParaphraseDO().setParaphraseId(id).setIsDel(CommonConstants.FLAG_DEL_NO)));
    }

    @Override
    public List<WordParaphraseVO> selectParaphraseAndIsCollect(Integer characterId, Integer currentUserId) {
        return this.wordParaphraseMapper.selectParaphraseAndIsCollect(
            new SelectEntityIsCollectDTO().setEntityId(characterId).setOwner(currentUserId));
    }

}
