/*
 *
 *   Copyright [2019~2025] [codingByFeng]
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

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import me.fengorz.kiwi.common.api.constant.CommonConstants;
import me.fengorz.kiwi.word.api.common.CrawlerConstants;
import me.fengorz.kiwi.word.api.entity.WordFetchQueueDO;
import me.fengorz.kiwi.word.biz.mapper.WordFetchQueueMapper;
import me.fengorz.kiwi.word.biz.service.IWordFetchQueueService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 单词待抓取列表
 *
 * @author codingByFeng
 * @date 2019-10-30 14:45:45
 */
@Service("wordFetchQueueService")
@AllArgsConstructor
public class WordFetchQueueServiceImpl extends ServiceImpl<WordFetchQueueMapper, WordFetchQueueDO> implements IWordFetchQueueService {

    @Override
    public boolean insertNewQueue(WordFetchQueueDO wordFetchQueue) {
        WordFetchQueueDO one = this.getOne(new QueryWrapper<>(new WordFetchQueueDO().setWordName(wordFetchQueue.getWordName())));
        if (one != null) {
            return false;
        }
        return this.save(wordFetchQueue);
    }

    @Override
    public boolean fetchNewWord(String wordName) {
        this.getOne(
                new QueryWrapper<>(
                        new WordFetchQueueDO()
                                .setWordName(wordName)
                )
        );
        this.insertNewQueue(
                new WordFetchQueueDO()
                        .setFetchStatus(CrawlerConstants.STATUS_TO_FETCH)
                        .setIsValid(CommonConstants.TRUE)
                        .setFetchPriority(100)
        );
        return false;
    }
}
