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
import me.fengorz.kiwi.domain.word.entity.*;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Word Cleaner Service - handles word data cleanup and cascade deletion
 * Migrated from CleanerServiceImpl
 *
 * @author codingByFeng
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WordCleanerService {

    private final WordMainService wordMainService;
    private final WordCharacterService characterService;
    private final ParaphraseService paraphraseService;
    private final ParaphraseExampleService exampleService;
    private final PronunciationService pronunciationService;
    private final WordMainVariantService variantService;
    private final ParaphrasePhraseService phraseService;
    private final FetchQueueService fetchQueueService;
    private final WordQueryService wordQueryService;

    /**
     * Remove word by name and queue ID
     *
     * @param wordName the word name to remove
     * @param queueId  the queue ID for tracking
     * @return list of pronunciation file paths that need to be deleted from storage
     */
    @Transactional
    public List<String> removeWord(String wordName, Integer queueId) {
        List<String> pronunciationPaths = new ArrayList<>();

        // Find words by name
        List<WordMain> words = new ArrayList<>(wordMainService.findAllByWordName(wordName));

        // If not found by name, try finding by variant
        if (words.isEmpty()) {
            Optional<WordMainVariant> variantOpt = variantService.findByVariantName(wordName);
            variantOpt.flatMap(v -> wordMainService.findById(v.getWordId()))
                    .ifPresent(words::add);
        }

        if (words.isEmpty()) {
            log.info("No word found to remove: {}", wordName);
            return pronunciationPaths;
        }

        for (WordMain word : words) {
            evictAllCaches(word, wordName);
            pronunciationPaths.addAll(removeWordRelatedData(word));
        }

        log.info("Removed word [{}] with {} pronunciation files to clean", wordName, pronunciationPaths.size());
        return pronunciationPaths;
    }

    /**
     * Remove word by queue ID
     *
     * @param queueId the fetch queue ID
     * @return list of pronunciation file paths that need to be deleted from storage
     */
    @Transactional
    public List<String> removeWordByQueueId(Integer queueId) {
        List<String> pronunciationPaths = new ArrayList<>();

        fetchQueueService.findById(queueId).ifPresent(queue -> {
            String wordName = queue.getWordName();
            log.info("Removing word [{}] from queue ID [{}]", wordName, queueId);

            List<WordMain> words = wordMainService.findAllByWordName(wordName);
            for (WordMain word : words) {
                evictAllCaches(word, wordName);
                pronunciationPaths.addAll(removeWordRelatedData(word));
                variantService.deleteByWordId(word.getWordId());
            }
        });

        return pronunciationPaths;
    }

    /**
     * Remove phrase by queue ID
     *
     * @param queueId the fetch queue ID
     */
    @Transactional
    public void removePhraseByQueueId(Integer queueId) {
        fetchQueueService.findById(queueId).ifPresent(queue -> {
            String wordName = queue.getWordName();
            log.info("Removing phrase [{}] from queue ID [{}]", wordName, queueId);

            List<WordMain> phrases = wordMainService.findByWordNameAndInfoType(wordName, WordMain.INFO_TYPE_PHRASE);
            for (WordMain phrase : phrases) {
                evictAllCaches(phrase, wordName);
                variantService.deleteByWordId(phrase.getWordId());
                paraphraseService.deleteByWordId(phrase.getWordId());
                wordMainService.deleteById(phrase.getWordId());
            }
        });
    }

    /**
     * Remove all related data for a word
     */
    private List<String> removeWordRelatedData(WordMain word) {
        List<String> pronunciationPaths = new ArrayList<>();
        Integer wordId = word.getWordId();

        // Delete variants
        variantService.deleteByWordId(wordId);

        // Get all characters for this word
        List<WordCharacter> characters = characterService.findByWordId(wordId);
        for (WordCharacter character : characters) {
            Integer characterId = character.getCharacterId();

            // Get all paraphrases for this character
            List<Paraphrase> paraphrases = paraphraseService.findByCharacterId(characterId);
            for (Paraphrase paraphrase : paraphrases) {
                Integer paraphraseId = paraphrase.getParaphraseId();

                // Delete examples for this paraphrase
                exampleService.deleteByParaphraseId(paraphraseId);

                // Delete phrases for this paraphrase
                phraseService.deleteByParaphraseId(paraphraseId);
            }

            // Delete all paraphrases for this character
            paraphraseService.deleteByCharacterId(characterId);

            // Evict character cache
            characterService.evictCache(characterId);
        }

        // Delete all characters
        characterService.deleteByWordId(wordId);

        // Get pronunciation file paths before deletion
        List<Pronunciation> pronunciations = pronunciationService.findByWordId(wordId);
        for (Pronunciation pronunciation : pronunciations) {
            if (pronunciation.getVoiceFilePath() != null && !pronunciation.getVoiceFilePath().isBlank()) {
                pronunciationPaths.add(pronunciation.getVoiceFilePath());
            }
        }

        // Delete all pronunciations
        pronunciationService.deleteByWordId(wordId);

        // Finally delete the word main record
        wordMainService.deleteById(wordId);

        return pronunciationPaths;
    }

    /**
     * Evict all caches related to a word
     */
    @CacheEvict(value = {"wordQuery", "wordMain"}, allEntries = true)
    public void evictAllCaches(WordMain word, String wordName) {
        // Evict word query cache
        wordQueryService.evictWordCache(wordName);
        if (!wordName.equals(word.getWordName())) {
            wordQueryService.evictWordCache(word.getWordName());
        }
        // Evict word main cache
        wordMainService.evictCacheById(word.getWordId());
        log.debug("Evicted all caches for word: {} (ID: {})", wordName, word.getWordId());
    }

    /**
     * Clean up orphaned data (data without parent references)
     */
    @Transactional
    public void cleanOrphanedData() {
        log.info("Starting orphaned data cleanup...");

        // This is a placeholder for orphaned data cleanup logic
        // In production, this would identify and remove:
        // - Characters without words
        // - Paraphrases without characters or words
        // - Examples without paraphrases
        // - Pronunciations without words

        log.info("Orphaned data cleanup completed");
    }
}
