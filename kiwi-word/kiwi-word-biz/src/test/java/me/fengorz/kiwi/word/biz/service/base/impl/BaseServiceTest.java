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

package me.fengorz.kiwi.word.biz.service.base.impl;

import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.sdk.constant.EnvConstants;
import me.fengorz.kiwi.word.biz.WordBizApplication;
import me.fengorz.kiwi.word.biz.service.base.ParaphraseStarListService;
import me.fengorz.kiwi.word.biz.service.base.ParaphraseStarRelService;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

@Slf4j
@ActiveProfiles({EnvConstants.DEV, EnvConstants.BASE})
@ExtendWith(SpringExtension.class)
@TestPropertySource("classpath:env.properties")
@SpringBootTest(classes = WordBizApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class BaseServiceTest {

    @Autowired
    private ParaphraseStarRelService paraphraseStarRelService;
    @Autowired
    private ParaphraseStarListService starListService;

    @Test
    @Disabled
    public void test_listNotGeneratedVoice() {
        Assertions.assertTrue(CollectionUtils.isEmpty(paraphraseStarRelService.listNotGeneratedVoice()));
        List<Integer> notAllGeneratedList = paraphraseStarRelService.listNotAllGeneratedVoice();
        Assertions.assertTrue(CollectionUtils.isNotEmpty(notAllGeneratedList));
        log.info("notAllGeneratedList size={}", notAllGeneratedList.size());
    }

    @Test
    public void test_rememberOne() {
        Assertions.assertDoesNotThrow(() -> {
            starListService.rememberOne(2539690, 1266094);
        });
    }

}