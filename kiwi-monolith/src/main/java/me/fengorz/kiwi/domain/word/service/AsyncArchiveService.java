/*
 * Copyright [2019~2025] [codingByFeng]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.fengorz.kiwi.domain.word.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.domain.tools.service.SequenceService;
import me.fengorz.kiwi.domain.word.entity.StarRelHis;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Async Archive Service - manages async archiving of starred items
 * Migrated from AsyncArchiveServiceImpl
 *
 * @author codingByFeng
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncArchiveService {

    private final WordMainService wordMainService;
    private final ParaphraseService paraphraseService;
    private final ParaphraseExampleService exampleService;
    private final StarRelHisService starRelHisService;
    private final SequenceService sequenceService;

    /**
     * Archive word relationship asynchronously
     */
    @Async
    @Transactional
    public void archiveWordRel(Integer wordId, Integer listId, Integer userId) {
        wordMainService.findById(wordId).ifPresent(word -> {
            StarRelHis his = StarRelHis.builder()
                    .id(sequenceService.generateSequence())
                    .listId(listId)
                    .wordName(word.getWordName())
                    .serialNum(0)
                    .type(StarRelHis.TYPE_WORD)
                    .userId(userId)
                    .build();
            starRelHisService.save(his);
            log.debug("Archived word relation: wordId={}, listId={}, userId={}", wordId, listId, userId);
        });
    }

    /**
     * Invalidate (soft delete) archived word relationship
     */
    @Async
    @Transactional
    public void invalidArchiveWordRel(Integer wordId, Integer listId, Integer userId) {
        wordMainService.findById(wordId).ifPresent(word -> {
            starRelHisService.invalidate(word.getWordName(), userId, listId, StarRelHis.TYPE_WORD);
            log.debug("Invalidated word archive: wordId={}, listId={}, userId={}", wordId, listId, userId);
        });
    }

    /**
     * Archive paraphrase relationship asynchronously
     */
    @Async
    @Transactional
    public void archiveParaphraseRel(Integer paraphraseId, Integer listId, Integer userId) {
        paraphraseService.findById(paraphraseId).ifPresent(paraphrase -> {
            wordMainService.findById(paraphrase.getWordId()).ifPresent(word -> {
                StarRelHis his = StarRelHis.builder()
                        .id(sequenceService.generateSequence())
                        .listId(listId)
                        .wordName(word.getWordName())
                        .serialNum(paraphrase.getSerialNumber())
                        .type(StarRelHis.TYPE_PARAPHRASE)
                        .userId(userId)
                        .build();
                starRelHisService.save(his);
                log.debug("Archived paraphrase relation: paraphraseId={}, listId={}, userId={}",
                        paraphraseId, listId, userId);
            });
        });
    }

    /**
     * Invalidate (soft delete) archived paraphrase relationship
     */
    @Async
    @Transactional
    public void invalidArchiveParaphraseRel(Integer paraphraseId, Integer listId, Integer userId) {
        paraphraseService.findById(paraphraseId).ifPresent(paraphrase -> {
            wordMainService.findById(paraphrase.getWordId()).ifPresent(word -> {
                starRelHisService.invalidate(word.getWordName(), userId, listId, StarRelHis.TYPE_PARAPHRASE);
                log.debug("Invalidated paraphrase archive: paraphraseId={}, listId={}, userId={}",
                        paraphraseId, listId, userId);
            });
        });
    }

    /**
     * Archive example relationship asynchronously
     */
    @Async
    @Transactional
    public void archiveExampleRel(Integer exampleId, Integer listId, Integer userId) {
        exampleService.findById(exampleId).ifPresent(example -> {
            wordMainService.findById(example.getWordId()).ifPresent(word -> {
                StarRelHis his = StarRelHis.builder()
                        .id(sequenceService.generateSequence())
                        .listId(listId)
                        .wordName(word.getWordName())
                        .serialNum(example.getSerialNumber())
                        .type(StarRelHis.TYPE_EXAMPLE)
                        .userId(userId)
                        .build();
                starRelHisService.save(his);
                log.debug("Archived example relation: exampleId={}, listId={}, userId={}",
                        exampleId, listId, userId);
            });
        });
    }

    /**
     * Invalidate (soft delete) archived example relationship
     */
    @Async
    @Transactional
    public void invalidArchiveExampleRel(Integer exampleId, Integer listId, Integer userId) {
        exampleService.findById(exampleId).ifPresent(example -> {
            wordMainService.findById(example.getWordId()).ifPresent(word -> {
                starRelHisService.invalidate(word.getWordName(), userId, listId, StarRelHis.TYPE_EXAMPLE);
                log.debug("Invalidated example archive: exampleId={}, listId={}, userId={}",
                        exampleId, listId, userId);
            });
        });
    }
}
