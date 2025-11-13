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

package me.fengorz.kason.word.biz.service;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kason.common.sdk.constant.EnvConstants;
import me.fengorz.kason.word.api.common.enumeration.ReviseAudioGenerationEnum;
import me.fengorz.kason.word.biz.WordBizApplication;
import me.fengorz.kason.word.biz.service.operate.CrawlerService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@Slf4j
@ActiveProfiles({EnvConstants.TEST})
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = WordBizApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Disabled
public class CrawlerServiceTest {

    @Autowired
    private CrawlerService crawlerService;

    @Test
    @SneakyThrows
    @Disabled
    void generateTtsVoice() {
        Assertions.assertDoesNotThrow(() -> crawlerService.generateTtsVoice(ReviseAudioGenerationEnum.ONLY_COLLECTED));
    }

    @Test
    @Disabled
    void test_reFetchPronunciation() {
        crawlerService.reFetchPronunciation(3053026);
    }

    @Test
    @SneakyThrows
    void test_reGenIncorrectAudioByVoicerss() {
        crawlerService.reGenIncorrectAudioByVoicerss();
    }

}