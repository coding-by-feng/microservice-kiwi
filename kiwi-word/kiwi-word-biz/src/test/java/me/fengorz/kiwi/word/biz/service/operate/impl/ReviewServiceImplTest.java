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

package me.fengorz.kiwi.word.biz.service.operate.impl;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.sdk.constant.EnvConstants;
import me.fengorz.kiwi.common.sdk.constant.GlobalConstants;
import me.fengorz.kiwi.common.sdk.util.json.KiwiJsonUtils;
import me.fengorz.kiwi.word.api.common.enumeration.ReviewDailyCounterTypeEnum;
import me.fengorz.kiwi.word.api.vo.WordReviewDailyCounterVO;
import me.fengorz.kiwi.word.biz.WordBizApplication;
import me.fengorz.kiwi.word.biz.controller.WordReviewController;
import me.fengorz.kiwi.word.biz.model.TtsConfig;
import me.fengorz.kiwi.word.biz.service.operate.IReviewService;

@Slf4j
@ActiveProfiles({EnvConstants.DEV, EnvConstants.BASE})
@ExtendWith(SpringExtension.class)
@TestPropertySource("classpath:env.properties")
@SpringBootTest(classes = WordBizApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ReviewServiceImplTest {

    @Autowired
    private IReviewService reviewService;

    @Autowired
    private TtsConfig ttsConfig;

    @Autowired
    private WordReviewController controller;

    @Test
    @Disabled
    void initPermanent() {
        Assertions.assertDoesNotThrow(() -> reviewService.initPermanent(false, false));
    }

    @Test
    @Disabled
    void generateTtsVoiceFromParaphraseId() {
        Assertions.assertDoesNotThrow(() -> reviewService.generateTtsVoiceFromParaphraseId(2774367));
    }

    @Test
    @Disabled
    void generateTtsVoice() {
        Assertions.assertDoesNotThrow(() -> reviewService.generateTtsVoice(true));
    }

    @Test
    @Disabled
    void createTheDays() {
        Assertions.assertDoesNotThrow(() -> reviewService.createTheDays(1));
    }

    @Test
    @Disabled
    void listReviewCounterVO() {
        List<WordReviewDailyCounterVO> list = reviewService.listReviewCounterVO(1);
        log.info("listReviewCounterVO >>>>>>>>>>>> {}", KiwiJsonUtils.toJsonStr(list));
        Assertions.assertEquals(ReviewDailyCounterTypeEnum.values().length, list.size());
    }

    @Test
    // @Disabled
    void autoSelectApiKey() {
        String apiKey = reviewService.autoSelectApiKey();
        Assertions.assertTrue(ttsConfig.listApiKey().contains(apiKey));
        Assertions.assertEquals(apiKey, ttsConfig.getApiKey5());
    }

    @Test
    @Disabled
    void increaseApiKeyUsedTime() {
        Assertions.assertDoesNotThrow(() -> reviewService.increaseApiKeyUsedTime(ttsConfig.getApiKey1()));
    }

    @Test
    @Disabled
    void useTtsApiKey() {
        Assertions.assertDoesNotThrow(() -> {
            // for (String apiKey : ttsConfig.listApiKey()) {
            // reviewService.useTtsApiKey(apiKey, 350);
            // }

            reviewService.useTtsApiKey(ttsConfig.getApiKey5(), 0);
        });
    }

    @Test
    @Disabled
    void queryTtsApiKeyUsed() {
        Assertions.assertEquals(reviewService.queryTtsApiKeyUsed(ttsConfig.getApiKey5()), 0);
        Assertions.assertEquals(reviewService.queryTtsApiKeyUsed(ttsConfig.getApiKey1()), 350);
        Assertions.assertEquals(reviewService.queryTtsApiKeyUsed(ttsConfig.getApiKey2()), 350);
        Assertions.assertEquals(reviewService.queryTtsApiKeyUsed(ttsConfig.getApiKey3()), 350);
        Assertions.assertEquals(reviewService.queryTtsApiKeyUsed(ttsConfig.getApiKey4()), 350);
    }

    @Test
    @Disabled
    void testReplace() {
        System.out.println("--------------> AA...BB".replaceAll("\\.\\.\\.", GlobalConstants.WHAT));
    }
}