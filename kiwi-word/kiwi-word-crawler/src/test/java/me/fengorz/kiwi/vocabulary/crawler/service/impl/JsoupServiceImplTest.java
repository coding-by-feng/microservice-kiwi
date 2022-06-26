/*
 *
 * Copyright [2019~2025] [codingByFeng]
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

package me.fengorz.kiwi.vocabulary.crawler.service.impl;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;
import java.util.HashSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.word.api.dto.queue.FetchPhraseMqDTO;
import me.fengorz.kiwi.word.api.dto.queue.FetchPhraseRunUpMqDTO;
import me.fengorz.kiwi.word.api.dto.queue.FetchWordMqDTO;
import me.fengorz.kiwi.word.api.dto.queue.result.*;
import me.fengorz.kiwi.word.api.exception.JsoupFetchConnectException;
import me.fengorz.kiwi.word.api.exception.JsoupFetchPronunciationException;
import me.fengorz.kiwi.word.api.exception.JsoupFetchResultException;

@Slf4j
@ExtendWith(MockitoExtension.class)
public class JsoupServiceImplTest {

    private JsoupServiceImpl jsoupServiceImplUnderTest;

    @BeforeEach
    void setUp() {
        jsoupServiceImplUnderTest = new JsoupServiceImpl();
    }

    @Test
    void testFetchWordInfo() throws Exception {
        // Setup
        final FetchWordMqDTO dto = new FetchWordMqDTO();
        dto.setWord("against-time-the-clock");

        // Run the test
        final FetchWordResultDTO result = jsoupServiceImplUnderTest.fetchWordInfo(dto);
        log.info(result.toString());

        // Verify the results
        assertNotNull(result);
    }

    @Test
    @Disabled
    void testFetchWordInfo_ThrowsJsoupFetchResultException() {
        // Setup
        final FetchWordMqDTO dto = new FetchWordMqDTO();
        dto.setWord("word");
        dto.setQueueId(0);

        // Run the test
        assertThrows(JsoupFetchResultException.class, () -> jsoupServiceImplUnderTest.fetchWordInfo(dto));
    }

    @Test
    @Disabled
    void testFetchWordInfo_ThrowsJsoupFetchConnectException() {
        // Setup
        final FetchWordMqDTO dto = new FetchWordMqDTO();
        dto.setWord("word");
        dto.setQueueId(0);

        // Run the test
        assertThrows(JsoupFetchConnectException.class, () -> jsoupServiceImplUnderTest.fetchWordInfo(dto));
    }

    @Test
    @Disabled
    void testFetchWordInfo_ThrowsJsoupFetchPronunciationException() {
        // Setup
        final FetchWordMqDTO dto = new FetchWordMqDTO();
        dto.setWord("word");
        dto.setQueueId(0);

        // Run the test
        assertThrows(JsoupFetchPronunciationException.class, () -> jsoupServiceImplUnderTest.fetchWordInfo(dto));
    }

    @Test
    @Disabled
    void testFetchPhraseRunUp() throws Exception {
        // Setup
        final FetchPhraseRunUpMqDTO dto = new FetchPhraseRunUpMqDTO();
        dto.setWord("word");
        dto.setWordId(0);
        dto.setQueueId(0);

        final FetchPhraseRunUpResultDTO expectedResult = new FetchPhraseRunUpResultDTO();
        expectedResult.setWord("word");
        expectedResult.setWordId(0);
        expectedResult.setRelatedWords(new HashSet<>(Collections.singletonList("value")));
        expectedResult.setPhrases(new HashSet<>(Collections.singletonList("value")));

        // Run the test
        final FetchPhraseRunUpResultDTO result = jsoupServiceImplUnderTest.fetchPhraseRunUp(dto);

        // Verify the results
        assertEquals(expectedResult, result);
    }

    @Test
    @Disabled
    void testFetchPhraseRunUp_ThrowsJsoupFetchConnectException() {
        // Setup
        final FetchPhraseRunUpMqDTO dto = new FetchPhraseRunUpMqDTO();
        dto.setWord("word");
        dto.setWordId(0);
        dto.setQueueId(0);

        // Run the test
        assertThrows(JsoupFetchConnectException.class, () -> jsoupServiceImplUnderTest.fetchPhraseRunUp(dto));
    }

    @Test
    @Disabled
    void testFetchPhraseInfo() throws Exception {
        // Setup
        final FetchPhraseMqDTO dto = new FetchPhraseMqDTO();
        dto.setPhrase("phrase");
        dto.setDerivation("derivation");
        dto.setQueueId(0);

        final FetchPhraseResultDTO expectedResult = new FetchPhraseResultDTO();
        expectedResult.setPhrase("phrase");
        expectedResult.setDerivation("derivation");
        expectedResult.setQueueId(0);
        expectedResult.setRelatedWords(new HashSet<>(Collections.singletonList("value")));
        final FetchParaphraseDTO fetchParaphraseDTO = new FetchParaphraseDTO();
        fetchParaphraseDTO.setSerialNumber(0);
        fetchParaphraseDTO.setCodes("codes");
        fetchParaphraseDTO.setParaphraseEnglish("paraphraseEnglish");
        fetchParaphraseDTO.setParaphraseEnglishTranslate("paraphraseEnglishTranslate");
        fetchParaphraseDTO.setMeaningChinese("meaningChinese");
        fetchParaphraseDTO.setTranslateLanguage("translateLanguage");
        final FetchParaphraseExampleDTO fetchParaphraseExampleDTO = new FetchParaphraseExampleDTO();
        fetchParaphraseExampleDTO.setExampleSentence("exampleSentence");
        fetchParaphraseExampleDTO.setExampleTranslate("exampleTranslate");
        fetchParaphraseExampleDTO.setTranslateLanguage("translateLanguage");
        fetchParaphraseExampleDTO.setSerialNumber(0);
        fetchParaphraseDTO.setExampleDTOList(Collections.singletonList(fetchParaphraseExampleDTO));
        final FetchPhraseDTO fetchPhraseDTO = new FetchPhraseDTO();
        fetchPhraseDTO.setPhrase("phrase");
        fetchParaphraseDTO.setPhraseDTOList(Collections.singletonList(fetchPhraseDTO));
        expectedResult.setFetchParaphraseDTOList(Collections.singletonList(fetchParaphraseDTO));

        // Run the test
        final FetchPhraseResultDTO result = jsoupServiceImplUnderTest.fetchPhraseInfo(dto);

        // Verify the results
        assertEquals(expectedResult, result);
    }

    @Test
    @Disabled
    void testFetchPhraseInfo_ThrowsJsoupFetchConnectException() {
        // Setup
        final FetchPhraseMqDTO dto = new FetchPhraseMqDTO();
        dto.setPhrase("phrase");
        dto.setDerivation("derivation");
        dto.setQueueId(0);

        // Run the test
        assertThrows(JsoupFetchConnectException.class, () -> jsoupServiceImplUnderTest.fetchPhraseInfo(dto));
    }

    @Test
    @Disabled
    void testFetchPhraseInfo_ThrowsJsoupFetchResultException() {
        // Setup
        final FetchPhraseMqDTO dto = new FetchPhraseMqDTO();
        dto.setPhrase("phrase");
        dto.setDerivation("derivation");
        dto.setQueueId(0);

        // Run the test
        assertThrows(JsoupFetchResultException.class, () -> jsoupServiceImplUnderTest.fetchPhraseInfo(dto));
    }
}
