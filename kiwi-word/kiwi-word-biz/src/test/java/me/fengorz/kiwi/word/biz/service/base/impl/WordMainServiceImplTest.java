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

package me.fengorz.kiwi.word.biz.service.base.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import me.fengorz.kiwi.word.api.dto.mapper.out.FuzzyQueryResultDTO;
import me.fengorz.kiwi.word.api.entity.WordMainDO;
import me.fengorz.kiwi.word.api.vo.WordMainVO;
import me.fengorz.kiwi.word.biz.mapper.WordMainMapper;
import me.fengorz.kiwi.word.biz.service.base.WordFetchQueueService;
import me.fengorz.kiwi.word.biz.service.base.WordMainService;

@ExtendWith(MockitoExtension.class)
class WordMainServiceImplTest {

    @Mock
    private WordFetchQueueService mockQueueService;
    @Mock
    private WordMainMapper mockMapper;

    private WordMainService underTester;

    @BeforeEach
    void setUp() {
        underTester = new WordMainServiceImpl(mockQueueService, mockMapper);
    }

    @Test
    void testSave() {
        // Setup
        final WordMainDO entity = new WordMainDO();
        entity.setWordId(0);
        entity.setWordName("wordName");
        entity.setInfoType(0);
        entity.setInTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastUpdateTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setIsDel(0);

        // Run the test
        final boolean result = underTester.save(entity);

        // Verify the results
        assertTrue(result);
    }

    @Test
    void testGetById() {
        // Setup
        final WordMainDO expectedResult = new WordMainDO();
        expectedResult.setWordId(0);
        expectedResult.setWordName("wordName");
        expectedResult.setInfoType(0);
        expectedResult.setInTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        expectedResult.setLastUpdateTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        expectedResult.setIsDel(0);

        // Run the test
        final WordMainDO result = underTester.getById("value");

        // Verify the results
        assertEquals(expectedResult, result);
    }

    @Test
    void testGetOneAndCatch() {
        // Setup
        final WordMainVO expectedResult = new WordMainVO();

        // Run the test
        final WordMainVO result = underTester.getOneAndCatch("wordName", 0);

        // Verify the results
        assertEquals(expectedResult, result);
        verify(mockQueueService).flagWordQueryException("wordName");
    }

    @Test
    void testGetWordName() {
        // Setup
        // Run the test
        final String result = underTester.getWordName(0);

        // Verify the results
        assertEquals("wordName", result);
    }

    @Test
    void testFuzzyQueryList() {
        // Setup
        final Page<WordMainDO> page = new Page<>(0L, 0L, 0L, false);
        final FuzzyQueryResultDTO fuzzyQueryResultDTO = new FuzzyQueryResultDTO();
        fuzzyQueryResultDTO.setValue("value");
        final List<FuzzyQueryResultDTO> expectedResult = Collections.singletonList(fuzzyQueryResultDTO);
        when(mockMapper.fuzzyQuery(any(Page.class), eq("query"))).thenReturn(null);

        // Run the test
        final List<FuzzyQueryResultDTO> result = underTester.fuzzyQueryList(page, "wordName");

        // Verify the results
        assertEquals(expectedResult, result);
    }

    @Test
    void testIsExist() {
        // Setup
        // Run the test
        final boolean result = underTester.isExist("wordName");

        // Verify the results
        assertTrue(result);
    }

    @Test
    void testEvictById() {
        // Setup
        // Run the test
        underTester.evictById(0);

        // Verify the results
    }

    @Test
    void testList() {
        // Setup
        final WordMainDO wordMainDO = new WordMainDO();
        wordMainDO.setWordId(0);
        wordMainDO.setWordName("wordName");
        wordMainDO.setInfoType(0);
        wordMainDO.setInTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        wordMainDO.setLastUpdateTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        wordMainDO.setIsDel(0);
        final List<WordMainDO> expectedResult = Collections.singletonList(wordMainDO);

        // Run the test
        final List<WordMainDO> result = underTester.list("wordName", 0);

        // Verify the results
        assertEquals(expectedResult, result);
    }

    @Test
    void testListDirtyData() {
        // Setup
        final WordMainDO wordMainDO = new WordMainDO();
        wordMainDO.setWordId(0);
        wordMainDO.setWordName("wordName");
        wordMainDO.setInfoType(0);
        wordMainDO.setInTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        wordMainDO.setLastUpdateTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        wordMainDO.setIsDel(0);
        final List<WordMainDO> expectedResult = Collections.singletonList(wordMainDO);

        // Run the test
        final List<WordMainDO> result = underTester.listDirtyData(0);

        // Verify the results
        assertEquals(expectedResult, result);
    }

    @Test
    void testListOverlapInUnLock() {
        // Setup
        when(mockMapper.selectOverlapAnyway()).thenReturn(Collections.singletonList("value"));

        // Run the test
        final List<String> result = underTester.listOverlapAnyway();

        // Verify the results
        assertEquals(Collections.singletonList("value"), result);
    }

    @Test
    void testListOverlapInUnLock_WordMainMapperReturnsNoItems() {
        // Setup
        when(mockMapper.selectOverlapAnyway()).thenReturn(Collections.emptyList());

        // Run the test
        final List<String> result = underTester.listOverlapAnyway();

        // Verify the results
        assertEquals(Collections.emptyList(), result);
    }
}
