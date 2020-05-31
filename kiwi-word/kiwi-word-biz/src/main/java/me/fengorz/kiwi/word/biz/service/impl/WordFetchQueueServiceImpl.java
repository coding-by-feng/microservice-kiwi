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

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import me.fengorz.kiwi.bdf.core.service.ISeqService;
import me.fengorz.kiwi.common.api.constant.CommonConstants;
import me.fengorz.kiwi.common.api.constant.MapperConstant;
import me.fengorz.kiwi.common.api.exception.ServiceException;
import me.fengorz.kiwi.word.api.common.WordCrawlerConstants;
import me.fengorz.kiwi.word.api.entity.WordFetchQueueDO;
import me.fengorz.kiwi.word.biz.mapper.WordFetchQueueMapper;
import me.fengorz.kiwi.word.biz.service.IWordFetchQueueService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 单词待抓取列表
 *
 * @author zhanshifeng
 * @date 2019-10-30 14:45:45
 */
@Service()
@RequiredArgsConstructor
public class WordFetchQueueServiceImpl extends ServiceImpl<WordFetchQueueMapper, WordFetchQueueDO> implements IWordFetchQueueService {

    private final ISeqService seqService;

    @Override
    @Transactional(rollbackFor = Exception.class, noRollbackFor = ServiceException.class, propagation = Propagation.REQUIRES_NEW)
    public boolean fetchNewWord(String wordName) {
        WordFetchQueueDO one = this.getOne(wordName);

        if (one != null) {
            // TODO ZSF 锁住的暂时也重新抓取
            // if (CommonConstants.FLAG_YES == one.getIsLock()) {
            //     return false;
            // }
            this.del(wordName);
        }

        return this.insertNewQueue(
                new WordFetchQueueDO()
                        .setQueueId(seqService.genIntSequence(MapperConstant.T_INS_SEQUENCE))
                        .setWordName(wordName)
                        .setFetchStatus(WordCrawlerConstants.STATUS_TO_FETCH)
                        .setIsValid(CommonConstants.FLAG_Y)
                        .setFetchPriority(100)
        );
    }

    @Async
    @Override
    public void asyncFetchNewWord(String wordName) {
        this.fetchNewWord(wordName);
    }

    private boolean del(String wordName) {
        return this.remove(new LambdaQueryWrapper<WordFetchQueueDO>()
                .eq(WordFetchQueueDO::getWordName, wordName)
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean invalid(String wordName) {
        if (!this.isExist(wordName)) {
            return false;
        }
        // TODO ZSF 这里要搬到废弃的历史表
        return this.update(new WordFetchQueueDO().setIsValid(CommonConstants.FLAG_N).setIsLock(CommonConstants.FLAG_NO),
                new LambdaQueryWrapper<WordFetchQueueDO>().eq(WordFetchQueueDO::getWordName, wordName));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean lock(String wordName) {
        if (!this.isExist(wordName)) {
            return false;
        }
        // TODO ZSF 这里要搬到废弃的历史表
        return this.update(new WordFetchQueueDO().setIsLock(CommonConstants.FLAG_YES),
                new LambdaQueryWrapper<WordFetchQueueDO>().eq(WordFetchQueueDO::getWordName, wordName).eq(WordFetchQueueDO::getIsLock, CommonConstants.FLAG_NO));
    }

    @Override
    public boolean insertNewQueue(WordFetchQueueDO wordFetchQueue) {
        return this.save(wordFetchQueue);
    }

    private WordFetchQueueDO getOne(String wordName) {
        return this.getOne(
                new LambdaQueryWrapper<WordFetchQueueDO>().eq(WordFetchQueueDO::getWordName, wordName)
        );
    }

    private boolean isExist(String wordName) {
        return this.getOne(wordName) != null;
    }

}
