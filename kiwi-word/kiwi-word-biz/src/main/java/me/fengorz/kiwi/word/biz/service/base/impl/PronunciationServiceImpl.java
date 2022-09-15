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

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import lombok.RequiredArgsConstructor;
import me.fengorz.kiwi.word.api.entity.PronunciationDO;
import me.fengorz.kiwi.word.biz.mapper.PronunciationMapper;
import me.fengorz.kiwi.word.biz.service.base.PronunciationService;

/**
 * 单词例句表
 *
 * @author zhanshifeng
 * @date 2019-10-31 20:54:06
 */
@Service()
@RequiredArgsConstructor
public class PronunciationServiceImpl extends ServiceImpl<PronunciationMapper, PronunciationDO>
    implements PronunciationService {

    private final PronunciationMapper pronunciationMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int blankPronunciationVoice(String wordName) {
        return pronunciationMapper.blankPronunciationVoice(wordName);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteByWordName(String wordName) {
        return pronunciationMapper.deleteByWordName(wordName);
    }
}
