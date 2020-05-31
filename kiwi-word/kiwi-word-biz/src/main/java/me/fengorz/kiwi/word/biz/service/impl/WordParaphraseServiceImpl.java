/*
 *
 *   Copyright [2019~2025] [zhanshifeng]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 *
 */
package me.fengorz.kiwi.word.biz.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import me.fengorz.kiwi.common.api.constant.CommonConstants;
import me.fengorz.kiwi.word.api.dto.mapper.in.SelectEntityIsCollectDTO;
import me.fengorz.kiwi.word.api.entity.WordParaphraseDO;
import me.fengorz.kiwi.word.api.entity.WordParaphraseExampleDO;
import me.fengorz.kiwi.word.api.vo.WordParaphraseExampleVO;
import me.fengorz.kiwi.word.api.vo.detail.WordParaphraseVO;
import me.fengorz.kiwi.word.biz.mapper.WordParaphraseExampleMapper;
import me.fengorz.kiwi.word.biz.mapper.WordParaphraseMapper;
import me.fengorz.kiwi.word.biz.service.IWordParaphraseService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 单词释义表
 *
 * @author zhanshifeng
 * @date 2019-10-31 20:39:48
 */
@Service()
@RequiredArgsConstructor
public class WordParaphraseServiceImpl extends ServiceImpl<WordParaphraseMapper, WordParaphraseDO> implements IWordParaphraseService {

    private final WordParaphraseMapper wordParaphraseMapper;
    private final WordParaphraseExampleMapper wordParaphraseExampleMapper;

    @Override
    public Integer countById(Integer id) {
        return this.count(
                new QueryWrapper<>(
                        new WordParaphraseDO()
                                .setParaphraseId(id)
                                .setIsDel(CommonConstants.FLAG_N)
                )
        );
    }

    @Override
    public List<WordParaphraseVO> selectParaphraseAndIsCollect(Integer characterId, Integer currentUserId) {
        return this.wordParaphraseMapper.selectParaphraseAndIsCollect(
                new SelectEntityIsCollectDTO()
                        .setEntityId(characterId)
                        .setOwner(currentUserId)
        );
    }

    @Override
    public WordParaphraseVO findWordParaphraseVO(Integer paraphraseId) {
        WordParaphraseVO wordParaphraseVO = new WordParaphraseVO();
        List<WordParaphraseExampleVO> wordParaphraseExampleVOList = new ArrayList<>();
        WordParaphraseDO wordParaphraseDO = this.wordParaphraseMapper.selectById(paraphraseId);
        BeanUtil.copyProperties(wordParaphraseDO, wordParaphraseVO);
        List<WordParaphraseExampleDO> exampleDOS = wordParaphraseExampleMapper.selectList(new LambdaQueryWrapper<WordParaphraseExampleDO>().eq(WordParaphraseExampleDO::getParaphraseId, paraphraseId));
        if (CollUtil.isNotEmpty(exampleDOS)) {
            exampleDOS.forEach(wordParaphraseExampleDO -> {
                WordParaphraseExampleVO exampleVO = new WordParaphraseExampleVO();
                BeanUtil.copyProperties(wordParaphraseExampleDO, exampleVO);
                wordParaphraseExampleVOList.add(exampleVO);
            });
        }
        wordParaphraseVO.setWordParaphraseExampleVOList(wordParaphraseExampleVOList);
        return wordParaphraseVO;
    }
}
