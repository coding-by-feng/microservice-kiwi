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

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import lombok.RequiredArgsConstructor;
import me.fengorz.kiwi.word.api.entity.WordPronunciationDO;
import me.fengorz.kiwi.word.biz.mapper.WordPronunciationMapper;
import me.fengorz.kiwi.word.biz.service.base.IWordPronunciationService;

/**
 * 单词例句表
 *
 * @author zhanshifeng
 * @date 2019-10-31 20:54:06
 */
@Service()
@RequiredArgsConstructor
public class WordPronunciationServiceImpl extends ServiceImpl<WordPronunciationMapper, WordPronunciationDO>
    implements IWordPronunciationService {

    private final WordPronunciationMapper wordPronunciationMapper;

    @Override
    public int blankPronunciationVoice(String wordName) {
        return wordPronunciationMapper.blankPronunciationVoice(wordName);
    }

    @Override
    public int deleteByWordName(String wordName) {
        return wordPronunciationMapper.deleteByWordName(wordName);
    }
}