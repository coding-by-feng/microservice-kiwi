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

package me.fengorz.kiwi.word.biz.service.operate.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.bdf.core.service.ISeqService;
import me.fengorz.kiwi.common.api.constant.CommonConstants;
import me.fengorz.kiwi.common.api.constant.MapperConstant;
import me.fengorz.kiwi.word.api.common.WordConstants;
import me.fengorz.kiwi.word.api.entity.StarRelHisDO;
import me.fengorz.kiwi.word.api.entity.WordMainDO;
import me.fengorz.kiwi.word.biz.service.base.IParaphraseExampleService;
import me.fengorz.kiwi.word.biz.service.base.IParaphraseService;
import me.fengorz.kiwi.word.biz.service.base.IStarRelHisService;
import me.fengorz.kiwi.word.biz.service.base.IWordMainService;
import me.fengorz.kiwi.word.biz.service.operate.IAsyncArchiveService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * @Author zhanshifeng
 * @Date 2020/9/16 5:26 PM
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncArchiveServiceImpl implements IAsyncArchiveService {

    private final IWordMainService mainService;
    private final IParaphraseService paraphraseService;
    private final IParaphraseExampleService exampleService;
    private final IStarRelHisService relHisService;
    private final ISeqService seqService;

    @Override
    @Async
    @Transactional(rollbackFor = Exception.class)
    public void archiveWordRel(Integer wordId, Integer listId, Integer userId) {
        Optional.ofNullable(mainService.getById(wordId)).ifPresent(word -> {
            relHisService.save(new StarRelHisDO().setId(seqService.genIntSequence(MapperConstant.T_INS_SEQUENCE))
                    .setListId(listId)
                    .setWordName(word.getWordName())
                    .setSerialNum(0)
                    .setType(WordConstants.REMEMBER_ARCHIVE_TYPE_WORD)
                    .setUserId(userId));
        });
    }

    @Override
    @Async
    @Transactional(rollbackFor = Exception.class)
    public void invalidArchiveWordRel(Integer wordId, Integer listId, Integer userId) {
        this.invalidRelHis(wordId, WordConstants.REMEMBER_ARCHIVE_TYPE_WORD, listId, userId);
    }

    private void invalidRelHis(Integer id, Integer type, Integer listId, Integer userId) {
        Optional.ofNullable(mainService.getById(id)).ifPresent(word -> {
            relHisService.update(new StarRelHisDO().setIsDel(CommonConstants.FLAG_DEL_YES),
                    Wrappers.<StarRelHisDO>lambdaUpdate()
                            .eq(StarRelHisDO::getWordName, word.getWordName())
                            .eq(StarRelHisDO::getUserId, userId)
                            .eq(StarRelHisDO::getListId, listId)
                            .eq(StarRelHisDO::getType, type)
                            .eq(StarRelHisDO::getIsDel, CommonConstants.FLAG_DEL_NO));
        });
    }

    @Async
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void archiveParaphraseRel(Integer paraphraseId, Integer listId, Integer userId) {
        Optional.ofNullable(paraphraseService.getById(paraphraseId)).ifPresent(paraphrase -> {
            WordMainDO word = mainService.getById(paraphrase.getWordId());
            if (word == null) {
                return;
            }
            relHisService.save(new StarRelHisDO().setId(seqService.genIntSequence(MapperConstant.T_INS_SEQUENCE))
                    .setListId(listId)
                    .setWordName(word.getWordName())
                    .setSerialNum(paraphrase.getSerialNumber())
                    .setType(WordConstants.REMEMBER_ARCHIVE_TYPE_PARAPHRASE)
                    .setUserId(userId));
        });
    }

    @Async
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void invalidArchiveParaphraseRel(Integer paraphraseId, Integer listId, Integer userId) {
        Optional.ofNullable(paraphraseService.getById(paraphraseId)).ifPresent(paraphrase -> {
            this.invalidRelHis(paraphrase.getWordId(), WordConstants.REMEMBER_ARCHIVE_TYPE_PARAPHRASE, listId, userId);
        });
    }

    @Override
    @Async
    @Transactional(rollbackFor = Exception.class)
    public void archiveExampleRel(Integer exampleId, Integer listId, Integer userId) {
        Optional.ofNullable(exampleService.getById(exampleId)).ifPresent(example -> {
            WordMainDO word = mainService.getById(example.getWordId());
            if (word == null) {
                return;
            }
            relHisService.save(new StarRelHisDO().setId(seqService.genIntSequence(MapperConstant.T_INS_SEQUENCE))
                    .setListId(listId)
                    .setWordName(word.getWordName())
                    .setSerialNum(example.getSerialNumber())
                    .setType(WordConstants.REMEMBER_ARCHIVE_TYPE_EXAMPLE)
                    .setUserId(userId));
        });
    }

    @Override
    @Async
    @Transactional(rollbackFor = Exception.class)
    public void invalidArchiveExampleRel(Integer exampleId, Integer listId, Integer userId) {
        Optional.ofNullable(exampleService.getById(exampleId)).ifPresent(example -> {
            this.invalidRelHis(example.getWordId(), WordConstants.REMEMBER_ARCHIVE_TYPE_EXAMPLE, listId, userId);
        });
    }
}
